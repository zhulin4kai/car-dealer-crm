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
public class TranController {

    @Resource
    private TranService tranService;

    /**
     * 获取交易列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_LIST + "')")
    @GetMapping("/api/tran/list")
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
    @GetMapping("/api/tran/{id}")
    public R<TTran> detail(@PathVariable Integer id) {
        return R.OK(tranService.getTransactionById(id));
    }

    /**
     * 创建交易 — 商品价格由服务端从数据库查询，不接受客户端传入
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CREATE + "')")
    @PostMapping("/api/transactions")
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
    @PutMapping("/api/tran/update")
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
    @PostMapping("/api/tran/{id}/settlement-preview")
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
    @GetMapping("/api/tran/{id}/available-promotions")
    public R<List<TProductPromotion>> availablePromotions(@PathVariable Integer id) {
        return R.OK(tranService.getAvailablePromotions(id));
    }

    /**
     * 结算交易（带 CAS 版本控制）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @PutMapping("/api/tran/{id}/settle")
    public R<SettlementPreviewResponse> settle(
            @PathVariable Integer id,
            @Valid @RequestBody SettleRequest request) {
        return R.OK(tranService.settleTransaction(id, request));
    }

    /**
     * 审批交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_APPROVE + "')")
    @PutMapping("/api/tran/approve/{id}")
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
    @GetMapping("/api/tran/approve/info/{tranId}")
    public R<TTranApprove> getApproveInfo(@PathVariable Integer tranId) {
        TTranApprove approve = tranService.getTranApprove(tranId);
        return R.OK(approve);
    }

    /**
     * 创建发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/api/tran/invoice")
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
    @GetMapping("/api/tran/invoice/{tranId}")
    public R<List<TTranInvoice>> getInvoiceList(@PathVariable Integer tranId) {
        List<TTranInvoice> invoices = tranService.getTranInvoices(tranId);
        return R.OK(invoices);
    }

    /**
     * 更新发票状态
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PutMapping("/api/tran/invoice/{invoiceId}/status")
    public R<Boolean> updateInvoiceStatus(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody UpdateInvoiceStatusRequest request) {
        boolean result = tranService.updateTranInvoiceStatus(invoiceId, request.getStatus(), request.getReason());
        return R.OK(result);
    }

    /**
     * 红冲发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/api/tran/invoice/{invoiceId}/red-reversal")
    public R<TTranInvoice> redReverseInvoice(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody RedReverseInvoiceRequest request) {
        TTranInvoice invoice = tranService.redReverseInvoice(invoiceId, request.getAmount(), request.getReason());
        return R.OK(invoice);
    }

    /**
     * 重开发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/api/tran/invoice/{invoiceId}/reissue")
    public R<TTranInvoice> reissueInvoice(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody ReissueInvoiceRequest request) {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setAmount(request.getAmount());
        invoice.setType(request.getType());
        invoice.setTitle(request.getTitle());
        invoice.setTaxNumber(request.getTaxNumber());
        invoice.setBankName(request.getBankName());
        invoice.setBankAccount(request.getBankAccount());
        invoice.setAddress(request.getAddress());
        invoice.setPhone(request.getPhone());
        TTranInvoice created = tranService.reissueInvoice(invoiceId, invoice, request.getReason());
        return R.OK(created);
    }

    /**
     * 获取交易备注
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/remarks/{tranId}")
    public R<List<TTranRemark>> remarks(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionRemarks(tranId));
    }

    /**
     * 获取交易产品详情列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/products/{id}")
    public R<List<TTranProduct>> getTransactionProducts(@PathVariable Integer id) {
        return R.OK(tranService.getTransactionProductDetails(id));
    }

    /**
     * 取消交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CANCEL + "')")
    @PutMapping("/api/tran/{id}/cancel")
    public R<Boolean> cancel(
            @PathVariable Integer id,
            @Valid @RequestBody TransactionLifecycleRequest request) {
        return R.OK(tranService.cancelTransaction(id, request.getReason()));
    }

    /**
     * 关闭交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CLOSE + "')")
    @PutMapping("/api/tran/{id}/close")
    public R<Boolean> close(
            @PathVariable Integer id,
            @Valid @RequestBody TransactionLifecycleRequest request) {
        return R.OK(tranService.closeTransaction(id, request.getReason()));
    }

    /**
     * 重新提交交易（审批拒绝后）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_RESUBMIT + "')")
    @PutMapping("/api/tran/resubmit/{id}")
    public R<Boolean> resubmit(@PathVariable Integer id) {
        boolean result = tranService.resubmitTransaction(id);
        return R.OK(result);
    }

    /**
     * 记录收款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_PAYMENT + "')")
    @PostMapping("/api/tran/payment")
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
    @PutMapping("/api/tran/payment/{id}/confirm")
    public R<TPayment> confirmPayment(
            @PathVariable Integer id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return R.OK(tranService.confirmPayment(id, request.getApproved(), request.getComment()));
    }

    /**
     * 获取交易收款记录
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/payment/{tranId}")
    public R<List<TPayment>> getPayments(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionPayments(tranId));
    }

    /**
     * 查询交易退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/refund-requests/{tranId}")
    public R<List<TRefundRequest>> getRefundRequests(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionRefundRequests(tranId));
    }

    /**
     * 创建退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND + "')")
    @PostMapping("/api/tran/payment/{id}/refund-requests")
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
    @PutMapping("/api/tran/refund-requests/{id}/approve")
    public R<TRefundRequest> approveRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveRefundRequest request) {
        return R.OK(tranService.approveRefundRequest(id, request.getApproved(), request.getComment()));
    }

    /**
     * 执行退款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND_EXECUTE + "')")
    @PostMapping("/api/tran/refund-requests/{id}/execute")
    public R<TRefundRequest> executeRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody ExecuteRefundRequest request) {
        return R.OK(tranService.executeRefundRequest(id, request.getTransactionRef(), request.getRemark(),
                request.getSuccess(), request.getFailureReason()));
    }

}
