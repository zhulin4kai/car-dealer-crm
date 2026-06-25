package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.*;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.TranQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.TranService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 交易管理控制器
 */
@RestController
@RequestMapping("/api/tran")
public class TranController {

    @Resource
    private TranService tranService;

    /**
     * 获取交易列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_LIST + "')")
    @GetMapping("/list")
    public R<PageInfo<TTran>> list(
            TranQuery query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(tranService.getTransactionList(query, page, size));
    }

    /**
     * 获取交易详情
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/{id}")
    public R<TTran> detail(@PathVariable Integer id) {
        return R.OK(tranService.getTransactionById(id));
    }

    /**
     * 创建交易 — 商品价格由服务端从数据库查询，不接受客户端传入
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CREATE + "')")
    @PostMapping("/create")
    public R<Integer> create(@Valid @RequestBody CreateTranRequest request) {
        TTran tran = new TTran();
        tran.setCustomerId(request.getCustomerId());
        tran.setDescription(request.getDescription());
        if (request.getExpectedDeliveryDate() != null) {
            tran.setExpectedDate(Date.from(
                    request.getExpectedDeliveryDate().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        tran.setStage(TranStage.QUOTATION);

        List<TTranProduct> products = request.getProducts().stream()
                .map(item -> {
                    TTranProduct tranProduct = new TTranProduct();
                    tranProduct.setProductId(item.getProductId());
                    tranProduct.setQuantity(item.getQuantity());
                    return tranProduct;
                })
                .toList();

        return R.OK(tranService.createTransaction(tran, products));
    }

    /**
     * 更新交易 — 商品价格由服务端从数据库查询，不接受客户端传入
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_EDIT + "')")
    @PutMapping("/update")
    public R<Boolean> update(@Valid @RequestBody UpdateTranRequest request) {
        TTran tran = new TTran();
        tran.setId(request.getId());
        tran.setCustomerId(request.getCustomerId());
        tran.setDescription(request.getDescription());
        if (request.getExpectedDeliveryDate() != null) {
            tran.setExpectedDate(Date.from(
                    request.getExpectedDeliveryDate().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }

        List<TTranProduct> products = null;
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            products = request.getProducts().stream()
                    .map(item -> {
                        TTranProduct tranProduct = new TTranProduct();
                        tranProduct.setProductId(item.getProductId());
                        tranProduct.setQuantity(item.getQuantity());
                        return tranProduct;
                    })
                    .toList();
        }

        boolean result = tranService.updateTransactionWithProducts(tran, products);
        return R.OK(result);
    }

    /**
     * 结算预览（只读）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @PostMapping("/{id}/settlement-preview")
    public R<SettlementPreviewResponse> settlementPreview(
            @PathVariable Integer id,
            @Valid @RequestBody SettlementPreviewRequest request) {
        SettlementPreviewResponse preview = tranService.getSettlementPreview(id, request.getPromotionId());
        return R.OK(preview);
    }

    /**
     * 获取交易当前可用于结算的促销活动。
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @GetMapping("/{id}/available-promotions")
    public R<List<TProductPromotion>> availablePromotions(@PathVariable Integer id) {
        return R.OK(tranService.getAvailablePromotions(id));
    }

    /**
     * 结算交易（带 CAS 版本控制）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @PutMapping("/{id}/settle")
    public R<SettlementPreviewResponse> settle(
            @PathVariable Integer id,
            @Valid @RequestBody SettleRequest request) {
        return R.OK(tranService.settleTransaction(id, request));
    }

    /**
     * 审批交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_APPROVE + "')")
    @PutMapping("/approve/{id}")
    public R<Boolean> approve(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveTranRequest request) {
        boolean result = tranService.approveTran(id, request.getApproved(), request.getComment());
        return R.OK(result);
    }

    /**
     * 获取交易审批信息
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/approve/info/{tranId}")
    public R<TTranApprove> getApproveInfo(@PathVariable Integer tranId) {
        TTranApprove approve = tranService.getTranApprove(tranId);
        return R.OK(approve);
    }

    /**
     * 创建发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/invoice")
    public R<Boolean> createInvoice(@Valid @RequestBody CreateTranInvoiceRequest request) {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(request.getTranId());
        invoice.setAmount(request.getAmount());
        invoice.setType(request.getType());
        invoice.setTitle(request.getTitle());
        invoice.setTaxNumber(request.getTaxNumber());
        invoice.setBankName(request.getBankName());
        invoice.setBankAccount(request.getBankAccount());
        invoice.setAddress(request.getAddress());
        invoice.setPhone(request.getPhone());
        invoice.setRemark(request.getRemark());
        boolean result = tranService.createTranInvoice(invoice);
        return R.OK(result);
    }

    /**
     * 获取交易发票列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/invoice/{tranId}")
    public R<List<TTranInvoice>> getInvoiceList(@PathVariable Integer tranId) {
        List<TTranInvoice> invoices = tranService.getTranInvoices(tranId);
        return R.OK(invoices);
    }

    /**
     * 更新发票状态
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PutMapping("/invoice/{invoiceId}/status")
    public R<Boolean> updateInvoiceStatus(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody UpdateInvoiceStatusRequest request) {
        boolean result = tranService.updateTranInvoiceStatus(invoiceId, request.getStatus());
        return R.OK(result);
    }

    /**
     * 获取交易备注
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/remarks/{tranId}")
    public R<List<TTranRemark>> remarks(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionRemarks(tranId));
    }

    /**
     * 获取交易产品详情列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/products/{id}")
    public R<List<TTranProduct>> getTransactionProducts(@PathVariable Integer id) {
        return R.OK(tranService.getTransactionProductDetails(id));
    }

    /**
     * 删除交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_DELETE + "')")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Integer id) {
        boolean result = tranService.deleteTransaction(id);
        if (result) {
            return R.OK("删除成功");
        } else {
            return R.FAIL("删除失败");
        }
    }

    /**
     * 批量删除交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_DELETE + "')")
    @PostMapping("/batch-delete")
    public R<String> batchDelete(@Valid @RequestBody BatchDeleteTranRequest request) {
        boolean result = tranService.batchDeleteTransactions(request.getIds());
        if (result) {
            return R.OK("批量删除成功");
        } else {
            return R.FAIL("批量删除失败");
        }
    }

    /**
     * 重新提交交易（审批拒绝后）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_RESUBMIT + "')")
    @PutMapping("/resubmit/{id}")
    public R<Boolean> resubmit(@PathVariable Integer id) {
        boolean result = tranService.resubmitTransaction(id);
        return R.OK(result);
    }

    /**
     * 记录收款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_PAYMENT + "')")
    @PostMapping("/payment")
    public R<TPayment> recordPayment(@Valid @RequestBody CreatePaymentRequest request) {
        TPayment payment = new TPayment();
        payment.setTranId(request.getTranId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentType(request.getPaymentType());
        payment.setTransactionRef(request.getTransactionRef());
        payment.setRemark(request.getRemark());
        return R.OK(tranService.recordPayment(payment));
    }

    /**
     * 确认或退回收款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_PAYMENT_CONFIRM + "')")
    @PutMapping("/payment/{id}/confirm")
    public R<TPayment> confirmPayment(
            @PathVariable Integer id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return R.OK(tranService.confirmPayment(id, request.getApproved(), request.getComment()));
    }

    /**
     * 获取交易收款记录
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/payment/{tranId}")
    public R<List<TPayment>> getPayments(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionPayments(tranId));
    }

    /**
     * 查询交易退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/refund-requests/{tranId}")
    public R<List<TRefundRequest>> getRefundRequests(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionRefundRequests(tranId));
    }

    /**
     * 创建退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND + "')")
    @PostMapping("/payment/{id}/refund-requests")
    public R<TRefundRequest> createRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody CreateRefundRequest request) {
        TRefundRequest refundRequest = new TRefundRequest();
        refundRequest.setRefundType(request.getRefundType());
        refundRequest.setAmount(request.getAmount());
        refundRequest.setReason(request.getReason());
        return R.OK(tranService.createRefundRequest(id, refundRequest));
    }

    /**
     * 审批退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND_APPROVE + "')")
    @PutMapping("/refund-requests/{id}/approve")
    public R<TRefundRequest> approveRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveRefundRequest request) {
        return R.OK(tranService.approveRefundRequest(id, request.getApproved(), request.getComment()));
    }

    /**
     * 执行退款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND_EXECUTE + "')")
    @PostMapping("/refund-requests/{id}/execute")
    public R<TPayment> executeRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody ExecuteRefundRequest request) {
        return R.OK(tranService.executeRefundRequest(id, request.getTransactionRef(), request.getRemark()));
    }

    /**
     * 旧退款接口保留兼容，不再直接执行退款。
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND + "')")
    @PostMapping("/payment/{id}/refund")
    public R<TPayment> refund(@PathVariable Integer id) {
        return R.OK(tranService.refundPayment(id));
    }
}
