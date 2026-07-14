package com.autodealer.crm.modules.ai.application.internal.tool.executor;

import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.application.internal.tool.AiToolArgumentBinder;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.modules.commerce.catalog.application.api.ProductService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetInventoryAlertsToolExecutorTest {

    @Test
    void execute_shouldUseProductService_getStockAlerts() {
        ProductService productService = mock(ProductService.class);
        AiToolArgumentBinder argumentBinder = mock(AiToolArgumentBinder.class);
        AiToolDtos.GetInventoryAlertsRequest request = new AiToolDtos.GetInventoryAlertsRequest();
        request.setPage(2);
        request.setSize(30);
        request.setSku("BMW");
        request.setName("X5");
        request.setCategoryId(1L);
        when(argumentBinder.bind(any(), eq(AiToolDtos.GetInventoryAlertsRequest.class))).thenReturn(request);
        TProduct product = new TProduct();
        product.setId(10L);
        product.setSku("BMW-X5");
        product.setName("宝马 X5");
        product.setStock(1);
        product.setMinStock(2);
        product.setStatus("ON_SALE");
        when(productService.getStockAlerts(2, 20, "BMW", "X5", 1L))
                .thenReturn(new PageInfo<>(List.of(product)));
        GetInventoryAlertsToolExecutor executor =
                new GetInventoryAlertsToolExecutor(productService, argumentBinder);
        TAiRun run = new TAiRun();
        run.setId(1L);

        ToolExecutionResult result = executor.execute(new ToolExecutionContext(run), Map.of());

        assertEquals("返回库存预警 1 条", result.outputSummary());
        verify(productService).getStockAlerts(2, 20, "BMW", "X5", 1L);
    }
}
