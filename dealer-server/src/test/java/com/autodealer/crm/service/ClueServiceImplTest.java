package com.autodealer.crm.service;

import com.alibaba.excel.EasyExcel;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.dto.ClueLifecycleRequest;
import com.autodealer.crm.dto.ImportResult;
import com.autodealer.crm.dto.ImportRowError;
import com.autodealer.crm.dto.TransferClueOwnerRequest;
import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TClueOwnerHistoryMapper;
import com.autodealer.crm.mapper.TClueRemarkMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.model.TClueOwnerHistory;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.query.ClueQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.ClueExcelRaw;
import com.autodealer.crm.service.ClueImportValidator.ValidatedClueImport;
import com.autodealer.crm.service.impl.ClueServiceImpl;
import com.autodealer.crm.service.impl.EmploymentResponsibilityGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    @Mock private EmploymentResponsibilityGuard responsibilityGuard;

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private TClueOwnerHistoryMapper clueOwnerHistoryMapper;

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

    @Test
    void getClueByPage_oversizedPageSize_shouldReject() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.getClueByPage(1, 101));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tClueMapper, never()).selectClueByPage(any());
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

    @Test void handoverOperatorCannotCreateClue(){
        ClueQuery query=new ClueQuery();query.setPhone("13800138000");when(tClueMapper.selectByCount("13800138000")).thenReturn(0);
        when(currentUserProvider.getCurrentUserId()).thenReturn(2);doThrow(new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT)).when(responsibilityGuard).requireActiveOwner(2);
        BusinessException error=assertThrows(BusinessException.class,()->clueService.saveClue(query));
        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,error.getCodeEnum());verify(tClueMapper,never()).insertSelective(any());verifyNoInteractions(auditRecorder);
    }

    @Test void handoverOperatorCannotImportClues(){
        when(currentUserProvider.getCurrentUserId()).thenReturn(2);doThrow(new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT)).when(responsibilityGuard).requireActiveOwner(2);
        BusinessException error=assertThrows(BusinessException.class,()->clueService.importExcel(new ByteArrayInputStream(new byte[0])));
        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,error.getCodeEnum());verify(tClueMapper,never()).saveClue(anyList());verifyNoInteractions(auditRecorder);
    }

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

    @Test
    void saveClue_phoneWithSeparators_shouldNormalizeBeforeCheckAndInsert() {
        ClueQuery query = new ClueQuery();
        query.setPhone("138 0013-8000");
        query.setFullName("John Doe");

        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);
        when(tClueMapper.insertSelective(any(TClue.class))).thenReturn(1);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);

        int result = clueService.saveClue(query);

        assertEquals(1, result);
        verify(tClueMapper).selectByCount("13800138000");
        verify(tClueMapper).insertSelective(argThat(clue -> {
            TClue c = (TClue) clue;
            return "13800138000".equals(c.getPhone());
        }));
    }

    @Test
    void saveClue_invalidPhone_shouldThrowParamErrorWithoutInsert() {
        ClueQuery query = new ClueQuery();
        query.setPhone("12345");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.saveClue(query));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tClueMapper, never()).selectByCount(anyString());
        verify(tClueMapper, never()).insertSelective(any());
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
        existingClue.setOwnerId(7);
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
                        && c.getOwnerId().equals(7)
                        && c.getEditTime() != null
                        && c.getEditBy().equals(2);
            }));
    }

    @Test
    void updateClue_scopeChangedBeforeUpdate_shouldThrowConflict() {
        ClueQuery query = new ClueQuery();
        query.setId(1);
        query.setFullName("Updated Name");
        query.setPhone("13800138000");

        TClue existingClue = new TClue();
        existingClue.setId(1);
        existingClue.setOwnerId(7);
        existingClue.setPhone("13800138000");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(existingClue);
        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(0);
        when(currentUserProvider.getCurrentUserId()).thenReturn(2);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.updateClue(query));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
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
        when(tClueMapper.deleteScopedByPrimaryKey(1, null)).thenReturn(1);

        int result = clueService.delClueById(1);

        assertEquals(1, result);
        verify(tClueRemarkMapper).deleteByClueId(1);
        verify(tClueMapper).deleteScopedByPrimaryKey(1, null);
    }

    @Test
    void delClueById_nullId_shouldReturnZero() {
        int result = clueService.delClueById(null);

        assertEquals(0, result);
        verify(tClueRemarkMapper, never()).deleteByClueId(anyInt());
        verify(tClueMapper, never()).deleteScopedByPrimaryKey(anyInt(), nullable(Integer.class));
    }

    @Test
    void delClueById_notFound_shouldThrow() {
        assertThrows(RuntimeException.class, () -> clueService.delClueById(999));
        verify(tClueRemarkMapper, never()).deleteByClueId(999);
        verify(tClueMapper, never()).deleteScopedByPrimaryKey(eq(999), nullable(Integer.class));
    }

    @Test
    void delClueById_convertedClue_shouldRejectWithoutDeletingHistory() {
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue(1));
        when(tCustomerMapper.countByClueId(1)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.delClueById(1));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(tClueRemarkMapper, never()).deleteByClueId(anyInt());
        verify(tClueMapper, never()).deleteScopedByPrimaryKey(anyInt(), nullable(Integer.class));
    }

    @Test
    void delClueById_scopeChangedBeforeDelete_shouldThrowConflict() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tClueMapper.selectScopedByPrimaryKey(1, 7)).thenReturn(clue(1));
        when(tCustomerMapper.countByClueId(1)).thenReturn(0);
        when(tClueRemarkMapper.deleteByClueId(1)).thenReturn(1);
        when(tClueMapper.deleteScopedByPrimaryKey(1, 7)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.delClueById(1));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(tClueMapper).deleteScopedByPrimaryKey(1, 7);
    }

    // ==================== batchDelClueByIds ====================

    @Test
    void batchDelClueByIds_success_shouldReturnDeletedCount() {
        List<Integer> ids = Arrays.asList(1, 2, 3);

        when(tClueMapper.selectScopedByPrimaryKey(anyInt(), isNull()))
                .thenAnswer(invocation -> clue(invocation.getArgument(0)));
        when(tCustomerMapper.countByClueId(anyInt())).thenReturn(0);
        when(tClueRemarkMapper.deleteByClueId(anyInt())).thenReturn(1);
        when(tClueMapper.batchDeleteScopedByIds(ids, null)).thenReturn(3);

        int result = clueService.batchDelClueByIds(ids);

        assertEquals(3, result);
        verify(tClueMapper).batchDeleteScopedByIds(ids, null);
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
        verify(tClueMapper, never()).batchDeleteScopedByIds(anyList(), nullable(Integer.class));
    }

    @Test
    void batchDelClueByIds_scopeChangedBeforeDelete_shouldThrowConflict() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tClueMapper.selectScopedByPrimaryKey(anyInt(), eq(7)))
                .thenAnswer(invocation -> clue(invocation.getArgument(0)));
        when(tCustomerMapper.countByClueId(anyInt())).thenReturn(0);
        when(tClueRemarkMapper.deleteByClueId(anyInt())).thenReturn(1);
        when(tClueMapper.batchDeleteScopedByIds(ids, 7)).thenReturn(2);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.batchDelClueByIds(ids));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(tClueMapper).batchDeleteScopedByIds(ids, 7);
    }

    @Test
    void batchDelClueByIds_emptyList_shouldReturnZero() {
        int result = clueService.batchDelClueByIds(Collections.emptyList());

        assertEquals(0, result);
        verify(tClueMapper, never()).batchDeleteScopedByIds(anyList(), nullable(Integer.class));
    }

    @Test
    void batchDelClueByIds_nullList_shouldReturnZero() {
        int result = clueService.batchDelClueByIds(null);

        assertEquals(0, result);
        verify(tClueMapper, never()).batchDeleteScopedByIds(anyList(), nullable(Integer.class));
    }

    // ==================== transferOwner ====================

    @Test
    void transferOwner_success_shouldUpdateOwnerAndWriteHistory() {
        TClue clue = clue(1);
        clue.setOwnerId(10);
        TransferClueOwnerRequest request = new TransferClueOwnerRequest();
        request.setNewOwnerId(20);
        request.setReason("销售休假转派");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(tUserMapper.selectByOwner()).thenReturn(List.of(owner(20)));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tClueMapper.updateOwnerAtomic(1, 10, 20, 99, null)).thenReturn(1);
        when(clueOwnerHistoryMapper.insert(any(TClueOwnerHistory.class))).thenReturn(1);

        boolean result = clueService.transferOwner(1, request);

        assertTrue(result);
        verify(tClueMapper).updateOwnerAtomic(1, 10, 20, 99, null);
        verify(clueOwnerHistoryMapper).insert(argThat(history ->
                history.getClueId().equals(1)
                        && history.getFromOwnerId().equals(10)
                        && history.getToOwnerId().equals(20)
                        && history.getAssignedBy().equals(99)
                        && "销售休假转派".equals(history.getReason())
                        && history.getAssignedTime() != null));
    }

    @Test
    void transferOwner_invalidTargetOwner_shouldRejectWithoutUpdate() {
        TransferClueOwnerRequest request = new TransferClueOwnerRequest();
        request.setNewOwnerId(20);
        request.setReason("转派");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue(1));
        when(tUserMapper.selectByOwner()).thenReturn(List.of(owner(30)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.transferOwner(1, request));

        assertEquals(CodeEnum.NOT_FOUND, exception.getCodeEnum());
        verify(tClueMapper, never()).updateOwnerAtomic(any(), any(), any(), any(), any());
        verify(clueOwnerHistoryMapper, never()).insert(any());
    }

    @Test
    void transferOwner_ownerChangedConcurrently_shouldRejectWithoutHistory() {
        TClue clue = clue(1);
        clue.setOwnerId(10);
        TransferClueOwnerRequest request = new TransferClueOwnerRequest();
        request.setNewOwnerId(20);
        request.setReason("转派");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(tUserMapper.selectByOwner()).thenReturn(List.of(owner(20)));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tClueMapper.updateOwnerAtomic(1, 10, 20, 99, null)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.transferOwner(1, request));

        assertEquals(CodeEnum.OPERATION_FAILED, exception.getCodeEnum());
        verify(clueOwnerHistoryMapper, never()).insert(any());
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
    void checkPhone_phoneWithSeparators_shouldNormalizeBeforeLookup() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);

        Boolean result = clueService.checkPhone("138 0013-8000");

        assertTrue(result);
        verify(tClueMapper).selectByCount("13800138000");
    }

    @Test
    void checkPhone_multipleRecords_shouldReturnFalse() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(3);

        Boolean result = clueService.checkPhone("13800138000");

        assertFalse(result);
    }

    // ==================== importExcel ====================

    @Test
    void importExcel_partialValidationFailure_shouldInsertValidRowsAndKeepErrors() throws Exception {
        TClue validClue = clue(1);
        validClue.setPhone("13800138000");
        ImportResult importResult = new ImportResult(2, 1, 1, 0);
        importResult.addError(new ImportRowError(2, "手机号", "手机号格式不正确"));

        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(clueImportValidator.validateAndTransform(anyList(), any(), eq(99)))
                .thenReturn(new ValidatedClueImport(importResult, List.of(validClue)));
        when(tClueMapper.selectExistingPhones(List.of("13800138000"))).thenReturn(Collections.emptyList());
        when(tClueMapper.saveClue(List.of(validClue))).thenReturn(1);

        ImportResult result = clueService.importExcel(emptyExcelInput());

        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getFailedRows());
        assertEquals(1, result.getImportedCount());
        verify(tClueMapper).saveClue(List.of(validClue));
        verify(clueOwnerHistoryMapper).insert(any(TClueOwnerHistory.class));
    }

    @Test
    void importExcel_databaseDuplicate_shouldInsertOtherRowsAndReportDuplicateError() throws Exception {
        TClue duplicateClue = clue(1);
        duplicateClue.setPhone("13800138000");
        TClue insertableClue = clue(2);
        insertableClue.setPhone("13900139000");
        ImportResult importResult = new ImportResult(2, 2, 0, 0);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(clueImportValidator.validateAndTransform(anyList(), any(), eq(99)))
                .thenReturn(new ValidatedClueImport(importResult, List.of(duplicateClue, insertableClue)));
        when(tClueMapper.selectExistingPhones(List.of("13800138000", "13900139000")))
                .thenReturn(List.of("13800138000"));
        when(tClueMapper.saveClue(List.of(insertableClue))).thenReturn(1);

        ImportResult result = clueService.importExcel(emptyExcelInput());

        assertEquals(1, result.getValidRows());
        assertEquals(1, result.getFailedRows());
        assertEquals(1, result.getImportedCount());
        assertEquals("该手机号在数据库中已存在", result.getErrors().get(0).getReason());
        verify(tClueMapper).saveClue(List.of(insertableClue));
    }

    // ==================== close/restore ====================

    @Test
    void closeClue_success_shouldUpdateStateAndWriteAuditReason() {
        TClue clue = clue(1);
        clue.setState(6);
        ClueLifecycleRequest request = lifecycleRequest("客户明确拒绝");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(dicMapper.selectDicValues(any())).thenReturn(clueStates());
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tClueMapper.updateStateAtomic(1, 6, 68, 99, null)).thenReturn(1);

        boolean result = clueService.closeClue(1, request);

        assertTrue(result);
        verify(tClueMapper).updateStateAtomic(1, 6, 68, 99, null);
        verify(auditRecorder).record(
                eq(AuditActionEnum.CLUE_CLOSE),
                eq("1"),
                eq("SUCCESS"),
                eq("{\"reason\":\"客户明确拒绝\"}"));
    }

    @Test
    void closeClue_blankReason_shouldRejectWithoutUpdate() {
        ClueLifecycleRequest request = lifecycleRequest(" ");
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue(1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.closeClue(1, request));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tClueMapper, never()).updateStateAtomic(any(), any(), any(), any(), any());
        verify(auditRecorder, never()).record(any(), any(), any(), any());
    }

    @Test
    void closeClue_convertedClue_shouldRejectWithoutUpdate() {
        TClue clue = clue(1);
        clue.setState(9);
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(dicMapper.selectDicValues(any())).thenReturn(clueStates());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.closeClue(1, lifecycleRequest("重复线索")));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(tClueMapper, never()).updateStateAtomic(any(), any(), any(), any(), any());
    }

    @Test
    void restoreClue_success_shouldCheckActiveDuplicateAndWriteAuditReason() {
        TClue clue = clue(1);
        clue.setPhone("13800138000");
        clue.setState(68);
        ClueLifecycleRequest request = lifecycleRequest("客户重新咨询");

        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(dicMapper.selectDicValues(any())).thenReturn(clueStates());
        when(tClueMapper.countActiveByPhoneExcludingId("13800138000", 1, 68, 9)).thenReturn(0);
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tClueMapper.updateStateAtomic(1, 68, 6, 99, null)).thenReturn(1);

        boolean result = clueService.restoreClue(1, request);

        assertTrue(result);
        verify(tClueMapper).updateStateAtomic(1, 68, 6, 99, null);
        verify(auditRecorder).record(
                eq(AuditActionEnum.CLUE_RESTORE),
                eq("1"),
                eq("SUCCESS"),
                eq("{\"reason\":\"客户重新咨询\"}"));
    }

    @Test
    void restoreClue_activeDuplicate_shouldRejectWithoutUpdate() {
        TClue clue = clue(1);
        clue.setPhone("13800138000");
        clue.setState(68);
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(dicMapper.selectDicValues(any())).thenReturn(clueStates());
        when(tClueMapper.countActiveByPhoneExcludingId("13800138000", 1, 68, 9)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.restoreClue(1, lifecycleRequest("恢复跟进")));

        assertEquals(CodeEnum.DUPLICATE, exception.getCodeEnum());
        verify(tClueMapper, never()).updateStateAtomic(any(), any(), any(), any(), any());
    }

    @Test
    void restoreClue_notClosed_shouldRejectWithoutUpdate() {
        TClue clue = clue(1);
        clue.setState(6);
        when(tClueMapper.selectScopedByPrimaryKey(1, null)).thenReturn(clue);
        when(dicMapper.selectDicValues(any())).thenReturn(clueStates());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> clueService.restoreClue(1, lifecycleRequest("恢复跟进")));

        assertEquals(CodeEnum.OPERATION_FAILED, exception.getCodeEnum());
        verify(tClueMapper, never()).updateStateAtomic(any(), any(), any(), any(), any());
    }

    private TClue clue(Integer id) {
        TClue clue = new TClue();
        clue.setId(id);
        clue.setOwnerId(1);
        return clue;
    }

    private TUser owner(Integer id) {
        TUser user = new TUser();
        user.setId(id);
        user.setName("负责人" + id);
        return user;
    }

    private ClueLifecycleRequest lifecycleRequest(String reason) {
        ClueLifecycleRequest request = new ClueLifecycleRequest();
        request.setReason(reason);
        return request;
    }

    private List<TDicValue> clueStates() {
        return List.of(
                clueState(6, "attempt_contact"),
                clueState(9, "converted"),
                clueState(68, "closed"));
    }

    private TDicValue clueState(Integer id, String valueCode) {
        TDicValue value = new TDicValue();
        value.setId(id);
        value.setTypeCode("clueState");
        value.setValueCode(valueCode);
        return value;
    }

    private ByteArrayInputStream emptyExcelInput() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream, ClueExcelRaw.class).sheet().doWrite(Collections.emptyList());
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
