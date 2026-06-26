package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.mapper.TActivityRemarkMapper;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.model.TActivityRemark;
import com.autodealer.crm.query.ActivityRemarkQuery;
import com.autodealer.crm.service.ActivityRemarkService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;

import java.util.Date;
import java.util.List;

@Service
public class ActivityRemarkServiceImpl implements ActivityRemarkService {

    @Resource
    private TActivityRemarkMapper tActivityRemarkMapper;

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveActivityRemark(ActivityRemarkQuery activityRemarkQuery) {
        requireAccessibleActivity(activityRemarkQuery.getActivityId());
        TActivityRemark tActivityRemark = new TActivityRemark();

        //把ActivityRemarkQuery对象里面的属性数据复制到TActivityRemark对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityRemarkQuery, tActivityRemark);

        tActivityRemark.setCreateTime(new Date()); //创建时间

        tActivityRemark.setCreateBy(currentUserProvider.getCurrentUserId()); //创建人

        return tActivityRemarkMapper.insertSelective(tActivityRemark);
    }

    @Override
    public PageInfo<TActivityRemark> getActivityRemarkByPage(Integer current, Integer pageSize, ActivityRemarkQuery activityRemarkQuery) {
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
        List<TActivityRemark> list = tActivityRemarkMapper.selectActivityRemarkByPage(activityRemarkQuery);
        // 3.封装分页数据到PageInfo
        PageInfo<TActivityRemark> info = new PageInfo<>(list);
        return info;
    }

    @Override
    public TActivityRemark getActivityRemarkById(Integer id) {
        return requireAccessibleRemark(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateActivityRemark(ActivityRemarkQuery activityRemarkQuery) {
        requireAccessibleRemark(activityRemarkQuery.getId());
        TActivityRemark tActivityRemark = new TActivityRemark();

        //把ActivityRemarkQuery对象里面的属性数据复制到TActivityRemark对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityRemarkQuery, tActivityRemark);

        tActivityRemark.setEditTime(new Date()); //编辑时间

        tActivityRemark.setEditBy(currentUserProvider.getCurrentUserId()); //编辑人

        return tActivityRemarkMapper.updateByPrimaryKeySelective(tActivityRemark);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int delActivityRemarkById(Integer id) {
        requireAccessibleRemark(id);
        //逻辑删除：不删数据，只是修改一下数据的状态，数据依然还在表里面；
        //物理删除：真正的把数据从表里面删掉
        TActivityRemark tActivityRemark = new TActivityRemark();
        tActivityRemark.setId(id);
        tActivityRemark.setDeleted(1); //删除状态（null或者0正常，1删除）
        return tActivityRemarkMapper.updateByPrimaryKeySelective(tActivityRemark);
    }

    private void requireAccessibleActivity(Integer activityId) {
        if (tActivityMapper.selectDetailByPrimaryKey(
                activityId, currentUserProvider.getDataScopeUserId()) == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "市场活动不存在或无权访问");
        }
    }

    private TActivityRemark requireAccessibleRemark(Integer remarkId) {
        TActivityRemark remark = tActivityRemarkMapper.selectScopedByPrimaryKey(
                remarkId, currentUserProvider.getDataScopeUserId());
        if (remark == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "市场活动备注不存在或无权访问");
        }
        return remark;
    }
}
