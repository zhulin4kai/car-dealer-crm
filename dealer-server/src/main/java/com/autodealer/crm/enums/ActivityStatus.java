package com.autodealer.crm.enums;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;

import java.util.Arrays;
import java.util.Set;

public enum ActivityStatus {
    DRAFT,
    PLANNED,
    ONGOING,
    ENDED,
    REVIEWED,
    CLOSED,
    CANCELED;

    private static final Set<ActivityStatus> TERMINAL_STATUSES = Set.of(REVIEWED, CLOSED, CANCELED);

    public static ActivityStatus parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "活动状态不能为空");
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CodeEnum.PARAM_ERROR, "活动状态编码无效"));
    }

    public boolean terminal() {
        return TERMINAL_STATUSES.contains(this);
    }

    public boolean locksCoreFacts() {
        return this == ENDED || this == REVIEWED || this == CLOSED || this == CANCELED;
    }

    public boolean canPublish() {
        return this == DRAFT;
    }

    public boolean canStart() {
        return this == PLANNED;
    }

    public boolean canEnd() {
        return this == ONGOING;
    }

    public boolean canReview() {
        return this == ENDED;
    }

    public boolean canCancel() {
        return this == DRAFT || this == PLANNED || this == ONGOING;
    }

    public boolean canClose() {
        return this == ENDED || this == REVIEWED;
    }
}
