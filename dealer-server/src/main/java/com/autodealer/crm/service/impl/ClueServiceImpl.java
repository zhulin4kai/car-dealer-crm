package com.autodealer.crm.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.dto.ImportContext;
import com.autodealer.crm.dto.ImportResult;
import com.autodealer.crm.dto.ImportRowError;
import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TClueRemarkMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.BaseQuery;
import com.autodealer.crm.query.ClueQuery;
import com.autodealer.crm.query.DicQuery;
import com.autodealer.crm.result.ClueExcelRaw;
import com.autodealer.crm.service.ClueImportValidator;
import com.autodealer.crm.service.ClueImportValidator.ValidatedClueImport;
import com.autodealer.crm.service.ClueService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClueServiceImpl implements ClueService {

    private static final Logger log = LoggerFactory.getLogger(ClueServiceImpl.class);

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private DicMapper dicMapper;

    @Resource
    private TUserMapper tUserMapper;

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private TProductMapper tProductMapper;

    @Resource
    private ClueImportValidator clueImportValidator;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Override
    public PageInfo<TClue> getClueByPage(Integer current, Integer pageSize) {
        // 参数校验
        if (current == null || current < 1) {
            current = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.PAGE_SIZE;
        }
        // 限制pageSize范围
        if (pageSize > 100) {
            pageSize = 100;
        }

        // 1.设置PageHelper
        PageHelper.startPage(current, pageSize);
        // 2.查询
        List<TClue> list = tClueMapper.selectClueByPage(BaseQuery.builder().build());
        // 3.封装分页数据到PageInfo
        PageInfo<TClue> info = new PageInfo<>(list);
        return info;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importExcel(InputStream inputStream) {
        Integer operatorId = currentUserProvider.getCurrentUserId();

        // 1. 构造 ImportContext
        ImportContext context = buildImportContext();

        // 2. Phase 1: EasyExcel 解析 ClueExcelRaw 到内存列表（仅收集原始数据）
        List<ClueExcelRaw> rawList = new ArrayList<>();
        EasyExcel.read(inputStream, ClueExcelRaw.class, new ReadListener<ClueExcelRaw>() {
            @Override
            public void invoke(ClueExcelRaw data, AnalysisContext context) {
                rawList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 解析完成，不做任何写入
            }
        }).sheet().doRead();

        // 3. Phase 2: 校验和转换
        ValidatedClueImport validated = clueImportValidator.validateAndTransform(rawList, context, operatorId);
        ImportResult result = validated.getResult();
        List<TClue> validClues = validated.getClues();

        // 空工作表明确标记为失败
        if (result.getTotalRows() == 0 && result.getFailedRows() == 0 && !result.getErrors().isEmpty()) {
            result.setFailedRows(1);
        }

        // 任一行有错误，不写入数据库
        if (result.getFailedRows() > 0) {
            return result;
        }

        // 空数据行（全部合法但无数据）
        if (validClues.isEmpty()) {
            return result;
        }

        // 4. 检查数据库手机号重复
        List<String> phones = validClues.stream()
                .map(TClue::getPhone)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (!phones.isEmpty()) {
            List<String> existingPhones = tClueMapper.selectExistingPhones(phones);
            if (existingPhones != null && !existingPhones.isEmpty()) {
                Set<String> existingSet = new HashSet<>(existingPhones);
                for (int i = 0; i < validClues.size(); i++) {
                    TClue clue = validClues.get(i);
                    if (existingSet.contains(clue.getPhone())) {
                        result.addError(new ImportRowError(i + 1, "手机号", "该手机号在数据库中已存在"));
                        result.setFailedRows(result.getFailedRows() + 1);
                        result.setValidRows(result.getValidRows() - 1);
                    }
                }
            }
        }

        // 数据库重复检查后有失败行，不写入
        if (result.getFailedRows() > 0) {
            return result;
        }

        // 5. 批量写入
        try {
            int affectedRows = tClueMapper.saveClue(validClues);
            if (affectedRows != validClues.size()) {
                throw new com.autodealer.crm.exception.BusinessException(
                        com.autodealer.crm.result.CodeEnum.FAIL,
                        "批量写入影响行数(" + affectedRows + ")不等于待写入数量(" + validClues.size() + ")，已全部回滚");
            }
            result.setImportedCount(validClues.size());
            auditRecorder.record(AuditActionEnum.CLUE_IMPORT, String.valueOf(validClues.size()));
        } catch (DuplicateKeyException e) {
            throw new com.autodealer.crm.exception.BusinessException(
                    com.autodealer.crm.result.CodeEnum.FAIL,
                    "导入数据存在唯一键冲突，已全部回滚");
        }

        return result;
    }

    /**
     * 构造导入上下文，一次性从数据库加载所有字典、负责人、活动和商品映射。
     */
    private ImportContext buildImportContext() {
        // 字典值：按 typeCode 分组
        List<TDicValue> allDicValues = dicMapper.selectDicValues(new DicQuery());
        Map<String, List<TDicValue>> dicMap = new HashMap<>();
        if (allDicValues != null) {
            for (TDicValue v : allDicValues) {
                dicMap.computeIfAbsent(v.getTypeCode(), k -> new ArrayList<>()).add(v);
            }
        }

        // 商品：名称到 ProductSimpleDTO 的映射
        Map<String, ProductSimpleDTO> productMap = new HashMap<>();
        List<TProduct> products = tProductMapper.selectAllOnSale();
        if (products != null) {
            for (TProduct p : products) {
                ProductSimpleDTO dto = new ProductSimpleDTO();
                dto.setId(p.getId().intValue());
                dto.setName(p.getName());
                productMap.put(p.getName(), dto);
            }
        }

        // 负责人：名称到 TUser 的映射
        Map<String, TUser> ownerMap = new HashMap<>();
        List<TUser> owners = tUserMapper.selectByOwner();
        if (owners != null) {
            for (TUser user : owners) {
                ownerMap.put(user.getName(), user);
            }
        }

        // 活动：名称到 TActivity 的映射
        Map<String, TActivity> activityMap = new HashMap<>();
        List<TActivity> activities = tActivityMapper.selecOngoingActivity(null);
        if (activities != null) {
            for (TActivity activity : activities) {
                activityMap.put(activity.getName(), activity);
            }
        }

        return new ImportContext(dicMap, productMap, ownerMap, activityMap);
    }

    @Override
    public Boolean checkPhone(String phone) {
        int count = tClueMapper.selectByCount(phone);
        return count <= 0; //没有查到手机号是true
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveClue(ClueQuery clueQuery) {
        int count = tClueMapper.selectByCount(clueQuery.getPhone());
        if (count <= 0) {
            TClue tClue = new TClue();

            //把前端提交过来的参数数据对象ClueQuery复制到TClue对象中
            //Spring框架有个工具类BeanUtils可以进行对象的复制,复制的条件要求是：两个对象的字段名要相同，字段的类型也相同，这样才可以复制
            BeanUtils.copyProperties(clueQuery, tClue);

            Integer operatorId = currentUserProvider.getCurrentUserId();
            tClue.setOwnerId(operatorId);
            tClue.setCreateTime(new Date()); //创建时间
            tClue.setCreateBy(operatorId); //创建人id

            int rows = tClueMapper.insertSelective(tClue);
            if (rows > 0) {
                auditRecorder.record(AuditActionEnum.CLUE_CREATE, String.valueOf(tClue.getId()));
            }
            return rows;
        } else {
            throw new BusinessException(CodeEnum.DUPLICATE, "该手机号已经录入过了，不能再录入");
        }
    }

    @Override
    public TClue getClueById(Integer id) {
        return tClueMapper.selectDetailById(id, currentUserProvider.getDataScopeUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateClue(ClueQuery clueQuery) {
        // 先查询原记录，获取原手机号
        TClue originalClue = requireAccessibleClue(clueQuery.getId());

        TClue tClue = new TClue();

        //把前端提交过来的参数数据对象ClueQuery复制到TClue对象中
        //Spring框架有个工具类BeanUtils可以进行对象的复制,复制的条件要求是：两个对象的字段名要相同，字段的类型也相同，这样才可以复制
        BeanUtils.copyProperties(clueQuery, tClue);
        tClue.setOwnerId(originalClue.getOwnerId());

        // 如果传入的手机号与原记录不同，忽略手机号字段
        if (clueQuery.getPhone() != null && !clueQuery.getPhone().equals(originalClue.getPhone())) {
            tClue.setPhone(null); // 设置为null，让MyBatis的updateByPrimaryKeySelective跳过该字段
        }

        tClue.setEditTime(new Date()); //编辑时间
        tClue.setEditBy(currentUserProvider.getCurrentUserId()); //编辑人id

        return tClueMapper.updateByPrimaryKeySelective(tClue);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int delClueById(Integer id) {
        if (id == null) {
            return 0;
        }
        requireAccessibleClue(id);
        // 先删除关联的线索备注
        tClueRemarkMapper.deleteByClueId(id);
        // 再删除线索
        int rows = tClueMapper.deleteByPrimaryKey(id);
        if (rows > 0) {
            auditRecorder.record(AuditActionEnum.CLUE_DELETE, String.valueOf(id));
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDelClueByIds(List<Integer> ids) {
        if (ids == null || ids.size() == 0) {
            return 0;
        }
        List<Integer> distinctIds = ids.stream().distinct().sorted().toList();
        distinctIds.forEach(this::requireAccessibleClue);
        // 先删除关联的线索备注
        for (Integer id : distinctIds) {
            tClueRemarkMapper.deleteByClueId(id);
        }
        // 再删除线索
        int rows = tClueMapper.batchDeleteByIds(distinctIds);
        if (rows > 0) {
            auditRecorder.record(AuditActionEnum.CLUE_DELETE, distinctIds.toString());
        }
        return rows;
    }

    private TClue requireAccessibleClue(Integer id) {
        TClue clue = tClueMapper.selectScopedByPrimaryKey(
                id, currentUserProvider.getDataScopeUserId());
        if (clue == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "线索不存在或无权访问");
        }
        return clue;
    }
}
