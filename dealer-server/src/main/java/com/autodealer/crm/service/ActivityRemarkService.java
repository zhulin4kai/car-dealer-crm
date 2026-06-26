package com.autodealer.crm.service;

import com.autodealer.crm.model.TActivityRemark;
import com.autodealer.crm.query.ActivityRemarkQuery;
import com.github.pagehelper.PageInfo;

public interface ActivityRemarkService {

    int saveActivityRemark(ActivityRemarkQuery activityRemarkQuery);

    PageInfo<TActivityRemark> getActivityRemarkByPage(Integer current, Integer pageSize, ActivityRemarkQuery activityRemarkQuery);

    TActivityRemark getActivityRemarkById(Integer id);

    int updateActivityRemark(ActivityRemarkQuery activityRemarkQuery);

    int delActivityRemarkById(Integer id);
}
