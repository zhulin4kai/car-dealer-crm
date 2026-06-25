package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TClueRemarkMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.query.ClueQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.ClueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClueServiceImplTest {

    @InjectMocks
    private ClueServiceImpl clueService;

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private TClueRemarkMapper tClueRemarkMapper;

    @Mock
    private TCustomerMapper tCustomerMapper;

    @Mock
    private DicMapper dicMapper;

    @Mock
    private TUserMapper tUserMapper;

    @Mock
    private TActivityMapper tActivityMapper;

    @Mock
    private TProductMapper tProductMapper;

    @Mock
    private ClueImportValidator clueImportValidator;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OperationAuditRecorder auditRecorder;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
    }

    // ==================== getClueById ====================

    @Test
    void getClueById_found_shouldReturnClue() {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setFullName("John Doe");
        clue.setPhone("13800138000");

        when(tClueMapper.selectDetailById(1, null)).thenReturn(clue);

        TClue result = clueService.getClueById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("John Doe", result.getFullName());
        assertEquals("13800138000", result.getPhone());
    }

    @Test
    void getClueById_notFound_shouldReturnNull() {
        when(tClueMapper.selectDetailById(999, null)).thenReturn(null);

        TClue result = clueService.getClueById(999);

        assertNull(result);
    }

    // ==================== saveClue ====================

    @Test
    void saveClue_phoneAlreadyExists_shouldThrowException() {
        ClueQuery query = new ClueQuery();
        query.setPhone("13800138000");

        when(tClueMapper.selectByCount("13800138000")).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.saveClue(query));

        assertEquals(CodeEnum.DUPLICATE, exception.getCodeEnum());
        assertEquals("该手机号已经录入过了，不能再录入", exception.getMessage());
        verify(tClueMapper, never()).insertSelective(any());
    }

    @Test
    void saveClue_concurrentDuplicatePhone_shouldReturnDuplicateCode() {
        ClueQuery query = new ClueQuery();
        query.setPhone("13800138000");
        query.setFullName("John Doe");

        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(tClueMapper.insertSelective(any(TClue.class)))
                .thenThrow(new DuplicateKeyException("uk_clue_phone"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.saveClue(query));

        assertEquals(CodeEnum.DUPLICATE, exception.getCodeEnum());
        assertEquals("该手机号已经录入过了，不能再录入", exception.getMessage());
        verify(auditRecorder, never()).record(any(), any());
    }

    @Test
    void saveClue_newPhone_shouldInsertAndReturnCount() {
        ClueQuery query = new ClueQuery();
        query.setPhone("13800138000");
        query.setFullName("John Doe");

        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);
        when(tClueMapper.insertSelective(any(TClue.class))).thenReturn(1);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);

            int result = clueService.saveClue(query);

            assertEquals(1, result);
            verify(tClueMapper).insertSelective(argThat(clue -> {
                TClue c = (TClue) clue;
                return "John Doe".equals(c.getFullName())
                        && "13800138000".equals(c.getPhone())
                        && c.getCreateTime() != null
                        && c.getCreateBy().equals(1);
            }));
    }

    // ==================== updateClue ====================

    @Test
    void updateClue_shouldUpdateAndReturnCount() {
        ClueQuery query = new ClueQuery();
        query.setId(1);
        query.setFullName("Updated Name");
        query.setPhone("13800138000");

        TClue existingClue = new TClue();
        existingClue.setId(1);
        existingClue.setPhone("13800138000");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(existingClue);
        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(1);
        when(currentUserProvider.getCurrentUserId()).thenReturn(2);

            int result = clueService.updateClue(query);

            assertEquals(1, result);
            verify(tClueMapper).updateByPrimaryKeySelective(argThat(clue -> {
                TClue c = (TClue) clue;
                return c.getId().equals(1)
                        && "Updated Name".equals(c.getFullName())
                        && c.getEditTime() != null
                        && c.getEditBy().equals(2);
            }));
    }

    @Test
    void updateClue_notFound_shouldReturnZero() {
        ClueQuery query = new ClueQuery();
        query.setId(999);

        assertThrows(RuntimeException.class, () -> clueService.updateClue(query));
    }

    // ==================== delClueById ====================

    @Test
    void delClueById_success_shouldReturnOne() {
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue(1));
        when(tCustomerMapper.countByClueId(1)).thenReturn(0);
        when(tClueRemarkMapper.deleteByClueId(1)).thenReturn(1);
        when(tClueMapper.deleteByPrimaryKey(1)).thenReturn(1);

        int result = clueService.delClueById(1);

        assertEquals(1, result);
        verify(tClueRemarkMapper).deleteByClueId(1);
        verify(tClueMapper).deleteByPrimaryKey(1);
    }

    @Test
    void delClueById_nullId_shouldReturnZero() {
        int result = clueService.delClueById(null);

        assertEquals(0, result);
        verify(tClueRemarkMapper, never()).deleteByClueId(anyInt());
        verify(tClueMapper, never()).deleteByPrimaryKey(any());
    }

    @Test
    void delClueById_notFound_shouldThrow() {
        assertThrows(RuntimeException.class, () -> clueService.delClueById(999));
        verify(tClueRemarkMapper, never()).deleteByClueId(999);
        verify(tClueMapper, never()).deleteByPrimaryKey(999);
    }

    @Test
    void delClueById_convertedClue_shouldRejectWithoutDeletingHistory() {
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue(1));
        when(tCustomerMapper.countByClueId(1)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.delClueById(1));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(tClueRemarkMapper, never()).deleteByClueId(anyInt());
        verify(tClueMapper, never()).deleteByPrimaryKey(anyInt());
    }

    // ==================== batchDelClueByIds ====================

    @Test
    void batchDelClueByIds_success_shouldReturnDeletedCount() {
        List<Integer> ids = Arrays.asList(1, 2, 3);

        when(tClueMapper.selectScopedByPrimaryKey(anyInt(), isNull()))
                .thenAnswer(invocation -> clue(invocation.getArgument(0)));
        when(tCustomerMapper.countByClueId(anyInt())).thenReturn(0);
        when(tClueRemarkMapper.deleteByClueId(anyInt())).thenReturn(1);
        when(tClueMapper.batchDeleteByIds(ids)).thenReturn(3);

        int result = clueService.batchDelClueByIds(ids);

        assertEquals(3, result);
        verify(tClueMapper).batchDeleteByIds(ids);
    }

    @Test
    void batchDelClueByIds_convertedClue_shouldRejectWholeBatchWithoutDeletingHistory() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(tClueMapper.selectScopedByPrimaryKey(anyInt(), isNull()))
                .thenAnswer(invocation -> clue(invocation.getArgument(0)));
        when(tCustomerMapper.countByClueId(1)).thenReturn(0);
        when(tCustomerMapper.countByClueId(2)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.batchDelClueByIds(ids));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(tClueRemarkMapper, never()).deleteByClueId(anyInt());
        verify(tClueMapper, never()).batchDeleteByIds(anyList());
    }

    @Test
    void batchDelClueByIds_emptyList_shouldReturnZero() {
        int result = clueService.batchDelClueByIds(Collections.emptyList());

        assertEquals(0, result);
        verify(tClueMapper, never()).batchDeleteByIds(any());
    }

    @Test
    void batchDelClueByIds_nullList_shouldReturnZero() {
        int result = clueService.batchDelClueByIds(null);

        assertEquals(0, result);
        verify(tClueMapper, never()).batchDeleteByIds(any());
    }

    // ==================== checkPhone ====================

    @Test
    void checkPhone_phoneNotExists_shouldReturnTrue() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);

        Boolean result = clueService.checkPhone("13800138000");

        assertTrue(result);
        verify(tClueMapper).selectByCount("13800138000");
    }

    @Test
    void checkPhone_phoneExists_shouldReturnFalse() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(1);

        Boolean result = clueService.checkPhone("13800138000");

        assertFalse(result);
    }

    @Test
    void checkPhone_multipleRecords_shouldReturnFalse() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(3);

        Boolean result = clueService.checkPhone("13800138000");

        assertFalse(result);
    }

    // ==================== importExcel ====================
    // importExcel 的完整测试由 ClueImportValidatorTest 和集成测试覆盖，
    // 因为 Service 层依赖 EasyExcel 的 InputStream 解析，
    // 在纯单元测试中难以 mock 静态方法。

    private TClue clue(Integer id) {
        TClue clue = new TClue();
        clue.setId(id);
        clue.setOwnerId(1);
        return clue;
    }
}
