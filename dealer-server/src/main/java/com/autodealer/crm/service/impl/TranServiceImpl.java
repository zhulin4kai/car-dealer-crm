package com.autodealer.crm.service.impl;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.TranQuery;
import com.autodealer.crm.service.TranService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 交易管理服务实现类
 */
@Service
public class TranServiceImpl implements TranService {

    @Resource
    private TTranMapper tranMapper;

    @Resource
    private TTranRemarkMapper tranRemarkMapper;

    @Resource
    private TTranProductMapper tranProductMapper;

    @Resource
    private TTranInvoiceMapper tranInvoiceMapper;

    @Resource
    private TTranApproveMapper tranApproveMapper;

    @Resource
    private TProductMapper productMapper;

    @Resource
    private RedisManager redisManager;

    @Override
    public PageInfo<TTran> getTransactionList(TranQuery query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TTran> tTranList = tranMapper.selectByQuery(query);
        PageInfo<TTran> pageInfo = new PageInfo<>(tTranList);
        return pageInfo;
    }

    @Override
    public TTran getTransactionById(Integer id) {
        TTran tTran = tranMapper.selectByPrimaryKey(id);
        return tTran;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createTransaction(TTran tTran, List<TTranProduct> products) {
        // 设置创建时间等
        Date now = new Date();
        tTran.setCreateTime(now);
        tTran.setTranNo(generateTranNo()); // 生成交易编号
        
        // 插入交易记录
        tranMapper.insertSelective(tTran);
        Integer tranId = tTran.getId();
        
        // 插入产品关联并更新库存
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                product.setCreateTime(now);
                tranProductMapper.insertSelective(product);
                
                // 减少产品库存，检查库存是否充足
                int updateCount = productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new RuntimeException("产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
        }
        
        // 清除缓存
        clearTransactionCache(tranId);
        
        return tranId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransaction(TTran tTran) {
        // 设置更新时间
        tTran.setEditTime(new Date());
        
        // 更新交易记录
        int rows = tranMapper.updateByPrimaryKeySelective(tTran);
        
        if (rows > 0) {
            // 清除缓存
            clearTransactionCache(tTran.getId());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransactionStage(Integer id, TranStage stage) {
        TTran tTran = new TTran();
        tTran.setId(id);
        tTran.setStage(stage);
        tTran.setEditTime(new Date());
        
        int result = tranMapper.updateByPrimaryKeySelective(tTran);
        if (result > 0) {
            clearTransactionCache(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean addTransactionRemark(TTranRemark remark) {
        remark.setCreateTime(new Date());
        int result = tranRemarkMapper.insert(remark);
        if (result > 0) {
            clearTransactionCache(remark.getTranId());
            return true;
        }
        return false;
    }

    @Override
    public List<TTranProduct> getTransactionProducts(Integer tranId) {
        String cacheKey = Constants.CACHE_KEY_TRAN_PRODUCTS + tranId;
        List<TTranProduct> products = redisManager.get(cacheKey);
        if (products != null) {
            return products;
        }
        
        products = tranProductMapper.selectByTranId(tranId);
        if (products != null) {
            redisManager.set(cacheKey, products, Constants.CACHE_EXPIRE_TIME);
        }
        return products;
    }

    @Override
    public boolean createInvoice(TTranInvoice invoice) {
        invoice.setCreateTime(new Date());
        int rows = tranInvoiceMapper.insertSelective(invoice);
        
        if (rows > 0) {
            redisManager.delete(Constants.CACHE_KEY_TRAN_INVOICES + invoice.getTranId());
            return true;
        }
        return false;
    }

    @Override
    public List<TTranInvoice> getTransactionInvoices(Integer tranId) {
        String cacheKey = Constants.CACHE_KEY_TRAN_INVOICES + tranId;
        List<TTranInvoice> invoices = redisManager.get(cacheKey);
        if (invoices != null) {
            return invoices;
        }
        
        invoices = tranInvoiceMapper.selectByTranId(tranId);
        if (invoices != null) {
            redisManager.set(cacheKey, invoices, Constants.CACHE_EXPIRE_TIME);
        }
        return invoices;
    }

    @Override
    public boolean updateInvoiceStatus(Integer id, String status) {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(id);
        invoice.setStatus(status);
        
        int rows = tranInvoiceMapper.updateByPrimaryKeySelective(invoice);
        if (rows > 0) {
            TTranInvoice updatedInvoice = tranInvoiceMapper.selectByPrimaryKey(id);
            if (updatedInvoice != null) {
                redisManager.delete(Constants.CACHE_KEY_TRAN_INVOICES + updatedInvoice.getTranId());
            }
            return true;
        }
        return false;
    }


    @Override
    public List<TTranRemark> getTransactionRemarks(Integer tranId) {
        return tranRemarkMapper.selectByTranId(tranId);
    }

    @Override
    public List<TTranProduct> getTransactionProductDetails(Integer tranId) {
        return tranMapper.selectTranProductsByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTransactionProducts(Integer tranId) {
        // 在删除前先恢复库存
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                // 恢复产品库存
                productMapper.updateStock(product.getProductId().longValue(), product.getQuantity());
            }
        }

        tranProductMapper.deleteByTranId(tranId);
        // 清除缓存
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTransactionProducts(Integer tranId, List<TTranProduct> products) {
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                tranProductMapper.insertSelective(product);

                // 减少产品库存，检查库存是否充足
                int updateCount = productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new RuntimeException("产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
        }
        // 清除缓存
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveTran(Integer tranId, Boolean approved, String comment, Integer approveBy) {
        validateStageTransition(tranId, TranStage.PENDING);

        Date now = new Date();

        // 1. 创建审批记录
        TTranApprove approve = new TTranApprove();
        approve.setTranId(tranId);
        approve.setApproveResult(approved);
        approve.setApproveComment(comment);
        approve.setApproveTime(now);
        approve.setApproveBy(approveBy);
        approve.setCreateTime(now);
        approve.setCreateBy(approveBy);

        int approveResult = tranApproveMapper.insertSelective(approve);

        if (approveResult > 0) {
            // 2. 更新交易状态
            TTran tran = new TTran();
            tran.setId(tranId);
            if (approved) {
                tran.setStage(TranStage.APPROVED);
            } else {
                tran.setStage(TranStage.LOST);
            }
            tran.setEditTime(now);
            tran.setEditBy(approveBy);

            int tranResult = tranMapper.updateByPrimaryKeySelective(tran);

            if (tranResult > 0) {
                // 清除缓存
                clearTransactionCache(tranId);
                return true;
            }
        }
        return false;
    }

    @Override
    public TTranApprove getTranApprove(Integer tranId) {
        return tranApproveMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTranInvoice(TTranInvoice invoice) {
        validateStageTransition(invoice.getTranId(), TranStage.APPROVED);

        // 检查该交易是否已有发票
        List<TTranInvoice> existingInvoices = tranInvoiceMapper.selectByTranId(invoice.getTranId());
        if (existingInvoices != null && !existingInvoices.isEmpty()) {
            throw new RuntimeException("该交易已开具发票，不可重复开票");
        }

        // 校验发票金额是否等于交易金额
        TTran tran = tranMapper.selectByPrimaryKey(invoice.getTranId());
        if (tran == null) {
            throw new RuntimeException("交易记录不存在");
        }
        if (invoice.getAmount() != null && tran.getMoney() != null) {
            if (invoice.getAmount().compareTo(tran.getMoney()) != 0) {
                throw new RuntimeException("发票金额必须等于交易结算金额");
            }
        }

        Date now = new Date();

        // 生成发票号码
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setStatus("PENDING"); // 待开具
        invoice.setCreateTime(now);
        invoice.setEditTime(now);

        int result = tranInvoiceMapper.insertSelective(invoice);

        if (result > 0) {
            // 更新交易状态为待收款
            TTran updateTran = new TTran();
            updateTran.setId(invoice.getTranId());
            updateTran.setStage(TranStage.PAYMENT);
            updateTran.setEditTime(now);
            updateTran.setEditBy(invoice.getCreateBy());

            int tranResult = tranMapper.updateByPrimaryKeySelective(updateTran);

            if (tranResult > 0) {
                // 清除缓存
                clearTransactionCache(invoice.getTranId());
                return true;
            }
        }
        return false;
    }

    @Override
    public List<TTranInvoice> getTranInvoices(Integer tranId) {
        return tranInvoiceMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTranInvoiceStatus(Integer invoiceId, String status, Integer updateBy) {
        Date now = new Date();

        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(invoiceId);
        invoice.setStatus(status);
        invoice.setEditTime(now);
        invoice.setEditBy(updateBy);

        if ("ISSUED".equals(status)) {
            invoice.setIssueTime(now);
        }

        int result = tranInvoiceMapper.updateByPrimaryKeySelective(invoice);

        if (result > 0) {
            // 如果发票已开具，更新交易状态为已完成
            if ("ISSUED".equals(status)) {
                TTranInvoice currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
                if (currentInvoice != null) {
                    validateStageTransition(currentInvoice.getTranId(), TranStage.PAYMENT);

                    TTran tran = new TTran();
                    tran.setId(currentInvoice.getTranId());
                    tran.setStage(TranStage.COMPLETED);
                    tran.setEditTime(now);
                    tran.setEditBy(updateBy);

                    int tranResult = tranMapper.updateByPrimaryKeySelective(tran);

                    if (tranResult > 0) {
                        // 清除缓存
                        clearTransactionCache(currentInvoice.getTranId());
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 生成交易编号
     * 格式：年月日 + 6位随机数
     */
    private String generateTranNo() {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomStr = String.format("%06d", new java.util.Random().nextInt(1000000));
        return "TN" + dateStr + randomStr;
    }

    /**
     * 生成发票号码
     * 格式：INV + 年月日 + 6位随机数
     */
    private String generateInvoiceNo() {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomStr = String.format("%06d", new java.util.Random().nextInt(1000000));
        return "INV" + dateStr + randomStr;
    }

    /**
     * 清除交易相关的所有缓存
     */
    private void clearTransactionCache(Integer tranId) {
        redisManager.delete(Constants.CACHE_KEY_TRAN + tranId);
        redisManager.deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTION + tranId);
        redisManager.delete(Constants.CACHE_KEY_TRAN_INVOICES + tranId);
    }

    /**
     * 校验交易状态流转是否合法
     * @param tranId 交易ID
     * @param requiredStage 需要的状态
     */
    private void validateStageTransition(Integer tranId, TranStage requiredStage) {
        TTran tran = tranMapper.selectByPrimaryKey(tranId);
        if (tran == null) {
            throw new RuntimeException("交易记录不存在");
        }
        if (!requiredStage.equals(tran.getStage())) {
            throw new RuntimeException("当前交易状态不允许执行此操作，需要状态: " + requiredStage.getLabel());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTransaction(Integer id) {
        // 检查交易是否存在
        TTran transaction = tranMapper.selectByPrimaryKey(id);
        if (transaction == null) {
            return false;
        }

        // 校验只有待报价状态允许删除
        if (transaction.getStage() != TranStage.QUOTATION) {
            throw new RuntimeException("只有待报价状态的交易才能删除");
        }

        // 删除交易产品关联（恢复库存）
        List<TTranProduct> tranProducts = tranProductMapper.selectByTranId(id);
        if (tranProducts != null && !tranProducts.isEmpty()) {
            for (TTranProduct product : tranProducts) {
                // 恢复产品库存
                productMapper.updateStock(product.getProductId().longValue(), product.getQuantity());
            }
            // 删除交易产品关联
            tranProductMapper.deleteByTranId(id);
        }

        // 删除交易备注
        tranRemarkMapper.deleteByTranId(id);

        // 删除交易主记录
        int result = tranMapper.deleteByPrimaryKey(id);

        if (result > 0) {
            // 清除缓存
            clearTransactionCache(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteTransactions(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // 逐个删除交易（保证事务一致性）
        for (Integer id : ids) {
            // 删除交易产品关联（恢复库存）
            List<TTranProduct> tranProducts = tranProductMapper.selectByTranId(id);
            if (tranProducts != null && !tranProducts.isEmpty()) {
                for (TTranProduct product : tranProducts) {
                    // 恢复产品库存
                    productMapper.updateStock(product.getProductId().longValue(), product.getQuantity());
                }
                // 删除交易产品关联
                tranProductMapper.deleteByTranId(id);
            }

            // 删除交易备注
            tranRemarkMapper.deleteByTranId(id);

            // 清除缓存
            clearTransactionCache(id);
        }

        // 批量删除交易主记录
        int result = tranMapper.deleteByIds(ids);

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resubmitTransaction(Integer tranId, Integer userId) {
        validateStageTransition(tranId, TranStage.LOST);

        Date now = new Date();

        TTran tran = new TTran();
        tran.setId(tranId);
        tran.setStage(TranStage.QUOTATION);
        tran.setEditTime(now);
        tran.setEditBy(userId);

        int result = tranMapper.updateByPrimaryKeySelective(tran);

        if (result > 0) {
            // 清除缓存
            clearTransactionCache(tranId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransactionWithProducts(TTran tran, List<TTranProduct> products) {
        // 1. 更新交易基本信息
        tran.setEditTime(new Date());
        int rows = tranMapper.updateByPrimaryKeySelective(tran);
        if (rows == 0) {
            return false;
        }

        // 2. 恢复旧产品库存
        deleteTransactionProducts(tran.getId());

        // 3. 扣减新产品库存
        if (products != null && !products.isEmpty()) {
            addTransactionProducts(tran.getId(), products);
        }

        clearTransactionCache(tran.getId());
        return true;
    }
}
