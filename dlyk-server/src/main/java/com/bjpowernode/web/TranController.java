package com.bjpowernode.web;

import com.bjpowernode.model.*;
import com.bjpowernode.query.TranQuery;
import com.bjpowernode.result.R;
import com.bjpowernode.service.TranService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    @GetMapping("/{id}")
    public R<TTran> detail(@PathVariable Integer id) {
        return R.OK(tranService.getTransactionById(id));
    }

    /**
     * 创建交易
     */
    @PostMapping
    public R<Integer> create(@RequestBody TranCreateRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        TTran tran = new TTran();
        tran.setMoney(request.getAmount());
        tran.setDescription(request.getDescription());
        tran.setExpectedDate(request.getExpectedDeliveryDate());
        tran.setStage(12); // 初始状态：01创建交易
        tran.setCreateBy(currentUser.getId()); // 设置创建人
        
        // 从产品ID列表创建交易产品关联
        List<TTranProduct> products = request.getProducts().stream()
            .map(productId -> {
                TTranProduct tranProduct = new TTranProduct();
                tranProduct.setProductId(productId);
                tranProduct.setQuantity(1); // 默认数量为1
                tranProduct.setPrice(request.getAmount().divide(BigDecimal.valueOf(request.getProducts().size()))); // 平均分配金额
                tranProduct.setCreateTime(new Date());
                tranProduct.setCreateBy(currentUser.getId());
                return tranProduct;
            })
            .toList();
            
        return R.OK(tranService.createTransaction(tran, products));
    }

    /**
     * 更新交易
     */
    @PutMapping
    public R<Boolean> update(@RequestBody TTran tran) {
        return R.OK(tranService.updateTransaction(tran));
    }

    /**
     * 更新交易阶段
     */
    @PutMapping("/approve/{id}")
    public R<Boolean> approve(
            @PathVariable Integer id,
            @RequestParam Boolean approved,
            @RequestParam String comment) {
        // 根据审批结果设置不同的阶段
        Integer stage = approved ? 37 : 21; // 37-02确认清单, 21-06丢失关闭
        boolean result = tranService.updateTransactionStage(id, stage);
        if (result) {
            // 添加审批备注
            TTranRemark remark = new TTranRemark();
            remark.setTranId(id);
            remark.setNoteContent(comment);
            tranService.addTransactionRemark(remark);
        }
        return R.OK(result);
    }

    /**
     * 获取生产状态
     */
    @GetMapping("/production/{tranId}")
    public R<List<TTranProduction>> productionStatus(@PathVariable Integer tranId) {
        List<TTranProduct> products = tranService.getTransactionProducts(tranId);
        List<TTranProduction> productionList = products.stream()
                .map(product -> tranService.getProductionStatus(product.getId()))
                .toList();
        return R.OK(productionList);
    }

    /**
     * 更新生产状态
     */
    @PutMapping("/production")
    public R<Boolean> updateProduction(@RequestBody TTranProduction production) {
        return R.OK(tranService.updateProductionStatus(production));
    }

    /**
     * 获取发票列表
     */
    @GetMapping("/invoice/{tranId}")
    public R<List<TTranInvoice>> invoiceList(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionInvoices(tranId));
    }

    /**
     * 创建发票
     */
    @PostMapping("/invoice")
    public R<Boolean> createInvoice(@RequestBody TTranInvoice invoice) {
        return R.OK(tranService.createInvoice(invoice));
    }

    /**
     * 更新发票状态
     */
    @PutMapping("/invoice/{id}/{status}")
    public R<Boolean> updateInvoiceStatus(
            @PathVariable Integer id,
            @PathVariable String status) {
        return R.OK(tranService.updateInvoiceStatus(id, status));
    }

    /**
     * 获取交易历史记录
     */
    @GetMapping("/history/{tranId}")
    public R<List<TTranHistory>> history(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionHistory(tranId));
    }

    /**
     * 获取交易备注
     */
    @GetMapping("/remarks/{tranId}")
    public R<List<TTranRemark>> remarks(@PathVariable Integer tranId) {
        return R.OK(tranService.getTransactionRemarks(tranId));
    }
} 