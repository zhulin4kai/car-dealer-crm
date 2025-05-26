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
import java.util.Map;

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
    @PostMapping("/create")
    public R<Integer> create(@RequestBody TranCreateRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        TTran tran = new TTran();
        tran.setCustomerId(request.getCustomerId());
        tran.setMoney(request.getAmount());
        tran.setDescription(request.getDescription());
        
        // 处理预计交付日期
        if (request.getExpectedDeliveryDate() != null && !request.getExpectedDeliveryDate().isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                tran.setExpectedDate(sdf.parse(request.getExpectedDeliveryDate()));
            } catch (Exception e) {
                return R.FAIL("日期格式错误");
            }
        }
        
        tran.setStage(41); // 初始状态：待报价
        tran.setCreateBy(currentUser.getId()); // 设置创建人
        
        // 从产品详情列表创建交易产品关联
        List<TTranProduct> products = request.getProducts().stream()
            .map(productDetail -> {
                TTranProduct tranProduct = new TTranProduct();
                tranProduct.setProductId(productDetail.getProductId());
                tranProduct.setQuantity(productDetail.getQuantity());
                tranProduct.setPrice(productDetail.getPrice());
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
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody TranCreateRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        if (request.getId() == null) {
            return R.FAIL("交易ID不能为空");
        }
        
        TTran tran = new TTran();
        tran.setId(request.getId());
        tran.setCustomerId(request.getCustomerId());
        tran.setMoney(request.getAmount());
        tran.setDescription(request.getDescription());
        
        // 处理预计交付日期
        if (request.getExpectedDeliveryDate() != null && !request.getExpectedDeliveryDate().isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                tran.setExpectedDate(sdf.parse(request.getExpectedDeliveryDate()));
            } catch (Exception e) {
                return R.FAIL("日期格式错误");
            }
        }
        
        tran.setEditBy(currentUser.getId());
        
        // 更新交易基本信息
        boolean result = tranService.updateTransaction(tran);
        
        if (result && request.getProducts() != null && !request.getProducts().isEmpty()) {
            // 更新产品信息（简单实现：先删除再插入）
            tranService.deleteTransactionProducts(request.getId());
            
            List<TTranProduct> products = request.getProducts().stream()
                .map(productDetail -> {
                    TTranProduct tranProduct = new TTranProduct();
                    tranProduct.setTranId(request.getId());
                    tranProduct.setProductId(productDetail.getProductId());
                    tranProduct.setQuantity(productDetail.getQuantity());
                    tranProduct.setPrice(productDetail.getPrice());
                    tranProduct.setCreateTime(new Date());
                    tranProduct.setCreateBy(currentUser.getId());
                    return tranProduct;
                })
                .toList();
                
            tranService.addTransactionProducts(request.getId(), products);
        }
        
        return R.OK(result);
    }

    /**
     * 结算交易 - 计算总金额并更新状态为待审批
     */
    @PutMapping("/settle/{id}")
    public R<Boolean> settle(@PathVariable Integer id) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        // 获取交易产品列表并计算总金额
        List<TTranProduct> products = tranService.getTransactionProducts(id);
        if (products == null || products.isEmpty()) {
            return R.FAIL("该交易没有产品信息，无法结算");
        }
        
        // 计算总金额
        BigDecimal totalAmount = products.stream()
            .map(product -> product.getPrice().multiply(new BigDecimal(product.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 更新交易金额和状态
        TTran tran = new TTran();
        tran.setId(id);
        tran.setMoney(totalAmount);
        tran.setStage(42); // 待审批状态
        tran.setEditBy(currentUser.getId());
        
        boolean result = tranService.updateTransaction(tran);
        return R.OK(result);
    }

    /**
     * 审批交易
     */
    @PutMapping("/approve/{id}")
    public R<Boolean> approve(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> approveData) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        Boolean approved = (Boolean) approveData.get("approved");
        String comment = (String) approveData.get("comment");
        
        if (approved == null || comment == null || comment.trim().isEmpty()) {
            return R.FAIL("审批结果和审批意见不能为空");
        }
        
        boolean result = tranService.approveTran(id, approved, comment, currentUser.getId());
        return R.OK(result);
    }

    /**
     * 获取交易审批信息
     */
    @GetMapping("/approve/info/{tranId}")
    public R<TTranApprove> getApproveInfo(@PathVariable Integer tranId) {
        TTranApprove approve = tranService.getTranApprove(tranId);
        return R.OK(approve);
    }

    /**
     * 创建发票
     */
    @PostMapping("/invoice")
    public R<Boolean> createInvoice(@RequestBody TTranInvoice invoice) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        invoice.setCreateBy(currentUser.getId());
        invoice.setUpdateBy(currentUser.getId());
        
        boolean result = tranService.createTranInvoice(invoice);
        return R.OK(result);
    }

    /**
     * 获取交易发票列表
     */
    @GetMapping("/invoice/{tranId}")
    public R<List<TTranInvoice>> getInvoiceList(@PathVariable Integer tranId) {
        List<TTranInvoice> invoices = tranService.getTranInvoices(tranId);
        return R.OK(invoices);
    }

    /**
     * 更新发票状态
     */
    @PutMapping("/invoice/{invoiceId}/status")
    public R<Boolean> updateInvoiceStatus(
            @PathVariable Integer invoiceId,
            @RequestBody Map<String, String> statusData) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        String status = statusData.get("status");
        if (status == null || status.trim().isEmpty()) {
            return R.FAIL("状态不能为空");
        }
        
        boolean result = tranService.updateTranInvoiceStatus(invoiceId, status, currentUser.getId());
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

    /**
     * 获取交易产品详情列表
     */
    @GetMapping("/products/{id}")
    public R<List<TTranProduct>> getTransactionProducts(@PathVariable Integer id) {
        return R.OK(tranService.getTransactionProductDetails(id));
    }
} 