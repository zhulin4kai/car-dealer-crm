package com.autodealer.crm.audit;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import com.autodealer.crm.service.impl.CredentialDerivationCodec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityFailureAuditServiceTest {
    @Test
    void auditInfrastructureFailureNeverReplacesOriginalSecurityResponse(){
        OperationAuditRecorder recorder=mock(OperationAuditRecorder.class);
        PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenThrow(new CannotCreateTransactionException("audit unavailable"));
        SecurityFailureAuditService service=new SecurityFailureAuditService(recorder,transactions,
                new CredentialDerivationCodec("test-only-credential-derivation-key-00000001"));

        assertDoesNotThrow(()->service.recordAnonymous(AuditActionEnum.USER_CREDENTIAL_RATE_LIMIT,
                "deadbeefcafebabe","{\"scope\":\"RESET\"}"));
    }
}
