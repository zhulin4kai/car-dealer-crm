package com.autodealer.crm.service;

import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.TranQuery;
import com.github.pagehelper.PageInfo;
import java.util.List;

/**
 * 交易管理服务接口
 */
public interface TranService {
    /**
     * 分页查询交易列表
     * @param query 查询条件
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageInfo<TTran> getTransactionList(TranQuery query, Integer pageNum, Integer pageSize);
    
    /**
     * 获取交易详情
     * @param id 交易ID
     * @return 交易详情
     */
    TTran getTransactionById(Integer id);
    
    /**
     * 创建新交易
     * @param transaction 交易信息
     * @param products 交易产品列表
     * @return 创建的交易ID
     */
    Integer createTransaction(TTran transaction, List<TTranProduct> products);
    
    /**
     * 更新交易信息
     * @param transaction 交易信息
     * @return 是否更新成功
     */
    boolean updateTransaction(TTran transaction);

    boolean settleTransaction(Integer tranId, java.math.BigDecimal amount);
    
    /**
     * 添加交易跟踪记录
     * @param remark 跟踪记录
     * @return 是否添加成功
     */
    boolean addTransactionRemark(TTranRemark remark);
    
    /**
     * 获取交易的产品列表
     * @param tranId 交易ID
     * @return 产品列表
     */
    List<TTranProduct> getTransactionProducts(Integer tranId);
    
    /**
     * 获取交易产品详情列表（包含产品名称）
     * @param tranId 交易ID
     * @return 交易产品详情列表
     */
    List<TTranProduct> getTransactionProductDetails(Integer tranId);

    
    /**
     * 创建发票
     * @param invoice 发票信息
     * @return 是否创建成功
     */
    boolean createInvoice(TTranInvoice invoice);
    
    /**
     * 获取交易的发票列表
     * @param tranId 交易ID
     * @return 发票列表
     */
    List<TTranInvoice> getTransactionInvoices(Integer tranId);
    
    /**
     * 更新发票状态
     * @param id 发票ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateInvoiceStatus(Integer id, String status);
    
    /**
     * 获取交易的跟踪记录
     * @param tranId 交易ID
     * @return 跟踪记录列表
     */
    List<TTranRemark> getTransactionRemarks(Integer tranId);
    
    /**
     * 删除交易的所有产品
     * @param tranId 交易ID
     * @return 是否删除成功
     */
    boolean deleteTransactionProducts(Integer tranId);
    
    /**
     * 添加交易产品
     * @param tranId 交易ID
     * @param products 产品列表
     * @return 是否添加成功
     */
    boolean addTransactionProducts(Integer tranId, List<TTranProduct> products);
    
    /**
     * 审批交易
     * @param tranId 交易ID
     * @param approved 审批结果
     * @param comment 审批意见
     * @param approveBy 审批人
     * @return 是否审批成功
     */
    boolean approveTran(Integer tranId, Boolean approved, String comment);
    
    /**
     * 获取交易审批信息
     * @param tranId 交易ID
     * @return 审批信息
     */
    TTranApprove getTranApprove(Integer tranId);
    
    /**
     * 创建发票
     * @param invoice 发票信息
     * @return 是否创建成功
     */
    boolean createTranInvoice(TTranInvoice invoice);
    
    /**
     * 获取交易发票列表
     * @param tranId 交易ID
     * @return 发票列表
     */
    List<TTranInvoice> getTranInvoices(Integer tranId);
      /**
     * 更新发票状态
     * @param invoiceId 发票ID
     * @param status 新状态
     * @param updateBy 更新人
     * @return 是否更新成功
     */
    boolean updateTranInvoiceStatus(Integer invoiceId, String status);
    
    /**
     * 删除交易
     * @param id 交易ID
     * @return 是否删除成功
     */
    boolean deleteTransaction(Integer id);
    
    /**
     * 批量删除交易
     * @param ids 交易ID列表
     * @return 是否删除成功
     */
    boolean batchDeleteTransactions(List<Integer> ids);

    /**
     * 重新提交交易（审批拒绝后）
     * @param tranId 交易ID
     * @param userId 操作人ID
     * @return 是否重新提交成功
     */
    boolean resubmitTransaction(Integer tranId);

    /**
     * 更新交易及产品信息（在同一事务中）
     * @param tran 交易信息
     * @param products 产品列表
     * @return 是否更新成功
     */
    boolean updateTransactionWithProducts(TTran tran, List<TTranProduct> products);

    /**
     * 记录收款
     */
    TPayment recordPayment(TPayment payment);

    /**
     * 退款（交易取消，恢复库存）
     */
    TPayment refundPayment(Integer paymentId);

    /**
     * 获取交易收款记录
     */
    List<TPayment> getTransactionPayments(Integer tranId);
}
