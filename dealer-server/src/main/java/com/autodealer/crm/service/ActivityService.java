package com.autodealer.crm.service;

import com.autodealer.crm.dto.ActivityLifecycleRequest;
import com.autodealer.crm.dto.ActivityRoiResponse;
import com.autodealer.crm.dto.CreateActivityRequest;
import com.autodealer.crm.dto.ReviewActivityRequest;
import com.autodealer.crm.dto.UpdateActivityRequest;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.result.ActivityExportRow;
import com.github.pagehelper.PageInfo;

import java.util.List;

// Easy Code 插件（Idea插件生成  controller 、service、dao）

public interface ActivityService {

    PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityQuery);

    int saveActivity(CreateActivityRequest request);

    TActivity getActivityById(Integer id);

    int updateActivity(UpdateActivityRequest request);

    List<TActivity> getOngoingActivity();

    TActivity publishActivity(Integer id);

    TActivity startActivity(Integer id);

    TActivity endActivity(Integer id);

    TActivity reviewActivity(Integer id, ReviewActivityRequest request);

    TActivity cancelActivity(Integer id, ActivityLifecycleRequest request);

    TActivity closeActivity(Integer id, ActivityLifecycleRequest request);

    ActivityRoiResponse getActivityRoi(Integer id);

    List<ActivityExportRow> exportActivityRoi(ActivityQuery query);

    int batchDeleteActivities(List<Integer> ids);

    int deleteActivity(Integer id);
}
