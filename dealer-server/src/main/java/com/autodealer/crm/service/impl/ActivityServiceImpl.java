package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.dto.ActivityLifecycleRequest;
import com.autodealer.crm.dto.ActivityRoiResponse;
import com.autodealer.crm.dto.CreateActivityRequest;
import com.autodealer.crm.dto.ReviewActivityRequest;
import com.autodealer.crm.dto.UpdateActivityRequest;
import com.autodealer.crm.enums.ActivityStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.result.ActivityExportRow;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.ActivityService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final int EXPORT_MAX_ROWS = 10000;

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Override
    public PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityQuery) {
        if (current == null || current < 1) {
            current = 1;
        }
        int pageSize = normalizePageSize(activityQuery);
        PageHelper.startPage(current, pageSize);
        List<TActivity> list = tActivityMapper.selectActivityByPage(activityQuery);
        return new PageInfo<>(list);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveActivity(CreateActivityRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        TActivity activity = new TActivity();
        activity.setOwnerId(operatorId);
        activity.setName(request.getName().trim());
        activity.setStatus(ActivityStatus.DRAFT.name());
        activity.setChannel(request.getChannel().trim());
        activity.setTargetModel(trimToNull(request.getTargetModel()));
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCost(request.getCost());
        activity.setDescription(trimToNull(request.getDescription()));
        activity.setCreateTime(new Date());
        activity.setCreateBy(operatorId);
        int rows = tActivityMapper.insertSelective(activity);
        if (rows == 1) {
            auditRecorder.record(AuditActionEnum.ACTIVITY_CREATE, String.valueOf(activity.getId()));
        }
        return rows;
    }

    @Override
    public TActivity getActivityById(Integer id) {
        return requireAccessibleActivity(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateActivity(UpdateActivityRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        TActivity existing = requireAccessibleActivity(request.getId());
        ActivityStatus status = ActivityStatus.parse(existing.getStatus());
        if (status.locksCoreFacts()) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "活动核心事实已锁定，不能编辑");
        }
        TActivity activity = new TActivity();
        activity.setId(request.getId());
        activity.setOwnerId(existing.getOwnerId());
        activity.setName(request.getName().trim());
        activity.setChannel(request.getChannel().trim());
        activity.setTargetModel(trimToNull(request.getTargetModel()));
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCost(request.getCost());
        activity.setDescription(trimToNull(request.getDescription()));
        activity.setEditTime(new Date());
        activity.setEditBy(currentUserProvider.getCurrentUserId());
        int rows = tActivityMapper.updateByPrimaryKeySelective(activity);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "活动已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.ACTIVITY_UPDATE, String.valueOf(request.getId()));
        return rows;
    }

    @Override
    public List<TActivity> getOngoingActivity() {
        return tActivityMapper.selecOngoingActivity(currentUserProvider.getDataScopeUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TActivity publishActivity(Integer id) {
        return transition(id, ActivityStatus.PLANNED, ActivityStatus::canPublish, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TActivity startActivity(Integer id) {
        return transition(id, ActivityStatus.ONGOING, ActivityStatus::canStart, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TActivity endActivity(Integer id) {
        return transition(id, ActivityStatus.ENDED, ActivityStatus::canEnd, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TActivity reviewActivity(Integer id, ReviewActivityRequest request) {
        TActivity current = requireAccessibleActivity(id);
        ActivityStatus status = ActivityStatus.parse(current.getStatus());
        if (!status.canReview()) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "只有已结束活动可以复盘");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        TActivity review = new TActivity();
        review.setActualCost(request.getActualCost());
        review.setResultSummary(request.getResultSummary().trim());
        review.setReviewConclusion(request.getReviewConclusion().trim());
        review.setReviewedBy(operatorId);
        review.setReviewedTime(new Date());
        review.setEditBy(operatorId);
        review.setEditTime(review.getReviewedTime());
        int rows = tActivityMapper.reviewAtomic(
                id, ActivityStatus.ENDED.name(), review, currentUserProvider.getDataScopeUserId());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "活动状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.ACTIVITY_REVIEW, String.valueOf(id),
                "SUCCESS", "{\"actualCost\":\"" + request.getActualCost() + "\"}");
        return requireAccessibleActivity(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TActivity cancelActivity(Integer id, ActivityLifecycleRequest request) {
        return transition(id, ActivityStatus.CANCELED, ActivityStatus::canCancel, normalizeReason(request));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TActivity closeActivity(Integer id, ActivityLifecycleRequest request) {
        return transition(id, ActivityStatus.CLOSED, ActivityStatus::canClose, normalizeReason(request));
    }

    @Override
    public ActivityRoiResponse getActivityRoi(Integer id) {
        ActivityRoiResponse response = tActivityMapper.selectActivityRoi(
                id, currentUserProvider.getDataScopeUserId());
        if (response == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "市场活动不存在或无权访问");
        }
        return response;
    }

    @Override
    public List<ActivityExportRow> exportActivityRoi(ActivityQuery query) {
        List<ActivityExportRow> rows = tActivityMapper.selectActivityExportRows(query);
        if (rows.size() > EXPORT_MAX_ROWS) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED,
                    "导出数据量超出限制，最多导出 " + EXPORT_MAX_ROWS + " 条");
        }
        auditRecorder.recordQuietly(AuditActionEnum.ACTIVITY_EXPORT, "export",
                "SUCCESS", "{\"count\":" + rows.size() + "}");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteActivities(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Integer> distinctIds = ids.stream().distinct().sorted().toList();
        for (Integer id : distinctIds) {
            requireDraftWithoutBusinessReferences(id);
        }
        int rows = tActivityMapper.batchDeleteByIds(distinctIds);
        if (rows != distinctIds.size()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "活动已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.ACTIVITY_DELETE, distinctIds.toString());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivity(Integer id) {
        if (id == null) {
            return 0;
        }
        requireDraftWithoutBusinessReferences(id);
        int rows = tActivityMapper.deleteByPrimaryKey(id);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "活动已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.ACTIVITY_DELETE, String.valueOf(id));
        return rows;
    }

    private TActivity transition(Integer id,
                                 ActivityStatus toStatus,
                                 java.util.function.Predicate<ActivityStatus> validator,
                                 String reason) {
        TActivity current = requireAccessibleActivity(id);
        ActivityStatus fromStatus = ActivityStatus.parse(current.getStatus());
        if (!validator.test(fromStatus)) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "活动状态不允许执行该操作");
        }
        int rows = tActivityMapper.updateStatusAtomic(
                id, fromStatus.name(), toStatus.name(), currentUserProvider.getCurrentUserId(),
                reason, currentUserProvider.getDataScopeUserId());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "活动状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.ACTIVITY_STATUS_CHANGE, String.valueOf(id),
                "SUCCESS", "{\"from\":\"" + fromStatus.name() + "\",\"to\":\"" + toStatus.name() + "\"}");
        return requireAccessibleActivity(id);
    }

    private void requireDraftWithoutBusinessReferences(Integer id) {
        TActivity activity = requireAccessibleActivity(id);
        ActivityStatus status = ActivityStatus.parse(activity.getStatus());
        if (status != ActivityStatus.DRAFT) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "只有未产生业务事实的草稿活动可以删除");
        }
        Integer references = tActivityMapper.countBusinessReferences(id);
        if (references != null && references > 0) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "市场活动已产生业务引用，不能物理删除");
        }
    }

    private TActivity requireAccessibleActivity(Integer id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "活动ID不能为空");
        }
        TActivity activity = tActivityMapper.selectDetailByPrimaryKey(
                id, currentUserProvider.getDataScopeUserId());
        if (activity == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "市场活动不存在或无权访问");
        }
        return activity;
    }

    private void validateTimeRange(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "活动时间不能为空");
        }
        if (!startTime.before(endTime)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "活动结束时间必须晚于开始时间");
        }
    }

    private int normalizePageSize(ActivityQuery query) {
        if (query == null) {
            return Constants.PAGE_SIZE;
        }
        Integer pageSize = query.getPageSize();
        if (pageSize == null || pageSize < 1) {
            return Constants.PAGE_SIZE;
        }
        if (pageSize > 100) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        return pageSize;
    }

    private String normalizeReason(ActivityLifecycleRequest request) {
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "原因不能为空");
        }
        return request.getReason().trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
