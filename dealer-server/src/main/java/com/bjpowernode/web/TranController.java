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
        
        // 从产品详情列表创建交易产品关联
        List<TTranProduct> products = null;
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            products = request.getProducts().stream()
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
        }
        
        // 使用统一事务方法更新交易和产品
        boolean result = tranService.updateTransactionWithProducts(tran, products);
        return R.OK(result);
    }

    /**
     * 结算交易 - 计算总金额并更新状态为待审批
     * 支持前端传递促销后的结算金额
     */
    @PutMapping("/settle/{id}")
    public R<Boolean> settle(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> body) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        // 获取交易产品列表并计算总金额
        List<TTranProduct> products = tranService.getTransactionProducts(id);
        if (products == null || products.isEmpty()) {
            return R.FAIL("该交易没有产品信息，无法结算");
        }
        
        BigDecimal totalAmount;
        
        // 检查是否有前端传递的促销后金额
        if (body != null && body.containsKey("amount")) {
            try {
                Object amountObj = body.get("amount");
                if (amountObj instanceof Number) {
                    totalAmount = new BigDecimal(amountObj.toString());
                } else if (amountObj instanceof String) {
                    totalAmount = new BigDecimal((String) amountObj);
                } else {
                    return R.FAIL("传递的金额格式不正确");
                }
                
                // 验证金额不能为负数
                if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                    return R.FAIL("结算金额不能为负数");
                }
            } catch (NumberFormatException e) {
                return R.FAIL("传递的金额格式不正确");
            }
        } else {
            // 使用原有逻辑计算总金额（产品原价合计）
            totalAmount = products.stream()
                .map(product -> product.getPrice().multiply(new BigDecimal(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
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

    /**
     * 删除交易
     */
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
    @PostMapping("/batch-delete")
    public R<String> batchDelete(@RequestBody Map<String, List<Integer>> request) {
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return R.FAIL("请选择要删除的交易");
        }
        
        boolean result = tranService.batchDeleteTransactions(ids);
        if (result) {
            return R.OK("批量删除成功");
        } else {
            return R.FAIL("批量删除失败");
        }
    }

    /**
     * 重新提交交易（审批拒绝后）
     */
    @PutMapping("/resubmit/{id}")
    public R<Boolean> resubmit(@PathVariable Integer id) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TUser currentUser = (TUser) authentication.getPrincipal();
        
        boolean result = tranService.resubmitTransaction(id, currentUser.getId());
        return R.OK(result);
    }
}
