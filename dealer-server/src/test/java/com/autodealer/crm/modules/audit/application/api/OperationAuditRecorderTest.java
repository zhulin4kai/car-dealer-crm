package com.autodealer.crm.modules.audit.application.api;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.audit.persistence.mapper.TOperationLogMapper;
import com.autodealer.crm.modules.audit.persistence.model.TOperationLog;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationAuditRecorder 单元测试。
 *
 * <p>CurrentUserProvider 通过测试桩实现注入，避免 Mockito inline mock
 * 在 Java 23 上的兼容性问题。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationAuditRecorder 单元测试")
class OperationAuditRecorderTest {

    private OperationAuditRecorder recorder;

    @Mock
    private TOperationLogMapper tOperationLogMapper;

    private CurrentUserProvider testCurrentUserProvider;

    @BeforeEach
    void setUp() {
        TUser mockUser = new TUser();
        mockUser.setId(1);
        mockUser.setName("管理员");

        testCurrentUserProvider = new CurrentUserProvider() {
            @Override
            public TUser getCurrentUser() {
                return mockUser;
            }

            @Override
            public Integer getCurrentUserId() {
                return 1;
            }

            @Override
            public boolean isAdmin() {
                return true;
            }

            @Override
            public Integer getDataScopeUserId() {
                return null;
            }
        };

        recorder = new OperationAuditRecorder(testCurrentUserProvider, tOperationLogMapper);
    }

    @Test
    @DisplayName("record 方法应正确写入操作者、动作、资源、时间和结果")
    void record_shouldWriteOperatorActionResourceTimeAndResult() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.USER_CREATE, "100", "SUCCESS",
                "{\"loginAct\":\"testuser\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        TOperationLog log = captor.getValue();

