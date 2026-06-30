package com.autodealer.crm.ai.service.impl;

import com.autodealer.crm.ai.dto.AiProactiveEventResponse;
import com.autodealer.crm.ai.dto.AiProactiveSubscriptionResponse;
import com.autodealer.crm.ai.dto.CreateAiProactiveSubscriptionRequest;
import com.autodealer.crm.ai.enums.AiProactiveEventStatus;
import com.autodealer.crm.ai.enums.AiProactiveFrequency;
import com.autodealer.crm.ai.enums.AiProactiveSubscriptionStatus;
import com.autodealer.crm.ai.enums.AiProactiveSubscriptionType;
import com.autodealer.crm.ai.mapper.TAiProactiveEventMapper;
import com.autodealer.crm.ai.mapper.TAiProactiveSubscriptionMapper;
import com.autodealer.crm.ai.model.TAiProactiveEvent;
import com.autodealer.crm.ai.model.TAiProactiveSubscription;
import com.autodealer.crm.ai.service.AiProactiveService;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.FollowTaskQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.FollowTaskService;
import com.autodealer.crm.service.ProductService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AiProactiveServiceImpl implements AiProactiveService {
    private static final int MAX_EVENT_PAGE_SIZE = 100;
    private static final int MAX_DUE_SUBSCRIPTIONS = 20;

    private final TAiProactiveSubscriptionMapper subscriptionMapper;
    private final TAiProactiveEventMapper eventMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AiSensitiveDataSanitizer sanitizer;
    private final ProductService productService;
    private final FollowTaskService followTaskService;

    public AiProactiveServiceImpl(TAiProactiveSubscriptionMapper subscriptionMapper,
                                  TAiProactiveEventMapper eventMapper,
                                  CurrentUserProvider currentUserProvider,
                                  AiSensitiveDataSanitizer sanitizer,
                                  ProductService productService,
                                  FollowTaskService followTaskService) {
        this.subscriptionMapper = subscriptionMapper;
        this.eventMapper = eventMapper;
        this.currentUserProvider = currentUserProvider;
        this.sanitizer = sanitizer;
        this.productService = productService;
        this.followTaskService = followTaskService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProactiveSubscriptionResponse createSubscription(CreateAiProactiveSubscriptionRequest request) {
        AiProactiveSubscriptionType type = parseType(request.getSubscriptionType());
        AiProactiveFrequency frequency = parseFrequency(request.getFrequency());
        LocalDateTime now = LocalDateTime.now();
        TAiProactiveSubscription subscription = new TAiProactiveSubscription();
        subscription.setSubscriptionNo("AIPS" + UUID.randomUUID().toString().replace("-", ""));
        subscription.setUserId(currentUserProvider.getCurrentUserId());
        subscription.setSubscriptionType(type.name());
        subscription.setStatus(AiProactiveSubscriptionStatus.ACTIVE.name());
        subscription.setFrequency(frequency.name());
        subscription.setQuietStartTime(normalizeQuiet(request.getQuietStartTime()));
        subscription.setQuietEndTime(normalizeQuiet(request.getQuietEndTime()));
        subscription.setDailyLimit(request.getDailyLimit() == null ? 5 : request.getDailyLimit());
        subscription.setMaxResults(request.getMaxResults() == null ? 10 : request.getMaxResults());
        subscription.setDuplicateWindowMinutes(request.getDuplicateWindowMinutes() == null
                ? 60 : request.getDuplicateWindowMinutes());
        subscription.setConfigSummary(sanitizer.sanitize(request.getConfigSummary(), 1000));
        subscription.setNextTriggerTime(now);
        subscription.setCreateTime(now);
        subscription.setCreateBy(currentUserProvider.getCurrentUserId());
        requireOne(subscriptionMapper.insert(subscription), "AI 主动提醒订阅写入失败");
        return AiProactiveSubscriptionResponse.from(subscription);
    }

    @Override
    public List<AiProactiveSubscriptionResponse> listSubscriptions() {
        return subscriptionMapper.selectByUserId(currentUserProvider.getCurrentUserId()).stream()
                .map(AiProactiveSubscriptionResponse::from)
                .toList();
    }

    @Override
    public AiProactiveSubscriptionResponse getSubscription(String subscriptionNo) {
        return AiProactiveSubscriptionResponse.from(requireOwnedSubscription(subscriptionNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProactiveSubscriptionResponse pauseSubscription(String subscriptionNo) {
        TAiProactiveSubscription subscription = requireOwnedSubscription(subscriptionNo);
        requireStatus(subscription, AiProactiveSubscriptionStatus.ACTIVE);
        updateStatus(subscription, AiProactiveSubscriptionStatus.PAUSED);
        return getSubscription(subscriptionNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProactiveSubscriptionResponse resumeSubscription(String subscriptionNo) {
        TAiProactiveSubscription subscription = requireOwnedSubscription(subscriptionNo);
        requireStatus(subscription, AiProactiveSubscriptionStatus.PAUSED);
        updateStatus(subscription, AiProactiveSubscriptionStatus.ACTIVE);
        return getSubscription(subscriptionNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProactiveSubscriptionResponse cancelSubscription(String subscriptionNo) {
        TAiProactiveSubscription subscription = requireOwnedSubscription(subscriptionNo);
        if (AiProactiveSubscriptionStatus.CANCELLED.name().equals(subscription.getStatus())) {
            return AiProactiveSubscriptionResponse.from(subscription);
        }
        updateStatus(subscription, AiProactiveSubscriptionStatus.CANCELLED);
        return getSubscription(subscriptionNo);
    }

    @Override
    public List<AiProactiveEventResponse> listEvents(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, MAX_EVENT_PAGE_SIZE));
        int offset = (safePage - 1) * safeSize;
        return eventMapper.selectByUserId(currentUserProvider.getCurrentUserId(), offset, safeSize).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    public AiProactiveEventResponse getEvent(String eventNo) {
        TAiProactiveEvent event = eventMapper.selectOwnedByEventNo(
                eventNo, currentUserProvider.getCurrentUserId());
        if (event == null) {
            throw new BusinessException(CodeEnum.AI_PROACTIVE_EVENT_NOT_FOUND, "AI 主动提醒事件不存在或无权访问");
        }
        return toEventResponse(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AiProactiveEventResponse> generateDueEvents() {
        requireCurrentUserEnabled();
        Integer userId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        return subscriptionMapper.selectActiveDueByUserId(userId, now, MAX_DUE_SUBSCRIPTIONS).stream()
                .map(subscription -> generateForSubscription(subscription, now))
                .map(this::toEventResponse)
                .toList();
    }

    private TAiProactiveEvent generateForSubscription(TAiProactiveSubscription subscription,
                                                      LocalDateTime now) {
        AiProactiveSubscriptionType type = AiProactiveSubscriptionType.valueOf(subscription.getSubscriptionType());
        TAiProactiveEvent event;
        if (!currentUserProvider.hasAuthority(permissionFor(type))) {
            event = skipped(subscription, now, "权限变化，已跳过本次提醒", CodeEnum.AI_PROACTIVE_FORBIDDEN.name());
        } else if (inQuietTime(subscription, now.toLocalTime())) {
            event = skipped(subscription, now, "当前处于静默时间，已跳过本次提醒", "AI_PROACTIVE_QUIET_TIME");
        } else if (dailyLimitReached(subscription, now)) {
            event = skipped(subscription, now, "已达到当天提醒数量上限", "AI_PROACTIVE_DAILY_LIMIT");
        } else if (hasRecentDuplicate(subscription, now)) {
            event = skipped(subscription, now, "重复提醒已合并", "AI_PROACTIVE_DUPLICATED");
        } else {
            event = buildBusinessEvent(subscription, type, now);
        }
        requireOne(eventMapper.insert(event), "AI 主动提醒事件写入失败");
        subscriptionMapper.updateTriggerTime(subscription.getId(), now,
                nextTriggerTime(subscription, now), currentUserProvider.getCurrentUserId());
        return event;
    }

    private TAiProactiveEvent buildBusinessEvent(TAiProactiveSubscription subscription,
                                                 AiProactiveSubscriptionType type,
                                                 LocalDateTime now) {
        return switch (type) {
            case INVENTORY_ALERT -> inventoryAlertEvent(subscription, now);
            case FOLLOW_UP_REMINDER -> followUpEvent(subscription, now);
            case TRANSACTION_EXCEPTION, DAILY_SUMMARY, PERIODIC_SALES_ANALYSIS ->
                    genericSummaryEvent(subscription, now, type);
        };
    }

    private TAiProactiveEvent inventoryAlertEvent(TAiProactiveSubscription subscription, LocalDateTime now) {
        PageInfo<TProduct> page = productService.getStockAlerts(1, subscription.getMaxResults());
        int count = page.getList() == null ? 0 : page.getList().size();
        return event(subscription, now, AiProactiveEventStatus.READY,
                "库存预警摘要",
                "当前可见库存预警 " + count + " 条",
                "库存预警基于 ProductService.getStockAlerts 生成。",
                "PRODUCT_STOCK_ALERT",
                subscription.getSubscriptionNo(),
                count > 0 ? "HIGH" : "LOW",
                null);
    }

    private TAiProactiveEvent followUpEvent(TAiProactiveSubscription subscription, LocalDateTime now) {
        FollowTaskQuery query = new FollowTaskQuery();
        query.setPage(1);
        query.setSize(subscription.getMaxResults());
        query.setOverdueOnly(true);
        PageInfo<TFollowTask> page = followTaskService.getFollowTaskPage(query);
        int count = page.getList() == null ? 0 : page.getList().size();
        return event(subscription, now, count == 0 ? AiProactiveEventStatus.NO_DATA : AiProactiveEventStatus.READY,
                "跟进任务提醒",
                "当前可见逾期或待处理跟进 " + count + " 条",
                "跟进提醒基于 FollowTaskService.getFollowTaskPage 生成。",
                "FOLLOW_TASK",
                subscription.getSubscriptionNo(),
                count > 0 ? "MEDIUM" : "LOW",
                null);
    }

    private TAiProactiveEvent genericSummaryEvent(TAiProactiveSubscription subscription,
                                                  LocalDateTime now,
                                                  AiProactiveSubscriptionType type) {
        return event(subscription, now, AiProactiveEventStatus.NO_DATA,
                "周期摘要",
                "当前只读口径暂无可生成摘要的数据",
                "摘要未直接访问数据库或修改普通业务口径。",
                type.name(),
                subscription.getSubscriptionNo(),
                "LOW",
                null);
    }

    private TAiProactiveEvent skipped(TAiProactiveSubscription subscription,
                                      LocalDateTime now,
                                      String summary,
                                      String errorCode) {
        return event(subscription, now, AiProactiveEventStatus.SKIPPED,
                "主动提醒已跳过",
                summary,
                summary,
                subscription.getSubscriptionType(),
                subscription.getSubscriptionNo(),
                "LOW",
                errorCode);
    }

    private TAiProactiveEvent event(TAiProactiveSubscription subscription,
                                    LocalDateTime now,
                                    AiProactiveEventStatus status,
                                    String title,
                                    String summary,
                                    String detailSummary,
                                    String objectType,
                                    String objectId,
                                    String severity,
                                    String errorCode) {
        TAiProactiveEvent event = new TAiProactiveEvent();
        event.setEventNo("AIPE" + UUID.randomUUID().toString().replace("-", ""));
        event.setSubscriptionId(subscription.getId());
        event.setUserId(subscription.getUserId());
        event.setEventType(subscription.getSubscriptionType());
        event.setStatus(status.name());
        event.setTitle(sanitizer.sanitize(title, 128));
        event.setSummary(sanitizer.sanitize(summary, 1000));
        event.setDetailSummary(sanitizer.sanitize(detailSummary, 2000));
        event.setObjectType(sanitizer.sanitize(objectType, 64));
        event.setObjectId(sanitizer.sanitize(objectId, 64));
        event.setSeverity(severity);
        event.setGeneratedTime(now);
        event.setErrorCode(sanitizer.sanitize(errorCode, 64));
        event.setCreateTime(now);
        event.setCreateBy(currentUserProvider.getCurrentUserId());
        return event;
    }

    private AiProactiveSubscriptionType parseType(String value) {
        try {
            return AiProactiveSubscriptionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "AI 主动提醒订阅类型不支持", ex);
        }
    }

    private AiProactiveFrequency parseFrequency(String value) {
        try {
            return AiProactiveFrequency.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "AI 主动提醒频率不支持", ex);
        }
    }

    private String permissionFor(AiProactiveSubscriptionType type) {
        return switch (type) {
            case FOLLOW_UP_REMINDER -> PermissionCodes.FOLLOW_TASK_LIST;
            case TRANSACTION_EXCEPTION -> PermissionCodes.TRAN_VIEW;
            case INVENTORY_ALERT -> PermissionCodes.PRODUCT_STOCK_VIEW;
            case DAILY_SUMMARY, PERIODIC_SALES_ANALYSIS -> PermissionCodes.STATISTIC_VIEW;
        };
    }

    private boolean dailyLimitReached(TAiProactiveSubscription subscription, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        return eventMapper.countBySubscriptionAfter(subscription.getId(), startOfDay)
                >= subscription.getDailyLimit();
    }

    private boolean hasRecentDuplicate(TAiProactiveSubscription subscription, LocalDateTime now) {
        LocalDateTime since = now.minusMinutes(subscription.getDuplicateWindowMinutes());
        return eventMapper.selectDuplicateAfter(subscription.getId(), subscription.getSubscriptionType(),
                subscription.getSubscriptionNo(), since) != null;
    }

    private boolean inQuietTime(TAiProactiveSubscription subscription, LocalTime now) {
        if (!StringUtils.hasText(subscription.getQuietStartTime())
                || !StringUtils.hasText(subscription.getQuietEndTime())) {
            return false;
        }
        LocalTime start = LocalTime.parse(subscription.getQuietStartTime());
        LocalTime end = LocalTime.parse(subscription.getQuietEndTime());
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private LocalDateTime nextTriggerTime(TAiProactiveSubscription subscription, LocalDateTime now) {
        AiProactiveFrequency frequency = AiProactiveFrequency.valueOf(subscription.getFrequency());
        return switch (frequency) {
            case REALTIME_LIMITED -> now.plusMinutes(15);
            case DAILY -> now.plusDays(1);
            case WEEKLY -> now.plusWeeks(1);
            case MONTHLY -> now.plusMonths(1);
        };
    }

    private String normalizeQuiet(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private void requireCurrentUserEnabled() {
        TUser currentUser = currentUserProvider.getCurrentUser();
        if (!Integer.valueOf(1).equals(currentUser.getAccountEnabled())) {
            throw new BusinessException(CodeEnum.AI_PROACTIVE_FORBIDDEN, "当前用户已停用，不能生成主动提醒");
        }
    }

    private TAiProactiveSubscription requireOwnedSubscription(String subscriptionNo) {
        TAiProactiveSubscription subscription = subscriptionMapper.selectOwnedBySubscriptionNo(
                subscriptionNo, currentUserProvider.getCurrentUserId());
        if (subscription == null) {
            throw new BusinessException(CodeEnum.AI_PROACTIVE_SUBSCRIPTION_NOT_FOUND, "AI 主动提醒订阅不存在或无权访问");
        }
        return subscription;
    }

    private void requireStatus(TAiProactiveSubscription subscription,
                               AiProactiveSubscriptionStatus expectedStatus) {
        if (!expectedStatus.name().equals(subscription.getStatus())) {
            throw new BusinessException(CodeEnum.AI_PROACTIVE_STATE_CONFLICT, "AI 主动提醒状态冲突");
        }
    }

    private void updateStatus(TAiProactiveSubscription subscription,
                              AiProactiveSubscriptionStatus status) {
        requireOne(subscriptionMapper.updateStatusIfCurrent(
                        subscription.getId(),
                        subscription.getStatus(),
                        status.name(),
                        currentUserProvider.getCurrentUserId()),
                "AI 主动提醒状态更新失败");
    }

    private AiProactiveEventResponse toEventResponse(TAiProactiveEvent event) {
        TAiProactiveSubscription subscription = subscriptionMapper.selectById(event.getSubscriptionId());
        return AiProactiveEventResponse.from(event,
                subscription == null ? null : subscription.getSubscriptionNo());
    }

    private void requireOne(int rows, String message) {
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, message);
        }
    }
}
