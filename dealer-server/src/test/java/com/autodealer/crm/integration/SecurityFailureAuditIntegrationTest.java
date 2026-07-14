package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.SecurityFailureAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = DealerCRMApplication.class)
@ActiveProfiles("test")
class SecurityFailureAuditIntegrationTest {
    private static final String RESOURCE = "deadbeefcafebabe";
    @Autowired SecurityFailureAuditService failureAudit;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup(){jdbc.update("DELETE FROM t_operation_log WHERE resource_id=?",RESOURCE);}

    @Test
    void rejectedSecurityFactSurvivesCallerTransactionRollback(){
        MockHttpServletRequest request=new MockHttpServletRequest();request.setRemoteAddr("203.0.113.44");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {transactions.executeWithoutResult(status->{
                failureAudit.recordAnonymous(AuditActionEnum.USER_CREDENTIAL_ATTEMPT_REJECTED,RESOURCE,
                        "{\"reason\":\"EXPIRED\"}");
                status.setRollbackOnly();
            });
        } finally {RequestContextHolder.resetRequestAttributes();}

        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_operation_log WHERE resource_id=? AND action_code=?",
                Integer.class,RESOURCE,AuditActionEnum.USER_CREDENTIAL_ATTEMPT_REJECTED.getActionCode()));
        String detail=jdbc.queryForObject("SELECT detail FROM t_operation_log WHERE resource_id=?",String.class,RESOURCE);
        assertFalse(detail.contains("credential"));
        assertFalse(detail.contains("recoveryKey"));
        String source=jdbc.queryForObject("SELECT ip FROM t_operation_log WHERE resource_id=?",String.class,RESOURCE);
        assertFalse(source.contains("203.0.113.44"));
        assertEquals(37,source.length());
    }
}