        assertEquals(1, log.getUserId());
        assertEquals("管理员", log.getUserName());
        assertEquals("USER_CREATE", log.getActionCode());
        assertEquals("用户管理", log.getModuleName());
        assertEquals("USER", log.getObjectType());
        assertEquals("100", log.getResourceId());
        assertEquals("SUCCESS", log.getResult());
        assertNotNull(log.getDetail());
        assertTrue(log.getDetail().contains("SUCCESS"));
        assertTrue(log.getDetail().contains("{\"loginAct\":\"testuser\"}"));
        assertNotNull(log.getCreateTime());
        assertNotNull(log.getIp());
        assertNotNull(log.getRequestId());
    }

    @Test
    @DisplayName("匿名凭证审计不应读取当前登录用户")
    void recordAnonymous_shouldNotDependOnCurrentUser() {
        CurrentUserProvider unavailableCurrentUser = mock(CurrentUserProvider.class);
        OperationAuditRecorder anonymousRecorder =
                new OperationAuditRecorder(unavailableCurrentUser, tOperationLogMapper);
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        anonymousRecorder.recordAnonymous(AuditActionEnum.USER_CREDENTIAL_CONSUME,
                "100", "SUCCESS", "{\"purpose\":\"SELF_RESET\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        assertNull(captor.getValue().getUserId());
        assertEquals("ANONYMOUS_CREDENTIAL_FLOW", captor.getValue().getUserName());
        verifyNoInteractions(unavailableCurrentUser);
    }

    @Test
    @DisplayName("record 无摘要时应只包含 result")
    void record_withoutSummary_shouldContainOnlyResult() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.CLUE_IMPORT, "200");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        TOperationLog log = captor.getValue();

        assertEquals("CLUE_IMPORT", log.getActionCode());
        assertEquals("线索管理", log.getModuleName());
        assertEquals("CLUE", log.getObjectType());
        assertEquals("200", log.getResourceId());
        assertEquals("SUCCESS", log.getResult());
        assertTrue(log.getDetail().contains("SUCCESS"));
    }

    @Test
    @DisplayName("record 插入影响行数不为 1 时应抛出异常")
    void record_whenInsertReturnsZero_shouldThrowException() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                recorder.record(AuditActionEnum.TRAN_CREATE, "300"));
        assertTrue(ex.getMessage().contains("影响行数"));
        assertTrue(ex.getMessage().contains("TRAN_CREATE"));
        assertTrue(ex.getMessage().contains("300"));
    }

    @Test
    @DisplayName("recordQuietly 插入失败时应仅记录日志不抛出异常")
    void recordQuietly_whenInsertFails_shouldNotThrow() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(0);

        assertDoesNotThrow(() ->
                recorder.recordQuietly(AuditActionEnum.PRODUCT_STOCK_IN, "400", "SUCCESS", null));
        verify(tOperationLogMapper).insert(any(TOperationLog.class));
    }

    @Test
    @DisplayName("审计摘要不应包含密码")
    void detail_shouldNotContainPassword() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.USER_CREATE, "100", "SUCCESS",
                "{\"loginAct\":\"testuser\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        String detail = captor.getValue().getDetail();

        assertFalse(detail.contains("password"));
        assertFalse(detail.contains("loginPwd"));
        assertFalse(detail.contains("$2a$"));
    }

    @Test
    @DisplayName("审计记录器必须主动清理密码哈希令牌和完整联系方式")
    void detail_shouldSanitizeSensitiveValuesEvenWhenCallerPassesThem() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);
        String hash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";

        recorder.record(AuditActionEnum.USER_PASSWORD_CHANGE, "100", "SUCCESS",
                "{\"loginPwd\":\"plain-secret\",\"hash\":\"" + hash
                        + "\",\"authorization\":\"Bearer " + jwt
                        + "\",\"phone\":\"13812345678\",\"email\":\"admin@example.com\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        String detail = captor.getValue().getDetail();
        assertFalse(detail.contains("plain-secret"));
        assertFalse(detail.contains(hash));
        assertFalse(detail.contains(jwt));
        assertFalse(detail.contains("13812345678"));
        assertFalse(detail.contains("admin@example.com"));
        assertTrue(detail.length() <= 2048);
    }

    @Test
    @DisplayName("扩容后的审计明细仍必须限制为 2048 字符")
    void detail_shouldUseExpandedButBoundedCapacity() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);
        recorder.record(AuditActionEnum.ROLE_MATRIX_CHANGE, "1", "SUCCESS",
                "{\"changes\":\"" + "x".repeat(3000) + "\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        String detail = captor.getValue().getDetail();
        assertTrue(detail.length() <= 2048);
        assertDoesNotThrow(() -> new ObjectMapper().readTree(detail));
        JsonNode parsed = assertDoesNotThrow(() -> new ObjectMapper().readTree(detail));
        assertEquals("[TRUNCATED]", parsed.path("summary").asText());
    }

    @Test
    @DisplayName("审计摘要应清理常见集成凭据字段并保持合法 JSON")
    void detail_shouldSanitizeIntegrationCredentialsAndRemainValidJson() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);
        recorder.record(AuditActionEnum.USER_PASSWORD_CHANGE, "100", "SUCCESS", """
                {"accessToken":"a","refreshToken":"b","clientSecret":"c",
                 "apiKey":"d","credentialHash":"e","safeField":"ok"}
                """);

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        String detail = captor.getValue().getDetail();
        assertFalse(detail.contains("\"a\"") || detail.contains("\"b\"")
                || detail.contains("\"c\"") || detail.contains("\"d\"") || detail.contains("\"e\""));
        assertTrue(detail.contains("ok"));
        assertDoesNotThrow(() -> new ObjectMapper().readTree(detail));
    }

    @Test
    @DisplayName("审计摘要不应包含 JWT 和 Authorization")
    void detail_shouldNotContainJwtOrAuthorization() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.DICT_TYPE_SAVE, "1", "SUCCESS",
                "{\"dictType\":\"customer_level\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        String detail = captor.getValue().getDetail();

        assertFalse(detail.contains("Bearer "));
        assertFalse(detail.contains("Authorization"));
        assertFalse(detail.contains("JWT"));
        assertFalse(detail.contains("eyJ"));
    }

    @Test
    @DisplayName("审计摘要不应包含手机号全值")
    void detail_shouldNotContainFullPhoneNumber() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.CUSTOMER_CONVERT, "500", "SUCCESS",
                "{\"customerName\":\"测试客户\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());
        String detail = captor.getValue().getDetail();

        assertFalse(detail.matches(".*\\b1[3-9]\\d{9}\\b.*"));
    }

    @Test
    @DisplayName("多动作枚举应正确映射 actionCode 和 moduleName")
    void multipleActions_shouldMapCorrectly() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.PAYMENT_REFUND, "600", "SUCCESS", null);
        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());

        assertEquals("PAYMENT_REFUND", captor.getValue().getActionCode());
        assertEquals("退款管理", captor.getValue().getModuleName());
    }

    @Test
    @DisplayName("FAILURE 结果应正确记录")
    void failureResult_shouldBeRecorded() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.TRAN_APPROVE, "700", "FAILURE",
                "{\"reason\":\"审批不通过\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());

        assertEquals("FAILURE", captor.getValue().getResult());
        assertTrue(captor.getValue().getDetail().contains("FAILURE"));
        assertTrue(captor.getValue().getDetail().contains("审批不通过"));
    }

    @Test
    @DisplayName("fromActionCode 应能正确解析已知 code")
    void fromActionCode_shouldResolveKnownCode() {
        AuditActionEnum action = AuditActionEnum.fromActionCode("USER_CREATE");
        assertEquals(AuditActionEnum.USER_CREATE, action);
    }

    @Test
    @DisplayName("fromActionCode 对未知 code 应抛出异常")
    void fromActionCode_unknownCode_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                AuditActionEnum.fromActionCode("NONEXISTENT"));
    }

    @Test
    @DisplayName("actionCode 不应与 ordinal 关联")
    void actionCode_shouldNotDependOnOrdinal() {
        assertEquals("USER_CREATE", AuditActionEnum.USER_CREATE.getActionCode());
        assertEquals("TRAN_DELETE", AuditActionEnum.TRAN_DELETE.getActionCode());
        assertEquals("EXPORT_ALL_CUSTOMER", AuditActionEnum.EXPORT_ALL_CUSTOMER.getActionCode());
    }

    @Test
    @DisplayName("所有审计动作枚举应覆盖指定模块")
    void auditActions_shouldCoverRequiredModules() {
        assertNotNull(AuditActionEnum.USER_CREATE);
        assertNotNull(AuditActionEnum.USER_STATUS_CHANGE);
        assertNotNull(AuditActionEnum.CLUE_IMPORT);
        assertNotNull(AuditActionEnum.CLUE_TRANSFORM);
        assertNotNull(AuditActionEnum.CUSTOMER_OWNER_CHANGE);
        assertNotNull(AuditActionEnum.TRAN_SETTLE);
        assertNotNull(AuditActionEnum.TRAN_RESUBMIT);
        assertNotNull(AuditActionEnum.PAYMENT_CREATE);
        assertNotNull(AuditActionEnum.INVOICE_CREATE);
        assertNotNull(AuditActionEnum.PRODUCT_STOCK_IN);
        assertNotNull(AuditActionEnum.PRODUCT_STOCK_ADJUST);
        assertNotNull(AuditActionEnum.PRODUCT_STATUS_CHANGE);
        assertNotNull(AuditActionEnum.DICT_TYPE_SAVE);
        assertNotNull(AuditActionEnum.DICT_VALUE_DELETE);
        assertNotNull(AuditActionEnum.EXPORT_ALL_CUSTOMER);
    }
}
