package com.autodealer.crm.config.security;

import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserManagementAccessGateTest {
    private final TUserMapper users = mock(TUserMapper.class);
    private final TOrganizationUnitMapper organizations = mock(TOrganizationUnitMapper.class);
    private final UserManagementAccessGate gate = new UserManagementAccessGate(users, organizations, true);

    @Test
    void initializationBlocksDailyBusinessButAllowsRecoveryBootstrap() {
        when(organizations.countInitializedRootOrganizations()).thenReturn(0);
        TUser recovery = user(true);
        assertTrue(gate.evaluate(recovery, request("POST", "/api/users")).allowed());
        assertTrue(gate.evaluate(recovery, request("GET", "/api/users/filter-options")).allowed());
        assertTrue(gate.evaluate(recovery, request("GET", "/api/login/info")).allowed());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,
                gate.evaluate(recovery, request("PUT", "/api/users/2/authorization/roles")).code());
        UserManagementAccessGate.Decision denied = gate.evaluate(recovery, request("GET", "/api/customers"));
        assertFalse(denied.allowed());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED, denied.code());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,
                gate.evaluate(user(false), request("GET", "/api/login/info")).code());
    }

    @Test
    void readySystemStillRejectsRecoveryAccountDailyBusiness() {
        when(organizations.countInitializedRootOrganizations()).thenReturn(1);when(users.countAdminUsers()).thenReturn(1);
        TUser recovery = user(true);
        assertTrue(gate.evaluate(recovery, request("GET", "/api/login/info")).allowed());
        assertEquals(CodeEnum.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN,
                gate.evaluate(recovery, request("GET", "/api/users")).code());
        assertEquals(CodeEnum.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN,
                gate.evaluate(recovery, request("GET", "/api/trans" )).code());
        assertTrue(gate.evaluate(user(false), request("GET", "/api/trans")).allowed());
    }

    @Test
    void fixedRecoveryIdentityRequiresAllProtectedAccountFacts() {
        TUser recovery = user(true);
        assertTrue(gate.isFixedRecoveryAccount(recovery));

        recovery.setLoginAct("renamed-admin");
        assertFalse(gate.isFixedRecoveryAccount(recovery));
        assertFalse(gate.isFixedRecoveryAccount(user(false)));
    }

    @Test
    void recoveryAccountCannotUseLifecycleHandoverEvenThoughItIsUnderUsersPath() {
        when(organizations.countInitializedRootOrganizations()).thenReturn(1);when(users.countAdminUsers()).thenReturn(1);
        assertEquals(CodeEnum.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN,
                gate.evaluate(user(true), request("POST", "/api/users/2/lifecycle/departure/complete")).code());
    }

    @Test
    void pendingFirstChangeOnlyAllowsOwnCompletionAndNarrowRecovery() {
        when(organizations.countInitializedRootOrganizations()).thenReturn(1);
        when(users.countAdminUsers()).thenReturn(0);when(users.countPendingAdminUsers()).thenReturn(1);
        assertEquals(UserManagementAccessGate.BootstrapState.PENDING_FIRST_CHANGE,gate.state());
        assertTrue(gate.evaluate(user(false),request("GET","/api/login/info")).allowed());
        assertTrue(gate.evaluate(user(false),request("PUT","/api/credentials/first-password-change")).allowed());
        assertTrue(gate.evaluate(user(false),request("GET","/api/profile")).allowed());
        assertTrue(gate.evaluate(user(false),request("PUT","/api/profile")).allowed());
        assertTrue(gate.evaluate(user(false),request("POST","/api/profile/contact-verification")).allowed());
        assertTrue(gate.evaluate(user(false),request("POST","/api/credentials/verify-contact")).allowed());
        assertTrue(gate.evaluate(user(false),request("POST","/api/logout")).allowed());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,gate.evaluate(user(false),request("GET","/api/customers")).code());
        assertTrue(gate.evaluate(user(true),request("POST","/api/recovery/admin-access")).allowed());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,gate.evaluate(user(true),request("POST","/api/users")).code());
    }

    @Test
    void degradedStateOnlyAllowsFixedRecoveryAdminAccessCommand() {
        when(organizations.countInitializedRootOrganizations()).thenReturn(1);
        when(users.countAdminUsers()).thenReturn(0);when(users.countPendingAdminUsers()).thenReturn(0);
        assertEquals(UserManagementAccessGate.BootstrapState.DEGRADED,gate.state());
        assertTrue(gate.evaluate(user(true),request("POST","/api/recovery/admin-access")).allowed());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,gate.evaluate(user(false),request("GET","/api/login/info")).code());
        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,gate.evaluate(user(true),request("POST","/api/users")).code());
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    private TUser user(boolean protectedAccount) {
        TUser user = new TUser();user.setId(protectedAccount ? 1 : 2);user.setProtectedAccount(protectedAccount);
        user.setLoginAct(protectedAccount ? "admin" : "ordinary");
        user.setAccountType(protectedAccount ? com.autodealer.crm.enums.AccountType.SYSTEM
                : com.autodealer.crm.enums.AccountType.HUMAN);
        return user;
    }
}
