package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.ToolSchemas;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.QuoteDetailResponse;
import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.model.TOpportunity;
import com.autodealer.crm.model.TQuote;
import com.autodealer.crm.model.TQuoteVersion;
import com.autodealer.crm.model.TQuoteVersionItem;
import com.autodealer.crm.model.TTestDrive;
import com.autodealer.crm.result.NameValue;
import com.autodealer.crm.result.SummaryData;
import com.autodealer.crm.service.DeliveryService;
import com.autodealer.crm.service.OpportunityService;
import com.autodealer.crm.service.QuoteService;
import com.autodealer.crm.service.StatisticService;
import com.autodealer.crm.service.TestDriveService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiBusinessDetailToolExecutorsTest {
    private final ToolExecutionContext context = new ToolExecutionContext(run());
    private final AiSensitiveDataSanitizer sanitizer = new AiSensitiveDataSanitizer();

    @Test
    void toolSchemas_shouldExposeOnlyDeclaredArguments() {
        assertTrue(properties("get_opportunity_detail").containsKey("opportunityId"));
        assertTrue(properties("get_quote_detail").containsKey("quoteId"));
        assertTrue(properties("get_test_drive_detail").containsKey("testDriveId"));
        assertTrue(properties("get_delivery_detail").containsKey("deliveryId"));
        assertTrue(properties("get_business_overview").isEmpty());
    }

    @Test
    void displayDtos_shouldNotExposeInternalNumericIds() {
        assertRecordOmits(AiToolDtos.OpportunityDetail.class, "id");
        assertRecordOmits(AiToolDtos.QuoteDetail.class, "id", "customerId", "opportunityId");
        assertRecordOmits(AiToolDtos.TestDriveDetail.class, "id");
        assertRecordOmits(AiToolDtos.DeliveryDetail.class,
                "id", "tranId", "customerId", "vehicleId", "responsibleUserId");
    }

    @Test
    void getOpportunityDetail_shouldUseScopedServiceAndReturnSanitizedDto() {
        OpportunityService service = mock(OpportunityService.class);
        AiToolArgumentBinder binder = mock(AiToolArgumentBinder.class);
        AiToolDtos.GetOpportunityDetailRequest request = new AiToolDtos.GetOpportunityDetailRequest();
        request.setOpportunityId(10L);
        when(binder.bind(any(), eq(AiToolDtos.GetOpportunityDetailRequest.class))).thenReturn(request);
        TOpportunity opportunity = new TOpportunity();
        opportunity.setId(10L);
        opportunity.setOpportunityNo("OPP10");
        opportunity.setRequirement("联系 13812345678");
        when(service.getOpportunity(10L)).thenReturn(opportunity);
        GetOpportunityDetailToolExecutor executor =
                new GetOpportunityDetailToolExecutor(service, binder, sanitizer);

        ToolExecutionResult result = executor.execute(context, Map.of());

        AiToolDtos.OpportunityDetail data = assertInstanceOf(
                AiToolDtos.OpportunityDetail.class, result.data());
        assertEquals("联系 138****5678", data.requirement());
        assertEquals(PermissionCodes.OPPORTUNITY_VIEW, executor.definition().permissionCode());
        verify(service).getOpportunity(10L);
    }

    @Test
    void getQuoteDetail_shouldLimitItemsAndNotReturnEntityGraph() {
        QuoteService service = mock(QuoteService.class);
        AiToolArgumentBinder binder = mock(AiToolArgumentBinder.class);
        AiToolDtos.GetQuoteDetailRequest request = new AiToolDtos.GetQuoteDetailRequest();
        request.setQuoteId(11L);
        when(binder.bind(any(), eq(AiToolDtos.GetQuoteDetailRequest.class))).thenReturn(request);
        QuoteDetailResponse detail = new QuoteDetailResponse();
        TQuote quote = new TQuote();
        quote.setId(11L);
        quote.setQuoteNo("Q11");
        detail.setQuote(quote);
        TQuoteVersion version = new TQuoteVersion();
        version.setVersionNo(2);
        detail.setCurrentVersion(version);
        detail.setItems(IntStream.range(0, 25).mapToObj(index -> {
            TQuoteVersionItem item = new TQuoteVersionItem();
            item.setProductSku("SKU-" + index);
            item.setProductName("车辆 " + index);
            return item;
        }).toList());
        when(service.getQuoteDetail(11L)).thenReturn(detail);
        GetQuoteDetailToolExecutor executor = new GetQuoteDetailToolExecutor(service, binder, sanitizer);

        AiToolDtos.QuoteDetail data = assertInstanceOf(AiToolDtos.QuoteDetail.class,
                executor.execute(context, Map.of()).data());

        assertEquals(25, data.totalItemCount());
        assertEquals(20, data.items().size());
        assertEquals(PermissionCodes.QUOTE_VIEW, executor.definition().permissionCode());
    }

    @Test
    void getTestDriveDetail_shouldMaskPhoneAndOmitVin() {
        TestDriveService service = mock(TestDriveService.class);
        AiToolArgumentBinder binder = mock(AiToolArgumentBinder.class);
        AiToolDtos.GetTestDriveDetailRequest request = new AiToolDtos.GetTestDriveDetailRequest();
        request.setTestDriveId(12L);
        when(binder.bind(any(), eq(AiToolDtos.GetTestDriveDetailRequest.class))).thenReturn(request);
        TTestDrive drive = new TTestDrive();
        drive.setId(12L);
        drive.setVin("VIN-SHOULD-NOT-LEAVE");
        drive.setContactPhone("13912345678");
        when(service.getTestDrive(12L)).thenReturn(drive);
        GetTestDriveDetailToolExecutor executor =
                new GetTestDriveDetailToolExecutor(service, binder, sanitizer);

        AiToolDtos.TestDriveDetail data = assertInstanceOf(AiToolDtos.TestDriveDetail.class,
                executor.execute(context, Map.of()).data());

        assertEquals("139****5678", data.contactPhoneMasked());
        assertEquals(PermissionCodes.TEST_DRIVE_VIEW, executor.definition().permissionCode());
    }

    @Test
    void getDeliveryDetail_shouldOmitSignEvidence() {
        DeliveryService service = mock(DeliveryService.class);
        AiToolArgumentBinder binder = mock(AiToolArgumentBinder.class);
        AiToolDtos.GetDeliveryDetailRequest request = new AiToolDtos.GetDeliveryDetailRequest();
        request.setDeliveryId(13L);
        when(binder.bind(any(), eq(AiToolDtos.GetDeliveryDetailRequest.class))).thenReturn(request);
        TDelivery delivery = new TDelivery();
        delivery.setId(13L);
        delivery.setSignEvidence("sensitive-file-key");
        when(service.getDelivery(13L)).thenReturn(delivery);
        GetDeliveryDetailToolExecutor executor =
                new GetDeliveryDetailToolExecutor(service, binder, sanitizer);

        AiToolDtos.DeliveryDetail data = assertInstanceOf(AiToolDtos.DeliveryDetail.class,
                executor.execute(context, Map.of()).data());

        assertNull(data.signMethod());
        assertEquals(PermissionCodes.DELIVERY_VIEW, executor.definition().permissionCode());
    }

    @Test
    void getBusinessOverview_shouldUseThreeScopedMetricsAndLimitSeries() {
        StatisticService service = mock(StatisticService.class);
        AiToolArgumentBinder binder = mock(AiToolArgumentBinder.class);
        when(binder.bind(any(), eq(AiToolDtos.GetBusinessOverviewRequest.class)))
                .thenReturn(new AiToolDtos.GetBusinessOverviewRequest());
        SummaryData summary = SummaryData.builder()
                .totalCustomerCount(8)
                .successTranAmount(BigDecimal.valueOf(1000))
                .build();
        List<NameValue> metrics = IntStream.range(0, 25)
                .mapToObj(index -> new NameValue("指标" + index, index))
                .toList();
        when(service.loadSummaryData()).thenReturn(summary);
        when(service.loadSaleFunnelData()).thenReturn(metrics);
        when(service.loadSourcePieData()).thenReturn(metrics);
        GetBusinessOverviewToolExecutor executor = new GetBusinessOverviewToolExecutor(service, binder);

        AiToolDtos.BusinessOverview data = assertInstanceOf(AiToolDtos.BusinessOverview.class,
                executor.execute(context, Map.of()).data());

        assertEquals(8, data.summary().totalCustomerCount());
        assertEquals(20, data.salesFunnel().size());
        assertEquals(20, data.sourceDistribution().size());
        assertEquals(PermissionCodes.STATISTIC_VIEW, executor.definition().permissionCode());
        verify(service).loadSummaryData();
        verify(service).loadSaleFunnelData();
        verify(service).loadSourcePieData();
    }

    private TAiRun run() {
        TAiRun run = new TAiRun();
        run.setId(1L);
        run.setRunNo("AIR1");
        return run;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(String toolName) {
        return (Map<String, Object>) ToolSchemas.forTool(toolName).get("properties");
    }

    private void assertRecordOmits(Class<?> recordType, String... forbiddenNames) {
        Set<String> fields = Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());
        Arrays.stream(forbiddenNames).forEach(name -> assertFalse(fields.contains(name),
                () -> recordType.getSimpleName() + " 不应向 AI 展示内部字段 " + name));
    }
}
