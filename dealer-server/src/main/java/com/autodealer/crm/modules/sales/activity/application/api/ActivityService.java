package com.autodealer.crm.modules.sales.activity.application.api;

import com.autodealer.crm.modules.sales.activity.application.api.dto.ActivityLifecycleRequest;
import com.autodealer.crm.modules.sales.activity.application.api.dto.ActivityRoiResponse;
import com.autodealer.crm.modules.sales.activity.application.api.dto.CreateActivityRequest;
import com.autodealer.crm.modules.sales.activity.application.api.dto.ReviewActivityRequest;
import com.autodealer.crm.modules.sales.activity.application.api.dto.UpdateActivityRequest;
import com.autodealer.crm.modules.sales.activity.application.api.model.TActivity;
import com.autodealer.crm.modules.sales.activity.application.api.query.ActivityQuery;
import com.autodealer.crm.modules.sales.activity.application.api.result.ActivityExportRow;
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
