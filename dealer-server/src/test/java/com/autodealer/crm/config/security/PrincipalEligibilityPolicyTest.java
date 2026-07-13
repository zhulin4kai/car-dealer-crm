package com.autodealer.crm.config.security;

import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.model.TUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrincipalEligibilityPolicyTest {
    private final TEmployeeMapper employees = mock(TEmployeeMapper.class);
    private final UserManagementAccessGate accessGate = mock(UserManagementAccessGate.class);
    private final PrincipalEligibilityPolicy policy = new PrincipalEligibilityPolicy(employees, accessGate);

    @Test
    void fixedRecoveryAccountKeepsDedicatedGateSemanticsWithoutEmployeeFacts() {
        TUser recovery = user(1, AccountType.SYSTEM);
        recovery.setLoginAct("admin");
        recovery.setProtectedAccount(true);
        when(accessGate.isFixedRecoveryAccount(recovery)).thenReturn(true);

        assertTrue(policy.isEligible(recovery));
        verify(employees, never()).countLoginEligibleByUserId(any(), any());
    }

    @Test
    void nonRecoverySystemAccountAndQueryFailureFailClosed() {
        TUser system = user(8, AccountType.SYSTEM);
        assertFalse(policy.isEligible(system));
        verify(employees, never()).countLoginEligibleByUserId(any(), any());

        TUser human = user(9, AccountType.HUMAN);
        when(employees.countLoginEligibleByUserId(eq(9), any())).thenThrow(new IllegalStateException("database unavailable"));
        assertFalse(policy.isEligible(human));
    }

    private static TUser user(int id, AccountType accountType) {
        TUser user = new TUser();
        user.setId(id);
        user.setAccountType(accountType);
        return user;
    }
}
