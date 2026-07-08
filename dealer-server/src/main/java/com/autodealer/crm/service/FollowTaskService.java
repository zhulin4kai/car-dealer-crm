package com.autodealer.crm.service;

import com.autodealer.crm.dto.CancelFollowTaskRequest;
import com.autodealer.crm.dto.CompleteFollowTaskRequest;
import com.autodealer.crm.dto.CreateFollowTaskRequest;
import com.autodealer.crm.dto.PostponeFollowTaskRequest;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.query.FollowTaskQuery;
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
