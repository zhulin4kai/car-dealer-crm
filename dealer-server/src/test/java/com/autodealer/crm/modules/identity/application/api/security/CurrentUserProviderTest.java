package com.autodealer.crm.modules.identity.application.api.security;

import com.autodealer.crm.modules.identity.application.api.model.TUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserProviderTest {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void salesManagerScopeShouldNotExpandToAllPendingTransactions() {
        loginAs(7, "sales_manager");

        CurrentUserProvider.TransactionDataScope scope = provider.getTransactionDataScope();

        assertFalse(scope.isAll());
        assertEquals(7, scope.getSelfUserId());
        assertFalse(scope.isApprovalScope());
        assertTrue(scope.getFinanceStages().isEmpty());
    }

    @Test
    void financeScopeShouldNotExpandToAllFinanceStageTransactions() {
        loginAs(8, "finance_specialist");

        CurrentUserProvider.TransactionDataScope scope = provider.getTransactionDataScope();

        assertFalse(scope.isAll());
        assertEquals(8, scope.getSelfUserId());
        assertFalse(scope.isApprovalScope());
        assertTrue(scope.getFinanceStages().isEmpty());
    }

    @Test
    void adminScopeShouldRemainGlobal() {
        loginAs(1, "admin");

        CurrentUserProvider.TransactionDataScope scope = provider.getTransactionDataScope();

        assertTrue(scope.isAll());
    }

    private void loginAs(Integer id, String role) {
        TUser user = new TUser();
        user.setId(id);
        user.setLoginAct("user" + id);
        user.setRoleList(List.of(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
