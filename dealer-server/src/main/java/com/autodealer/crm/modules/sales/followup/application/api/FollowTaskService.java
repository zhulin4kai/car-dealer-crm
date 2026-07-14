package com.autodealer.crm.modules.sales.followup.application.api;

import com.autodealer.crm.modules.sales.followup.application.api.dto.CancelFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CompleteFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.PostponeFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.model.TFollowTask;
import com.autodealer.crm.modules.sales.followup.application.api.query.FollowTaskQuery;
import com.github.pagehelper.PageInfo;

public interface FollowTaskService {
    PageInfo<TFollowTask> getFollowTaskPage(FollowTaskQuery query);

    PageInfo<TFollowTask> getFollowTaskPageReadOnly(FollowTaskQuery query);

    TFollowTask createFollowTask(CreateFollowTaskRequest request);

    TFollowTask getFollowTask(Long id);

    TFollowTask startFollowTask(Long id);

    TFollowTask postponeFollowTask(Long id, PostponeFollowTaskRequest request);

    TFollowTask cancelFollowTask(Long id, CancelFollowTaskRequest request);

    TFollowTask completeFollowTask(Long id, CompleteFollowTaskRequest request);
}
