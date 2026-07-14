package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.OwnerCandidateCacheInvalidator;
import com.autodealer.crm.service.UserSessionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Collections;

/**
 * 统一编排用户安全变化后的事务内会话撤销，并只在访问资格变化时失效负责人缓存。
 *
 * <p>安全版本仍由持有乐观锁和业务状态的应用服务更新；本组件不隐藏状态迁移、锁序或授权规则。
 * 密码、登录账号等纯认证变化不得调用 {@code accessChanged}，避免无关的候选缓存全量失效。</p>
 */
@Component
public class UserSecurityMutationCoordinator {
    private final CurrentUserProvider currentUserProvider;
    private final UserSessionService userSessionService;
    private final OwnerCandidateCacheInvalidator ownerCandidateCacheInvalidator;

    public UserSecurityMutationCoordinator(CurrentUserProvider currentUserProvider,
                                           UserSessionService userSessionService,
                                           OwnerCandidateCacheInvalidator ownerCandidateCacheInvalidator) {
        this.currentUserProvider = currentUserProvider;
        this.userSessionService = userSessionService;
        this.ownerCandidateCacheInvalidator = ownerCandidateCacheInvalidator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void accessChanged(Integer userId, String reason) {
        accessChanged(Collections.singleton(userId), reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void accessChanged(Integer userId, Integer operatorId, String reason) {
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        revoke(Collections.singleton(userId), operatorId, reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void accessChanged(Collection<Integer> userIds, String reason) {
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        revoke(userIds, currentUserProvider.getCurrentUserId(), reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void authenticationChanged(Integer userId, String reason) {
        revoke(Collections.singleton(userId), currentUserProvider.getCurrentUserId(), reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void authenticationChanged(Integer userId, Integer operatorId, String reason) {
        revoke(Collections.singleton(userId), operatorId, reason);
    }

    private void revoke(Collection<Integer> userIds, Integer operatorId, String reason) {
        LinkedHashSet<Integer> uniqueUserIds = new LinkedHashSet<>(userIds);
        uniqueUserIds.remove(null);
        if (uniqueUserIds.isEmpty()) return;
        uniqueUserIds.forEach(userId ->
                userSessionService.revokeAllForSecurityChange(userId, operatorId, reason));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void ownerEligibilityChanged() {
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
    }
}
