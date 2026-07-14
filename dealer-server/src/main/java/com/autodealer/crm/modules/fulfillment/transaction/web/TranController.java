package com.autodealer.crm.modules.fulfillment.transaction.web;

import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotion;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.dto.CreateTranInvoiceRequest;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.dto.RedReverseInvoiceRequest;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.dto.ReissueInvoiceRequest;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.dto.UpdateInvoiceStatusRequest;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.model.TTranInvoice;
import com.autodealer.crm.modules.fulfillment.payment.application.api.dto.ApproveRefundRequest;
import com.autodealer.crm.modules.fulfillment.payment.application.api.dto.ConfirmPaymentRequest;
import com.autodealer.crm.modules.fulfillment.payment.application.api.dto.CreatePaymentRequest;
import com.autodealer.crm.modules.fulfillment.payment.application.api.dto.CreateRefundRequest;
import com.autodealer.crm.modules.fulfillment.payment.application.api.dto.ExecuteRefundRequest;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TPayment;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TRefundRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.ApproveTranRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.CreateTranRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.SettleRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.SettlementPreviewRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.SettlementPreviewResponse;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.TransactionLifecycleRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.UpdateTranRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranApprove;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranProduct;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranRemark;
import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.identity.application.api.dto.*;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.query.TranQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.TranService;
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
    public Result<PageInfo<TTran>> list(
            TranQuery query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.OK(tranService.getTransactionList(query, page, size));
    }

    /**
     * 获取交易详情
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/{id}")
    public Result<TTran> detail(@PathVariable Integer id) {
        return Result.OK(tranService.getTransactionById(id));
    }

    /**
     * 创建交易 — 商品价格由服务端从数据库查询，不接受客户端传入
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CREATE + "')")
    @PostMapping("/api/transactions")
    public Result<Integer> create(@Valid @RequestBody CreateTranRequest request) {
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

        return Result.OK(tranService.createTransaction(tran, products));
    }

    /**
     * 更新交易 — 商品价格由服务端从数据库查询，不接受客户端传入
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_EDIT + "')")
    @PutMapping("/api/tran/update")
    public Result<Boolean> update(@Valid @RequestBody UpdateTranRequest request) {
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
        return Result.OK(result);
    }

    /**
     * 结算预览（只读）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @PostMapping("/api/tran/{id}/settlement-preview")
    public Result<SettlementPreviewResponse> settlementPreview(
            @PathVariable Integer id,
            @Valid @RequestBody SettlementPreviewRequest request) {
        SettlementPreviewResponse preview = tranService.getSettlementPreview(id, request.getPromotionId());
        return Result.OK(preview);
    }

    /**
     * 获取交易当前可用于结算的促销活动。
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @GetMapping("/api/tran/{id}/available-promotions")
    public Result<List<TProductPromotion>> availablePromotions(@PathVariable Integer id) {
        return Result.OK(tranService.getAvailablePromotions(id));
    }

    /**
     * 结算交易（带 CAS 版本控制）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_SETTLE + "')")
    @PutMapping("/api/tran/{id}/settle")
    public Result<SettlementPreviewResponse> settle(
            @PathVariable Integer id,
            @Valid @RequestBody SettleRequest request) {
        return Result.OK(tranService.settleTransaction(id, request));
    }

    /**
     * 审批交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_APPROVE + "')")
    @PutMapping("/api/tran/approve/{id}")
    public Result<Boolean> approve(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveTranRequest request) {
        boolean result = tranService.approveTran(id, request.getApproved(), request.getComment());
        return Result.OK(result);
    }

    /**
     * 获取交易审批信息
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/approve/info/{tranId}")
    public Result<TTranApprove> getApproveInfo(@PathVariable Integer tranId) {
        TTranApprove approve = tranService.getTranApprove(tranId);
        return Result.OK(approve);
    }

    /**
     * 创建发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/api/tran/invoice")
    public Result<Boolean> createInvoice(@Valid @RequestBody CreateTranInvoiceRequest request) {
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
        return Result.OK(result);
    }

    /**
     * 获取交易发票列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/invoice/{tranId}")
    public Result<List<TTranInvoice>> getInvoiceList(@PathVariable Integer tranId) {
        List<TTranInvoice> invoices = tranService.getTranInvoices(tranId);
        return Result.OK(invoices);
    }

    /**
     * 更新发票状态
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PutMapping("/api/tran/invoice/{invoiceId}/status")
    public Result<Boolean> updateInvoiceStatus(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody UpdateInvoiceStatusRequest request) {
        boolean result = tranService.updateTranInvoiceStatus(invoiceId, request.getStatus(), request.getReason());
        return Result.OK(result);
    }

    /**
     * 红冲发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/api/tran/invoice/{invoiceId}/red-reversal")
    public Result<TTranInvoice> redReverseInvoice(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody RedReverseInvoiceRequest request) {
        TTranInvoice invoice = tranService.redReverseInvoice(invoiceId, request.getAmount(), request.getReason());
        return Result.OK(invoice);
    }

    /**
     * 重开发票
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_INVOICE + "')")
    @PostMapping("/api/tran/invoice/{invoiceId}/reissue")
    public Result<TTranInvoice> reissueInvoice(
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
        return Result.OK(created);
    }

    /**
     * 获取交易备注
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/remarks/{tranId}")
    public Result<List<TTranRemark>> remarks(@PathVariable Integer tranId) {
        return Result.OK(tranService.getTransactionRemarks(tranId));
    }

    /**
     * 获取交易产品详情列表
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/products/{id}")
    public Result<List<TTranProduct>> getTransactionProducts(@PathVariable Integer id) {
        return Result.OK(tranService.getTransactionProductDetails(id));
    }

    /**
     * 取消交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CANCEL + "')")
    @PutMapping("/api/tran/{id}/cancel")
    public Result<Boolean> cancel(
            @PathVariable Integer id,
            @Valid @RequestBody TransactionLifecycleRequest request) {
        return Result.OK(tranService.cancelTransaction(id, request.getReason()));
    }

    /**
     * 关闭交易
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_CLOSE + "')")
    @PutMapping("/api/tran/{id}/close")
    public Result<Boolean> close(
            @PathVariable Integer id,
            @Valid @RequestBody TransactionLifecycleRequest request) {
        return Result.OK(tranService.closeTransaction(id, request.getReason()));
    }

    /**
     * 重新提交交易（审批拒绝后）
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_RESUBMIT + "')")
    @PutMapping("/api/tran/resubmit/{id}")
    public Result<Boolean> resubmit(@PathVariable Integer id) {
        boolean result = tranService.resubmitTransaction(id);
        return Result.OK(result);
    }

    /**
     * 记录收款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_PAYMENT + "')")
    @PostMapping("/api/tran/payment")
    public Result<TPayment> recordPayment(@Valid @RequestBody CreatePaymentRequest request) {
        TPayment payment = new TPayment();
        payment.setTranId(request.getTranId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentType(request.getPaymentType());
        payment.setTransactionRef(request.getTransactionRef());
        payment.setRemark(request.getRemark());
        return Result.OK(tranService.recordPayment(payment));
    }

    /**
     * 确认或退回收款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_PAYMENT_CONFIRM + "')")
    @PutMapping("/api/tran/payment/{id}/confirm")
    public Result<TPayment> confirmPayment(
            @PathVariable Integer id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return Result.OK(tranService.confirmPayment(id, request.getApproved(), request.getComment()));
    }

    /**
     * 获取交易收款记录
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/payment/{tranId}")
    public Result<List<TPayment>> getPayments(@PathVariable Integer tranId) {
        return Result.OK(tranService.getTransactionPayments(tranId));
    }

    /**
     * 查询交易退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_VIEW + "')")
    @GetMapping("/api/tran/refund-requests/{tranId}")
    public Result<List<TRefundRequest>> getRefundRequests(@PathVariable Integer tranId) {
        return Result.OK(tranService.getTransactionRefundRequests(tranId));
    }

    /**
     * 创建退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND + "')")
    @PostMapping("/api/tran/payment/{id}/refund-requests")
    public Result<TRefundRequest> createRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody CreateRefundRequest request) {
        TRefundRequest refundRequest = new TRefundRequest();
        refundRequest.setRefundType(request.getRefundType());
        refundRequest.setAmount(request.getAmount());
        refundRequest.setReason(request.getReason());
        return Result.OK(tranService.createRefundRequest(id, refundRequest));
    }

    /**
     * 审批退款申请
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND_APPROVE + "')")
    @PutMapping("/api/tran/refund-requests/{id}/approve")
    public Result<TRefundRequest> approveRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveRefundRequest request) {
        return Result.OK(tranService.approveRefundRequest(id, request.getApproved(), request.getComment()));
    }

    /**
     * 执行退款
     */
    @PreAuthorize("hasAuthority('" + PermissionCodes.TRAN_REFUND_EXECUTE + "')")
    @PostMapping("/api/tran/refund-requests/{id}/execute")
    public Result<TRefundRequest> executeRefundRequest(
            @PathVariable Integer id,
            @Valid @RequestBody ExecuteRefundRequest request) {
        return Result.OK(tranService.executeRefundRequest(id, request.getTransactionRef(), request.getRemark(),
                request.getSuccess(), request.getFailureReason()));
    }

}
