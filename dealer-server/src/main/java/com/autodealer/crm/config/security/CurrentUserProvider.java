package com.autodealer.crm.config.security;

import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.TranQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CurrentUserProvider {
    private static final String ROLE_ADMIN = "admin";

    public TUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof TUser user)) {
            throw new IllegalStateException("当前用户不存在");
        }
        return user;
    }

    public Integer getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof SessionAuthenticationDetails details) {
            return details.sessionId();
        }
        throw new IllegalStateException("当前会话不存在");
    }

    public boolean isAdmin() {
        return hasRole(getCurrentUser(), ROLE_ADMIN);
    }

    public boolean hasAuthority(String authority) {
        return getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    public Integer getDataScopeUserId() {
        if (isAdmin()) {
            return null;
        }
        Integer userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("当前用户 ID 不能为空");
        }
        return userId;
    }

    public TransactionDataScope getTransactionDataScope() {
        TUser currentUser = getCurrentUser();
        Integer userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("当前用户 ID 不能为空");
        }
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return TransactionDataScope.all();
        }

        return TransactionDataScope.limited(userId, false, List.of());
    }

    public void applyTransactionDataScope(TranQuery query) {
        TransactionDataScope scope = getTransactionDataScope();
        query.setDataScopeUserId(scope.getSelfUserId());
        query.setTransactionAllScope(scope.isAll());
        query.setTransactionApprovalScope(scope.isApprovalScope());
        query.setTransactionFinanceStages(scope.getFinanceStages());
    }

    private boolean hasRole(TUser user, String role) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    public static final class TransactionDataScope {
        private final boolean all;
        private final Integer selfUserId;
        private final boolean approvalScope;
        private final List<TranStage> financeStages;

        private TransactionDataScope(boolean all, Integer selfUserId,
                                     boolean approvalScope, List<TranStage> financeStages) {
            this.all = all;
            this.selfUserId = selfUserId;
            this.approvalScope = approvalScope;
            this.financeStages = financeStages == null ? List.of() : List.copyOf(financeStages);
        }

        public static TransactionDataScope all() {
            return new TransactionDataScope(true, null, false, List.of());
        }

        public static TransactionDataScope limited(Integer selfUserId,
                                                   boolean approvalScope,
                                                   List<TranStage> financeStages) {
            return new TransactionDataScope(false, selfUserId, approvalScope, financeStages);
        }

        public boolean isAll() {
            return all;
        }

        public Integer getSelfUserId() {
            return selfUserId;
        }

        public boolean isApprovalScope() {
            return approvalScope;
        }

        public List<TranStage> getFinanceStages() {
            return financeStages;
        }
    }
}
