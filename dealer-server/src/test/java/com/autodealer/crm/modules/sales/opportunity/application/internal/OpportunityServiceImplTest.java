package com.autodealer.crm.modules.sales.opportunity.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.AdvanceOpportunityStageRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.CreateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.OpportunityResultRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.enums.OpportunityStage;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.customer.persistence.mapper.TCustomerMapper;
import com.autodealer.crm.modules.sales.opportunity.persistence.mapper.TOpportunityMapper;
import com.autodealer.crm.modules.sales.opportunity.persistence.mapper.TOpportunityStageHistoryMapper;
import com.autodealer.crm.modules.commerce.quote.persistence.mapper.TQuoteMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranMapper;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunityStageHistory;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.opportunity.application.internal.OpportunityServiceImpl;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceImplTest {

    @Mock private TOpportunityMapper opportunityMapper;
    @Mock private TOpportunityStageHistoryMapper stageHistoryMapper;
    @Mock private TCustomerMapper customerMapper;
    @Mock private TQuoteMapper quoteMapper;
    @Mock private TTranMapper tranMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @Mock private EmploymentResponsibilityGuard responsibilityGuard;
    @InjectMocks private OpportunityServiceImpl opportunityService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        lenient().when(customerMapper.selectScopedById(eq(10), nullable(Integer.class))).thenReturn(customer());
        lenient().when(stageHistoryMapper.insert(any())).thenReturn(1);
    }

    @Test
    void createOpportunity_shouldCreateIndependentOpportunityWithoutTransaction() {
        CreateOpportunityRequest request = createRequest();
        when(opportunityMapper.insert(any())).thenAnswer(invocation -> {
            TOpportunity opportunity = invocation.getArgument(0);
            opportunity.setId(100L);
            return 1;
        });
        TOpportunity persisted = opportunity(100L, OpportunityStage.INITIAL_CONTACT);
        when(opportunityMapper.selectById(100L)).thenReturn(persisted);

        TOpportunity result = opportunityService.createOpportunity(request);

        assertSame(persisted, result);
        ArgumentCaptor<TOpportunity> opportunityCaptor = ArgumentCaptor.forClass(TOpportunity.class);
        verify(opportunityMapper).insert(opportunityCaptor.capture());
        TOpportunity inserted = opportunityCaptor.getValue();
        assertEquals(10, inserted.getCustomerId());
        assertEquals(3, inserted.getOwnerId());
        assertEquals(OpportunityStage.INITIAL_CONTACT.name(), inserted.getStage());
        assertEquals("置换一台中大型SUV", inserted.getRequirement());

        ArgumentCaptor<TOpportunityStageHistory> historyCaptor =
                ArgumentCaptor.forClass(TOpportunityStageHistory.class);
        verify(stageHistoryMapper).insert(historyCaptor.capture());
        assertEquals(OpportunityStage.INITIAL_CONTACT.name(), historyCaptor.getValue().getToStage());
        verify(tranMapper, never()).selectByPrimaryKey(anyInt());
        verify(auditRecorder).record(AuditActionEnum.OPPORTUNITY_CREATE, "100");
    }

    @Test
    void createOpportunity_inaccessibleCustomer_shouldRejectBeforeWrite() {
        when(customerMapper.selectScopedById(eq(10), nullable(Integer.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.createOpportunity(createRequest()));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(opportunityMapper, never()).insert(any());
        verify(stageHistoryMapper, never()).insert(any());
    }

    @Test
    void advanceStage_illegalJump_shouldRejectBeforeUpdate() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.INITIAL_CONTACT));
        AdvanceOpportunityStageRequest request =
                advanceRequest(OpportunityStage.INITIAL_CONTACT, OpportunityStage.QUOTING);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.advanceStage(100L, request));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(opportunityMapper, never()).updateStageIfCurrent(anyLong(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
        verify(stageHistoryMapper, never()).insert(any());
    }

    @Test
    void advanceStage_success_shouldUseCasAndWriteHistory() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.INITIAL_CONTACT));
        when(opportunityMapper.updateStageIfCurrent(eq(100L), eq("INITIAL_CONTACT"),
                eq("NEEDS_ANALYSIS"), eq("客户已确认需求"), isNull(), isNull(),
                eq(LocalDate.of(2026, 7, 1)), isNull(), any(), eq(7)))
                .thenReturn(1);
        TOpportunity persisted = opportunity(100L, OpportunityStage.NEEDS_ANALYSIS);
        when(opportunityMapper.selectById(100L)).thenReturn(persisted);

        TOpportunity result = opportunityService.advanceStage(
                100L, advanceRequest(OpportunityStage.INITIAL_CONTACT, OpportunityStage.NEEDS_ANALYSIS));

        assertSame(persisted, result);
        ArgumentCaptor<TOpportunityStageHistory> historyCaptor =
                ArgumentCaptor.forClass(TOpportunityStageHistory.class);
        verify(stageHistoryMapper).insert(historyCaptor.capture());
        assertEquals("INITIAL_CONTACT", historyCaptor.getValue().getFromStage());
        assertEquals("NEEDS_ANALYSIS", historyCaptor.getValue().getToStage());
        verify(auditRecorder).record(AuditActionEnum.OPPORTUNITY_STAGE_CHANGE, "100");
    }

    @Test
    void advanceStage_toQuotingWithoutQuote_shouldReject() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.VEHICLE_MATCHING));
        when(quoteMapper.countActiveByOpportunityId(100L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.advanceStage(
                        100L, advanceRequest(OpportunityStage.VEHICLE_MATCHING, OpportunityStage.QUOTING)));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(opportunityMapper, never()).updateStageIfCurrent(anyLong(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
        verify(stageHistoryMapper, never()).insert(any());
    }

    @Test
    void markWon_withoutOrder_shouldReject() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.PENDING_APPROVAL));
        OpportunityResultRequest request = resultRequest();
        request.setOrderTranId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.markWon(100L, request));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(opportunityMapper, never()).updateStageIfCurrent(anyLong(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void markWon_unestablishedOrder_shouldReject() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.PENDING_APPROVAL));
        TTran tran = transaction(20, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(20)).thenReturn(tran);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.markWon(100L, resultRequest()));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(opportunityMapper, never()).updateStageIfCurrent(anyLong(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void markWon_withEstablishedOrder_shouldRecordWonFactOnly() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.PENDING_APPROVAL));
        when(tranMapper.selectByPrimaryKey(20)).thenReturn(transaction(20, TranStage.APPROVED));
        when(opportunityMapper.updateStageIfCurrent(eq(100L), eq("PENDING_APPROVAL"), eq("WON"),
                eq("客户已签约"), isNull(), eq("签约完成"), isNull(), eq(20), any(), eq(7)))
                .thenReturn(1);
        TOpportunity persisted = opportunity(100L, OpportunityStage.WON);
        persisted.setOrderTranId(20);
        when(opportunityMapper.selectById(100L)).thenReturn(persisted);

        TOpportunity result = opportunityService.markWon(100L, resultRequest());

        assertSame(persisted, result);
        verify(auditRecorder).record(AuditActionEnum.OPPORTUNITY_WIN, "100");
    }

    @Test
    void shelve_withoutNextActionTime_shouldReject() {
        when(opportunityMapper.selectByIdForUpdate(100L))
                .thenReturn(opportunity(100L, OpportunityStage.NEGOTIATION));
        OpportunityResultRequest request = resultRequest();
        request.setNextActionTime(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.shelve(100L, request));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(opportunityMapper, never()).updateStageIfCurrent(anyLong(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test void handoverOwnerCannotReceiveNewOpportunity(){
        org.mockito.Mockito.doThrow(new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT)).when(responsibilityGuard).requireActiveOwner(3);
        BusinessException error=assertThrows(BusinessException.class,()->opportunityService.createOpportunity(createRequest()));
        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,error.getCodeEnum());verify(opportunityMapper,never()).insert(any());verify(auditRecorder,never()).record(any(),anyString());
    }

    private CreateOpportunityRequest createRequest() {
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setCustomerId(10);
        request.setProductId(1L);
        request.setRequirement("置换一台中大型SUV");
        request.setExpectedAmount(new BigDecimal("380000.00"));
        request.setExpectedCloseDate(LocalDate.of(2026, 7, 20));
        request.setNextActionTime(LocalDate.of(2026, 7, 1));
        return request;
    }

    private AdvanceOpportunityStageRequest advanceRequest(OpportunityStage expected, OpportunityStage target) {
        AdvanceOpportunityStageRequest request = new AdvanceOpportunityStageRequest();
        request.setExpectedStage(expected.name());
        request.setTargetStage(target.name());
        request.setReason("客户已确认需求");
        request.setNextActionTime(LocalDate.of(2026, 7, 1));
        return request;
    }

    private OpportunityResultRequest resultRequest() {
        OpportunityResultRequest request = new OpportunityResultRequest();
        request.setOrderTranId(20);
        request.setReason("客户已签约");
        request.setRemark("签约完成");
        request.setNextActionTime(LocalDate.of(2026, 7, 5));
        return request;
    }

    private TCustomer customer() {
        TCustomer customer = new TCustomer();
        customer.setId(10);
        customer.setOwnerId(3);
        customer.setClueId(8);
        return customer;
    }

    private TOpportunity opportunity(Long id, OpportunityStage stage) {
        TOpportunity opportunity = new TOpportunity();
        opportunity.setId(id);
        opportunity.setCustomerId(10);
        opportunity.setOwnerId(3);
        opportunity.setStage(stage.name());
        opportunity.setRequirement("置换一台中大型SUV");
        opportunity.setProductId(1L);
        opportunity.setExpectedAmount(new BigDecimal("380000.00"));
        return opportunity;
    }

    private TTran transaction(Integer id, TranStage stage) {
        TTran tran = new TTran();
        tran.setId(id);
        tran.setCustomerId(10);
        tran.setStage(stage);
        return tran;
    }
}
