package com.bjpowernode.service;

import com.bjpowernode.model.*;
import com.bjpowernode.query.TranQuery;
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
    
    /**
     * 更新交易阶段
     * @param id 交易ID
     * @param stage 新阶段
     * @return 是否更新成功
     */
    boolean updateTransactionStage(Integer id, Integer stage);
    
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
     * 获取交易产品的生产状态
     * @param tranProductId 交易产品ID
     * @return 生产状态
     */
    TTranProduction getProductionStatus(Integer tranProductId);
    
    /**
     * 更新生产状态
     * @param production 生产状态信息
     * @return 是否更新成功
     */
    boolean updateProductionStatus(TTranProduction production);
    
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
     * 获取交易的历史记录
     * @param tranId 交易ID
     * @return 历史记录列表
     */
    List<TTranHistory> getTransactionHistory(Integer tranId);
    
    /**
     * 获取交易的跟踪记录
     * @param tranId 交易ID
     * @return 跟踪记录列表
     */
    List<TTranRemark> getTransactionRemarks(Integer tranId);
} 