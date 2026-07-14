package com.autodealer.crm.modules.sales.lead.application.internal;

import com.autodealer.crm.modules.identity.application.api.port.UserDirectoryDataPort;
import com.autodealer.crm.modules.dictionary.application.api.port.DictionaryDataPort;
import com.autodealer.crm.modules.commerce.catalog.application.api.port.ProductCatalogDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.sales.activity.application.api.port.ActivityDataPort;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ClueLifecycleRequest;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ImportContext;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ImportResult;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ImportRowError;
import com.autodealer.crm.modules.commerce.catalog.application.api.dto.ProductSimpleDTO;
import com.autodealer.crm.modules.sales.lead.application.api.dto.TransferClueOwnerRequest;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueMapper;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueOwnerHistoryMapper;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueRemarkMapper;
import com.autodealer.crm.modules.sales.activity.application.api.model.TActivity;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClueOwnerHistory;
import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.pagination.BaseQuery;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueQuery;
import com.autodealer.crm.modules.dictionary.application.api.query.DicQuery;
import com.autodealer.crm.modules.sales.lead.application.api.result.ClueExcelRaw;
import com.autodealer.crm.modules.sales.lead.application.internal.ClueImportValidator;
import com.autodealer.crm.modules.sales.lead.application.internal.ClueImportValidator.ValidatedClueImport;
import com.autodealer.crm.modules.sales.lead.application.api.ClueService;
import com.autodealer.crm.modules.identity.application.api.PhoneNormalizer;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.activity.application.api.enums.ActivityStatus;

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
    private static final String CLUE_STATE_TYPE = "clueState";
    private static final String CLUE_STATE_CONVERTED = "converted";
    private static final String CLUE_STATE_CLOSED = "closed";
    private static final String CLUE_STATE_RESTORE_TARGET = "attempt_contact";

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TClueOwnerHistoryMapper clueOwnerHistoryMapper;

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private CustomerDataPort tCustomerMapper;

    @Resource
    private DictionaryDataPort dicMapper;

    @Resource
    private UserDirectoryDataPort tUserMapper;

    @Resource
    private ActivityDataPort tActivityMapper;

    @Resource
    private ProductCatalogDataPort tProductMapper;

    @Resource
    private ClueImportValidator clueImportValidator;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Resource
    private EmploymentResponsibilityGuard responsibilityGuard;

    @Override
    public PageInfo<TClue> getClueByPage(Integer current, Integer pageSize) {
        // 参数校验
        if (current == null || current < 1) {
            current = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.PAGE_SIZE;
        }
        if (pageSize > 100) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
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
        responsibilityGuard.requireActiveOwner(operatorId);

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
        for (TClue clue : validClues) responsibilityGuard.requireActiveOwner(clue.getOwnerId());

        // 空工作表明确标记为失败
        if (result.getTotalRows() == 0 && result.getFailedRows() == 0 && !result.getErrors().isEmpty()) {
            result.setFailedRows(1);
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

        Set<String> duplicatePhones = new HashSet<>();
        if (!phones.isEmpty()) {
            List<String> existingPhones = tClueMapper.selectExistingPhones(phones);
            if (existingPhones != null && !existingPhones.isEmpty()) {
                Set<String> existingSet = new HashSet<>(existingPhones);
                for (int i = 0; i < validClues.size(); i++) {
                    TClue clue = validClues.get(i);
                    if (existingSet.contains(clue.getPhone())) {
                        duplicatePhones.add(clue.getPhone());
                        result.addError(new ImportRowError(i + 1, "手机号", "该手机号在数据库中已存在"));
                        result.setFailedRows(result.getFailedRows() + 1);
                        result.setValidRows(Math.max(0, result.getValidRows() - 1));
                    }
                }
            }
        }

        List<TClue> insertableClues = validClues.stream()
                .filter(clue -> !duplicatePhones.contains(clue.getPhone()))
                .collect(Collectors.toList());

        if (insertableClues.isEmpty()) {
            return result;
        }

        // 5. 批量写入
        try {
            int affectedRows = tClueMapper.saveClue(insertableClues);
            if (affectedRows != insertableClues.size()) {
                throw new com.autodealer.crm.shared.error.BusinessException(
                        com.autodealer.crm.shared.error.CodeEnum.FAIL,
                        "批量写入影响行数(" + affectedRows + ")不等于待写入数量(" + insertableClues.size() + ")，已全部回滚");
            }
            for (TClue clue : insertableClues) {
                writeOwnerHistory(clue.getId(), null, clue.getOwnerId(), operatorId, "线索导入");
            }
            result.setImportedCount(insertableClues.size());
            auditRecorder.record(AuditActionEnum.CLUE_IMPORT, String.valueOf(insertableClues.size()));
        } catch (DuplicateKeyException e) {
            throw new com.autodealer.crm.shared.error.BusinessException(
                    com.autodealer.crm.shared.error.CodeEnum.DUPLICATE,
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
        String normalizedPhone = PhoneNormalizer.normalizeMainlandMobile(phone);
        if (!PhoneNormalizer.isMainlandMobile(normalizedPhone)) {
            return false;
        }
        int count = tClueMapper.selectByCount(normalizedPhone);
        return count <= 0; //没有查到手机号是true
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveClue(ClueQuery clueQuery) {
        String normalizedPhone = requireValidPhone(clueQuery.getPhone());
        int count = tClueMapper.selectByCount(normalizedPhone);
        if (count <= 0) {
            TClue tClue = new TClue();

            //把前端提交过来的参数数据对象ClueQuery复制到TClue对象中
            //Spring框架有个工具类BeanUtils可以进行对象的复制,复制的条件要求是：两个对象的字段名要相同，字段的类型也相同，这样才可以复制
            BeanUtils.copyProperties(clueQuery, tClue);
            tClue.setPhone(normalizedPhone);
            tClue.setActivityNameSnapshot(resolveActivitySnapshot(tClue.getActivityId()));

            Integer operatorId = currentUserProvider.getCurrentUserId();
            responsibilityGuard.requireActiveOwner(operatorId);
            tClue.setOwnerId(operatorId);
            tClue.setCreateTime(new Date()); //创建时间
            tClue.setCreateBy(operatorId); //创建人id

            int rows;
            try {
                rows = tClueMapper.insertSelective(tClue);
            } catch (DuplicateKeyException e) {
                throw new BusinessException(CodeEnum.DUPLICATE, "该手机号已经录入过了，不能再录入", e);
            }
            if (rows > 0) {
                writeOwnerHistory(tClue.getId(), null, tClue.getOwnerId(), operatorId, "线索创建");
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
        if (tClue.getActivityId() != null) {
            tClue.setActivityNameSnapshot(resolveActivitySnapshot(tClue.getActivityId()));
        }

        // 如果传入的手机号与原记录不同，忽略手机号字段
        String normalizedPhone = PhoneNormalizer.normalizeMainlandMobile(clueQuery.getPhone());
        if (normalizedPhone != null && !PhoneNormalizer.isMainlandMobile(normalizedPhone)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "手机号格式不正确");
        }
        if (normalizedPhone != null && !normalizedPhone.equals(originalClue.getPhone())) {
            tClue.setPhone(null); // 设置为null，让MyBatis的updateByPrimaryKeySelective跳过该字段
        } else if (normalizedPhone != null) {
            tClue.setPhone(normalizedPhone);
        }

        tClue.setEditTime(new Date()); //编辑时间
        tClue.setEditBy(currentUserProvider.getCurrentUserId()); //编辑人id

        int rows = tClueMapper.updateByPrimaryKeySelective(tClue);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "线索归属已变化，请刷新后重试");
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int delClueById(Integer id) {
        if (id == null) {
            return 0;
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        requireAccessibleClue(id);
        requireClueNotReferenced(id);
        // 先删除关联的线索备注
        tClueRemarkMapper.deleteByClueId(id);
        // 再删除线索
        int rows = tClueMapper.deleteScopedByPrimaryKey(id, dataScopeUserId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "线索归属已变化，请刷新后重试");
        }
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
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        distinctIds.forEach(this::requireAccessibleClue);
        distinctIds.forEach(this::requireClueNotReferenced);
        // 先删除关联的线索备注
        for (Integer id : distinctIds) {
            tClueRemarkMapper.deleteByClueId(id);
        }
        // 再删除线索
        int rows = tClueMapper.batchDeleteScopedByIds(distinctIds, dataScopeUserId);
        if (rows != distinctIds.size()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "线索归属已变化，请刷新后重试");
        }
        if (rows > 0) {
            auditRecorder.record(AuditActionEnum.CLUE_DELETE, distinctIds.toString());
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean transferOwner(Integer id, TransferClueOwnerRequest request) {
        TClue clue = requireAccessibleClue(id);
        Integer newOwnerId = requireValidTargetOwner(request.getNewOwnerId());
        if (newOwnerId.equals(clue.getOwnerId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "目标负责人不能与当前负责人相同");
        }
        String reason = normalizeReason(request.getReason());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = tClueMapper.updateOwnerAtomic(
                id, clue.getOwnerId(), newOwnerId, operatorId, currentUserProvider.getDataScopeUserId());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "线索负责人已变化，请刷新后重试");
        }
        writeOwnerHistory(id, clue.getOwnerId(), newOwnerId, operatorId, reason);
        auditRecorder.record(AuditActionEnum.CLUE_TRANSFER, String.valueOf(id));
        return true;
    }

    @Override
    public List<TClueOwnerHistory> getOwnerHistory(Integer id) {
        requireAccessibleClue(id);
        return clueOwnerHistoryMapper.selectByClueId(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean closeClue(Integer id, ClueLifecycleRequest request) {
        TClue clue = requireAccessibleClue(id);
        String reason = normalizeLifecycleReason(request.getReason(), "关闭原因不能为空");
        Integer convertedState = resolveClueStateId(CLUE_STATE_CONVERTED);
        Integer closedState = resolveClueStateId(CLUE_STATE_CLOSED);
        if (closedState.equals(clue.getState())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "线索已经关闭");
        }
        if (convertedState.equals(clue.getState())) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "已转客户线索不能关闭");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = tClueMapper.updateStateAtomic(
                id, clue.getState(), closedState, operatorId, currentUserProvider.getDataScopeUserId());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "线索状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.CLUE_CLOSE, String.valueOf(id), "SUCCESS", reasonSummary(reason));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean restoreClue(Integer id, ClueLifecycleRequest request) {
        TClue clue = requireAccessibleClue(id);
        String reason = normalizeLifecycleReason(request.getReason(), "恢复原因不能为空");
        Integer convertedState = resolveClueStateId(CLUE_STATE_CONVERTED);
        Integer closedState = resolveClueStateId(CLUE_STATE_CLOSED);
        Integer restoreState = resolveClueStateId(CLUE_STATE_RESTORE_TARGET);
        if (!closedState.equals(clue.getState())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "只有已关闭线索可以恢复");
        }
        int activeDuplicates = tClueMapper.countActiveByPhoneExcludingId(
                clue.getPhone(), id, closedState, convertedState);
        if (activeDuplicates > 0) {
            throw new BusinessException(CodeEnum.DUPLICATE, "存在相同手机号的活跃线索，不能恢复");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = tClueMapper.updateStateAtomic(
                id, clue.getState(), restoreState, operatorId, currentUserProvider.getDataScopeUserId());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "线索状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.CLUE_RESTORE, String.valueOf(id), "SUCCESS", reasonSummary(reason));
        return true;
    }

    private Integer requireValidTargetOwner(Integer ownerId) {
        if (ownerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "目标负责人不能为空");
        }
        List<TUser> owners = tUserMapper.selectByOwner();
        boolean exists = owners != null && owners.stream()
                .anyMatch(user -> ownerId.equals(user.getId()));
        if (!exists) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "目标负责人不存在或不可用");
        }
        return ownerId;
    }

    private String resolveActivitySnapshot(Integer activityId) {
        if (activityId == null) {
            return null;
        }
        TActivity activity = tActivityMapper.selectDetailByPrimaryKey(
                activityId, currentUserProvider.getDataScopeUserId());
        if (activity == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "来源活动不存在或无权访问");
        }
        ActivityStatus status = ActivityStatus.parse(activity.getStatus());
        if (status == ActivityStatus.DRAFT || status.terminal()) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "只有待开始或进行中的活动可以作为线索来源");
        }
        return activity.getName();
    }

    private void writeOwnerHistory(Integer clueId, Integer fromOwnerId, Integer toOwnerId,
                                   Integer assignedBy, String reason) {
        TClueOwnerHistory history = new TClueOwnerHistory();
        history.setClueId(clueId);
        history.setFromOwnerId(fromOwnerId);
        history.setToOwnerId(toOwnerId);
        history.setAssignedBy(assignedBy);
        history.setReason(reason);
        history.setAssignedTime(new Date());
        clueOwnerHistoryMapper.insert(history);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "转派原因不能为空");
        }
        return reason.trim();
    }

    private String normalizeLifecycleReason(String reason, String message) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        return reason.trim();
    }

    private Integer resolveClueStateId(String valueCode) {
        DicQuery query = new DicQuery();
        query.setTypeCode(CLUE_STATE_TYPE);
        List<TDicValue> states = dicMapper.selectDicValues(query);
        if (states == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "线索状态字典缺失: " + valueCode);
        }
        return states.stream()
                .filter(state -> valueCode.equals(state.getValueCode()))
                .map(TDicValue::getId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(CodeEnum.NOT_FOUND, "线索状态字典缺失: " + valueCode));
    }

    private String reasonSummary(String reason) {
        return "{\"reason\":\"" + escapeJson(reason) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String requireValidPhone(String phone) {
        String normalizedPhone = PhoneNormalizer.normalizeMainlandMobile(phone);
        if (!PhoneNormalizer.isMainlandMobile(normalizedPhone)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "手机号格式不正确");
        }
        return normalizedPhone;
    }

    private TClue requireAccessibleClue(Integer id) {
        TClue clue = tClueMapper.selectScopedByPrimaryKey(
                id, currentUserProvider.getDataScopeUserId());
        if (clue == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "线索不存在或无权访问");
        }
        return clue;
    }

    private void requireClueNotReferenced(Integer id) {
        if (tCustomerMapper.countByClueId(id) > 0) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "线索已转客户或存在客户引用，不能删除");
        }
    }
}
