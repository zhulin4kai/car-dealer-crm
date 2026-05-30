package com.bjpowernode.web;

import com.bjpowernode.model.*;
import com.bjpowernode.query.TranQuery;
import com.bjpowernode.service.TranService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TranControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranService tranService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TUser currentUser = new TUser();
        currentUser.setId(1);
        currentUser.setLoginAct("testuser");
        currentUser.setName("测试用户");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET /api/tran/list ====================

    @Test
    void list_shouldReturnPageInfo() throws Exception {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("TN20260101000001");
        tran.setMoney(new BigDecimal("50000"));
        PageInfo<TTran> pageInfo = new PageInfo<>(Collections.singletonList(tran));

        when(tranService.getTransactionList(any(TranQuery.class), eq(1), eq(10)))
                .thenReturn(pageInfo);

        mockMvc.perform(get("/api/tran/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].tranNo").value("TN20260101000001"));
    }

    @Test
    void list_withDefaultParams_shouldUseDefaults() throws Exception {
        PageInfo<TTran> pageInfo = new PageInfo<>(Collections.emptyList());
        when(tranService.getTransactionList(any(TranQuery.class), eq(1), eq(10)))
                .thenReturn(pageInfo);

        mockMvc.perform(get("/api/tran/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/tran/{id} ====================

    @Test
    void detail_shouldReturnTran() throws Exception {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("TN20260101000001");
        tran.setCustomerId(100);
        tran.setMoney(new BigDecimal("80000"));

        when(tranService.getTransactionById(1)).thenReturn(tran);

        mockMvc.perform(get("/api/tran/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.tranNo").value("TN20260101000001"));
    }

    @Test
    void detail_nonExistentId_shouldReturnNull() throws Exception {
        when(tranService.getTransactionById(999)).thenReturn(null);

        mockMvc.perform(get("/api/tran/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== POST /api/tran/create ====================

    @Test
    void create_shouldReturnSuccess() throws Exception {
        TranCreateRequest request = new TranCreateRequest();
        request.setCustomerId(100);
        request.setAmount(new BigDecimal("50000"));
        request.setDescription("新车交易");
        TranCreateRequest.ProductDetail product = new TranCreateRequest.ProductDetail();
        product.setProductId(1);
        product.setQuantity(1);
        product.setPrice(new BigDecimal("50000"));
        request.setProducts(Collections.singletonList(product));

        when(tranService.createTransaction(any(TTran.class), anyList())).thenReturn(1);

        mockMvc.perform(post("/api/tran/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void create_withInvalidDateFormat_shouldReturnFail() throws Exception {
        TranCreateRequest request = new TranCreateRequest();
        request.setCustomerId(100);
        request.setAmount(new BigDecimal("50000"));
        request.setExpectedDeliveryDate("invalid-date");
        TranCreateRequest.ProductDetail product = new TranCreateRequest.ProductDetail();
        product.setProductId(1);
        product.setQuantity(1);
        product.setPrice(new BigDecimal("50000"));
        request.setProducts(Collections.singletonList(product));

        mockMvc.perform(post("/api/tran/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("日期格式错误"));
    }

    // ==================== PUT /api/tran/update ====================

    @Test
    void update_shouldReturnSuccess() throws Exception {
        TranCreateRequest request = new TranCreateRequest();
        request.setId(1);
        request.setCustomerId(100);
        request.setAmount(new BigDecimal("60000"));
        request.setDescription("更新交易");

        when(tranService.updateTransaction(any(TTran.class))).thenReturn(true);

        mockMvc.perform(put("/api/tran/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void update_withoutId_shouldReturnFail() throws Exception {
        TranCreateRequest request = new TranCreateRequest();
        request.setCustomerId(100);
        request.setAmount(new BigDecimal("60000"));

        mockMvc.perform(put("/api/tran/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("交易ID不能为空"));
    }

    // ==================== PUT /api/tran/settle/{id} ====================

    @Test
    void settle_withProducts_shouldReturnSuccess() throws Exception {
        TTranProduct product = new TTranProduct();
        product.setProductId(1);
        product.setQuantity(2);
        product.setPrice(new BigDecimal("25000"));

        when(tranService.getTransactionProducts(1)).thenReturn(Collections.singletonList(product));
        when(tranService.updateTransaction(any(TTran.class))).thenReturn(true);

        mockMvc.perform(put("/api/tran/settle/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void settle_withCustomAmount_shouldUseProvidedAmount() throws Exception {
        TTranProduct product = new TTranProduct();
        product.setProductId(1);
        product.setQuantity(1);
        product.setPrice(new BigDecimal("50000"));

        when(tranService.getTransactionProducts(1)).thenReturn(Collections.singletonList(product));
        when(tranService.updateTransaction(any(TTran.class))).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", "45000");

        mockMvc.perform(put("/api/tran/settle/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void settle_noProducts_shouldReturnFail() throws Exception {
        when(tranService.getTransactionProducts(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(put("/api/tran/settle/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该交易没有产品信息，无法结算"));
    }

    @Test
    void settle_negativeAmount_shouldReturnFail() throws Exception {
        TTranProduct product = new TTranProduct();
        product.setProductId(1);
        product.setQuantity(1);
        product.setPrice(new BigDecimal("50000"));

        when(tranService.getTransactionProducts(1)).thenReturn(Collections.singletonList(product));

        Map<String, Object> body = new HashMap<>();
        body.put("amount", "-100");

        mockMvc.perform(put("/api/tran/settle/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("结算金额不能为负数"));
    }

    // ==================== ISSUE-002: settle状态校验测试 ====================

    @Test
    void settle_invalidCurrentStage_shouldReturnFail() throws Exception {
        // 测试：结算时当前状态必须为41（待报价）
        TTranProduct product = new TTranProduct();
        product.setProductId(1);
        product.setQuantity(2);
        product.setPrice(new BigDecimal("25000"));

        when(tranService.getTransactionProducts(1)).thenReturn(Collections.singletonList(product));
        when(tranService.updateTransaction(any(TTran.class))).thenThrow(new RuntimeException("当前交易状态不允许执行此操作，需要状态: 41"));

        mockMvc.perform(put("/api/tran/settle/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("当前交易状态不允许执行此操作，需要状态: 41"));
    }

    // ==================== PUT /api/tran/approve/{id} ====================

    @Test
    void approve_shouldReturnSuccess() throws Exception {
        when(tranService.approveTran(eq(1), eq(true), eq("同意"), eq(1))).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("approved", true);
        body.put("comment", "同意");

        mockMvc.perform(put("/api/tran/approve/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void approve_missingComment_shouldReturnFail() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approved", true);

        mockMvc.perform(put("/api/tran/approve/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("审批结果和审批意见不能为空"));
    }

    @Test
    void approve_missingApproved_shouldReturnFail() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("comment", "同意");

        mockMvc.perform(put("/api/tran/approve/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("审批结果和审批意见不能为空"));
    }

    // ==================== POST /api/tran/invoice ====================

    @Test
    void createInvoice_shouldReturnSuccess() throws Exception {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setInvoiceNo("INV20260001");
        invoice.setAmount(new BigDecimal("50000"));

        when(tranService.createTranInvoice(any(TTranInvoice.class))).thenReturn(true);

        mockMvc.perform(post("/api/tran/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void createInvoice_serviceFails_shouldReturnFalse() throws Exception {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);

        when(tranService.createTranInvoice(any(TTranInvoice.class))).thenReturn(false);

        mockMvc.perform(post("/api/tran/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }

    // ==================== PUT /api/tran/invoice/status ====================

    @Test
    void updateInvoiceStatus_shouldReturnSuccess() throws Exception {
        when(tranService.updateTranInvoiceStatus(eq(1), eq("已开票"), eq(1))).thenReturn(true);

        Map<String, String> body = new HashMap<>();
        body.put("status", "已开票");

        mockMvc.perform(put("/api/tran/invoice/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateInvoiceStatus_emptyStatus_shouldReturnFail() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("status", "");

        mockMvc.perform(put("/api/tran/invoice/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("状态不能为空"));
    }

    // ==================== DELETE /api/tran/{id} ====================

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        when(tranService.deleteTransaction(1)).thenReturn(true);

        mockMvc.perform(delete("/api/tran/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除成功"));
    }

    @Test
    void delete_failure_shouldReturnFail() throws Exception {
        when(tranService.deleteTransaction(1)).thenReturn(false);

        mockMvc.perform(delete("/api/tran/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("删除失败"));
    }

    // ==================== POST /api/tran/batch-delete ====================

    @Test
    void batchDelete_shouldReturnSuccess() throws Exception {
        when(tranService.batchDeleteTransactions(anyList())).thenReturn(true);

        Map<String, List<Integer>> body = new HashMap<>();
        body.put("ids", Arrays.asList(1, 2, 3));

        mockMvc.perform(post("/api/tran/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("批量删除成功"));
    }

    @Test
    void batchDelete_emptyIds_shouldReturnFail() throws Exception {
        Map<String, List<Integer>> body = new HashMap<>();
        body.put("ids", Collections.emptyList());

        mockMvc.perform(post("/api/tran/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("请选择要删除的交易"));
    }

    @Test
    void batchDelete_nullIds_shouldReturnFail() throws Exception {
        Map<String, List<Integer>> body = new HashMap<>();

        mockMvc.perform(post("/api/tran/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("请选择要删除的交易"));
    }

    // ==================== GET /api/tran/remarks/{tranId} ====================

    @Test
    void remarks_shouldReturnList() throws Exception {
        TTranRemark remark = new TTranRemark();
        remark.setId(1);
        remark.setTranId(1);
        remark.setNoteContent("电话回访");

        when(tranService.getTransactionRemarks(1)).thenReturn(Collections.singletonList(remark));

        mockMvc.perform(get("/api/tran/remarks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].noteContent").value("电话回访"));
    }

    @Test
    void remarks_emptyList_shouldReturnEmpty() throws Exception {
        when(tranService.getTransactionRemarks(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tran/remarks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== GET /api/tran/products/{id} ====================

    @Test
    void getTransactionProducts_shouldReturnList() throws Exception {
        TTranProduct product = new TTranProduct();
        product.setId(1);
        product.setProductId(1);
        product.setProductName("Model X");
        product.setQuantity(1);
        product.setPrice(new BigDecimal("80000"));

        when(tranService.getTransactionProductDetails(1)).thenReturn(Collections.singletonList(product));

        mockMvc.perform(get("/api/tran/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].productName").value("Model X"));
    }

    // ==================== GET /api/tran/approve/info/{tranId} ====================

    @Test
    void getApproveInfo_shouldReturnApprove() throws Exception {
        TTranApprove approve = new TTranApprove();
        approve.setId(1);
        approve.setTranId(1);
        approve.setApproveResult(true);
        approve.setApproveComment("同意");

        when(tranService.getTranApprove(1)).thenReturn(approve);

        mockMvc.perform(get("/api/tran/approve/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.approveResult").value(true))
                .andExpect(jsonPath("$.data.approveComment").value("同意"));
    }

    @Test
    void getApproveInfo_noApprove_shouldReturnNull() throws Exception {
        when(tranService.getTranApprove(1)).thenReturn(null);

        mockMvc.perform(get("/api/tran/approve/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
