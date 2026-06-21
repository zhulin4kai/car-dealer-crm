package com.autodealer.crm.audit;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.mapper.TOperationLogMapper;
import com.autodealer.crm.model.TOperationLog;
import com.autodealer.crm.model.TUser;
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
        assertEquals("100", log.getResourceId());
        assertNotNull(log.getDetail());
        assertTrue(log.getDetail().contains("SUCCESS"));
        assertTrue(log.getDetail().contains("{\"loginAct\":\"testuser\"}"));
        assertNotNull(log.getCreateTime());
        assertNotNull(log.getIp());
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
        assertEquals("200", log.getResourceId());
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
    @DisplayName("审计摘要不应包含 JWT 和 Authorization")
    void detail_shouldNotContainJwtOrAuthorization() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.SYSTEM_CONFIG_UPDATE, "1", "SUCCESS",
                "{\"configKey\":\"site.title\"}");

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
        assertEquals("支付管理", captor.getValue().getModuleName());
    }

    @Test
    @DisplayName("FAILURE 结果应正确记录")
    void failureResult_shouldBeRecorded() {
        when(tOperationLogMapper.insert(any(TOperationLog.class))).thenReturn(1);

        recorder.record(AuditActionEnum.TRAN_APPROVE, "700", "FAILURE",
                "{\"reason\":\"审批不通过\"}");

        ArgumentCaptor<TOperationLog> captor = ArgumentCaptor.forClass(TOperationLog.class);
        verify(tOperationLogMapper).insert(captor.capture());

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
        assertNotNull(AuditActionEnum.SYSTEM_CONFIG_UPDATE);
        assertNotNull(AuditActionEnum.EXPORT_ALL_CUSTOMER);
    }
}
