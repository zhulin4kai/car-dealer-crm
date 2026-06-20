package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.service.ActivityService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Override
    public PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityQuery) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TActivity> list = tActivityMapper.selectActivityByPage(activityQuery);
        // 3.封装分页数据到PageInfo
        PageInfo<TActivity> info = new PageInfo<>(list);
        return info;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveActivity(ActivityQuery activityQuery) {
        TActivity tActivity = new TActivity();

        //把ActivityQuery对象里面的属性数据复制到TActivity对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityQuery, tActivity);

        Integer operatorId = currentUserProvider.getCurrentUserId();
        tActivity.setOwnerId(operatorId);
        tActivity.setCreateTime(new Date()); //创建时间
        tActivity.setCreateBy(operatorId); //创建人

        return tActivityMapper.insertSelective(tActivity);
    }

    @Override
    public TActivity getActivityById(Integer id) {
        return tActivityMapper.selectDetailByPrimaryKey(id, currentUserProvider.getDataScopeUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateActivity(ActivityQuery activityQuery) {
        TActivity existing = requireAccessibleActivity(activityQuery.getId());
        TActivity tActivity = new TActivity();

        //把ActivityQuery对象里面的属性数据复制到TActivity对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityQuery, tActivity);
        tActivity.setOwnerId(existing.getOwnerId());

        tActivity.setEditTime(new Date()); //编辑时间

        tActivity.setEditBy(currentUserProvider.getCurrentUserId()); //编辑人

        return tActivityMapper.updateByPrimaryKeySelective(tActivity);
    }

    @Override
    public List<TActivity> getOngoingActivity() {
        return tActivityMapper.selecOngoingActivity(currentUserProvider.getDataScopeUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteActivities(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Integer> distinctIds = ids.stream().distinct().sorted().toList();
        distinctIds.forEach(this::requireAccessibleActivity);
        return tActivityMapper.batchDeleteByIds(distinctIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivity(Integer id) {
        if (id == null) {
            return 0;
        }
        requireAccessibleActivity(id);
        return tActivityMapper.deleteByPrimaryKey(id);
    }

    private TActivity requireAccessibleActivity(Integer id) {
        TActivity activity = tActivityMapper.selectDetailByPrimaryKey(
                id, currentUserProvider.getDataScopeUserId());
        if (activity == null) {
            throw new RuntimeException("市场活动不存在或无权访问");
        }
        return activity;
    }
}
