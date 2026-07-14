package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.OwnerCandidateCacheInvalidator;
import com.autodealer.crm.service.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSecurityMutationCoordinatorTest {
    @Mock private CurrentUserProvider currentUser;
    @Mock private UserSessionService sessions;
    @Mock private OwnerCandidateCacheInvalidator ownerCache;

    private UserSecurityMutationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new UserSecurityMutationCoordinator(currentUser, sessions, ownerCache);
    }

    @Test
    void accessChangeDeduplicatesTargetsAndInvalidatesOwnerCacheOnce() {
        when(currentUser.getCurrentUserId()).thenReturn(9);

        coordinator.accessChanged(List.of(2, 3, 2), "授权变化");

        verify(ownerCache).invalidateAfterCommit();
        verify(sessions).revokeAllForSecurityChange(2, 9, "授权变化");
        verify(sessions).revokeAllForSecurityChange(3, 9, "授权变化");
    }

    @Test
    void authenticationChangeKeepsOwnerCacheAndSupportsAnonymousOperator() {
        coordinator.authenticationChanged(2, null, "密码变化");

        verify(sessions).revokeAllForSecurityChange(2, null, "密码变化");
        verify(ownerCache, never()).invalidateAfterCommit();
        verify(currentUser, never()).getCurrentUserId();
    }

    @Test
    void everyMutationEntryRequiresAnExistingTransaction() {
        for (Method method : UserSecurityMutationCoordinator.class.getDeclaredMethods()) {
            if (!List.of("accessChanged", "authenticationChanged", "ownerEligibilityChanged")
                    .contains(method.getName())) continue;
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertNotNull(transactional, method.getName() + " 必须声明事务传播");
            assertEquals(Propagation.MANDATORY, transactional.propagation());
        }
    }
}
