package com.autodealer.crm.service.impl;

import com.alibaba.excel.EasyExcel;
import com.autodealer.crm.config.converter.ClueExcelConverter;
import com.autodealer.crm.config.listener.UploadDataListener;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TClueRemarkMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.query.BaseQuery;
import com.autodealer.crm.query.ClueQuery;
import com.autodealer.crm.result.ClueExcel;
import com.autodealer.crm.service.ClueService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Service
public class ClueServiceImpl implements ClueService {

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private ClueExcelConverter clueExcelConverter;

    @Resource
    private CurrentUserProvider currentUserProvider;

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
    public void importExcel(InputStream inputStream) {
        //链式编程，3个参数, 第一个参数是要读取的Excel文件，第二个参数是Excel模板类，第三个参数是文件读取的监听器
        EasyExcel.read(inputStream, ClueExcel.class,
                        new UploadDataListener(tClueMapper, currentUserProvider.getCurrentUserId(), clueExcelConverter))
                .sheet()
                .doRead();
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

            return tClueMapper.insertSelective(tClue);
        } else {
            throw new RuntimeException("该手机号已经录入过了，不能再录入");
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
        return tClueMapper.deleteByPrimaryKey(id);
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
        return tClueMapper.batchDeleteByIds(distinctIds);
    }

    private TClue requireAccessibleClue(Integer id) {
        TClue clue = tClueMapper.selectScopedByPrimaryKey(
                id, currentUserProvider.getDataScopeUserId());
        if (clue == null) {
            throw new RuntimeException("线索不存在或无权访问");
        }
        return clue;
    }
}
