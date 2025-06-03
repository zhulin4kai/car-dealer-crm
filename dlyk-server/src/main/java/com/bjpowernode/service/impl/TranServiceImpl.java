package com.bjpowernode.service.impl;

import com.bjpowernode.constant.Constants;
import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.*;
import com.bjpowernode.model.*;
import com.bjpowernode.query.TranQuery;
import com.bjpowernode.service.TranService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
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
    private TTranHistoryMapper tranHistoryMapper;

    @Resource
    private TTranRemarkMapper tranRemarkMapper;

    @Resource
    private TTranProductMapper tranProductMapper;

    @Resource
    private TTranProductionMapper tranProductionMapper;

    @Resource
    private TTranInvoiceMapper tranInvoiceMapper;

    @Resource
    private TTranApproveMapper tranApproveMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
                
                // 减少产品库存
                productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
            }
        }
        
        // 清除缓存
        clearTransactionCache(tranId);
        
        return tranId;
    }

    @Override
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
    public boolean updateTransactionStage(Integer id, Integer stage) {
        TTran tTran = new TTran();
        tTran.setId(id);
        tTran.setStage(stage);
        tTran.setEditTime(new Date());
        
        int result = tranMapper.updateByPrimaryKeySelective(tTran);
        if (result > 0) {
            // 创建历史记录
            TTran current = tranMapper.selectByPrimaryKey(id);
            TTranHistory history = new TTranHistory();
            history.setTranId(id);
            history.setStage(stage);
            history.setMoney(current.getMoney());
            history.setExpectedDate(current.getExpectedDate());
            history.setCreateTime(new Date());
            history.setCreateBy(current.getEditBy());
            tranHistoryMapper.insert(history);
            
            // 清除相关缓存
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
    public TTranProduction getProductionStatus(Integer tranProductId) {
        String cacheKey = Constants.CACHE_KEY_TRAN_PRODUCTION + tranProductId;
        TTranProduction production = redisManager.get(cacheKey);
        if (production != null) {
            return production;
        }
        
        production = tranProductionMapper.selectByTranProductId(tranProductId);
        if (production != null) {
            redisManager.set(cacheKey, production, Constants.CACHE_EXPIRE_TIME);
        }
        return production;
    }

    @Override
    public boolean updateProductionStatus(TTranProduction production) {
        production.setUpdateTime(new Date());
        int rows = tranProductionMapper.updateByPrimaryKeySelective(production);
        
        if (rows > 0) {
            redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTION + production.getTranProductId());
            return true;
        }
        return false;
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
    public List<TTranHistory> getTransactionHistory(Integer tranId) {
        return tranHistoryMapper.selectByTranId(tranId);
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
        try {
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
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTransactionProducts(Integer tranId, List<TTranProduct> products) {
        try {
            if (products != null && !products.isEmpty()) {
                for (TTranProduct product : products) {
                    product.setTranId(tranId);
                    tranProductMapper.insertSelective(product);
                    
                    // 减少产品库存
                    productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
                }
            }
            // 清除缓存
            redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveTran(Integer tranId, Boolean approved, String comment, Integer approveBy) {
        try {
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
                    tran.setStage(43); // 已审批
                } else {
                    tran.setStage(21); // 丢失关闭
                }
                tran.setEditTime(now);
                tran.setEditBy(approveBy);
                
                int tranResult = tranMapper.updateByPrimaryKeySelective(tran);
                
                if (tranResult > 0) {
                    // 3. 创建历史记录
                    TTran current = tranMapper.selectByPrimaryKey(tranId);
                    TTranHistory history = new TTranHistory();
                    history.setTranId(tranId);
                    history.setStage(tran.getStage());
                    history.setMoney(current.getMoney());
                    history.setExpectedDate(current.getExpectedDate());
                    history.setCreateTime(now);
                    history.setCreateBy(approveBy);
                    tranHistoryMapper.insert(history);
                    
                    // 清除缓存
                    clearTransactionCache(tranId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public TTranApprove getTranApprove(Integer tranId) {
        return tranApproveMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTranInvoice(TTranInvoice invoice) {
        try {
            Date now = new Date();
            
            // 生成发票号码
            invoice.setInvoiceNo(generateInvoiceNo());
            invoice.setStatus("PENDING"); // 待开具
            invoice.setCreateTime(now);
            invoice.setUpdateTime(now);
            
            int result = tranInvoiceMapper.insertSelective(invoice);
            
            if (result > 0) {
                // 更新交易状态为待收款
                TTran tran = new TTran();
                tran.setId(invoice.getTranId());
                tran.setStage(45); // 待收款
                tran.setEditTime(now);
                tran.setEditBy(invoice.getCreateBy());
                
                int tranResult = tranMapper.updateByPrimaryKeySelective(tran);
                
                if (tranResult > 0) {
                    // 创建历史记录
                    TTran current = tranMapper.selectByPrimaryKey(invoice.getTranId());
                    TTranHistory history = new TTranHistory();
                    history.setTranId(invoice.getTranId());
                    history.setStage(45);
                    history.setMoney(current.getMoney());
                    history.setExpectedDate(current.getExpectedDate());
                    history.setCreateTime(now);
                    history.setCreateBy(invoice.getCreateBy());
                    tranHistoryMapper.insert(history);
                    
                    // 清除缓存
                    clearTransactionCache(invoice.getTranId());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<TTranInvoice> getTranInvoices(Integer tranId) {
        return tranInvoiceMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTranInvoiceStatus(Integer invoiceId, String status, Integer updateBy) {
        try {
            Date now = new Date();
            
            TTranInvoice invoice = new TTranInvoice();
            invoice.setId(invoiceId);
            invoice.setStatus(status);
            invoice.setUpdateTime(now);
            invoice.setUpdateBy(updateBy);
            
            if ("ISSUED".equals(status)) {
                invoice.setIssueTime(now);
            }
            
            int result = tranInvoiceMapper.updateByPrimaryKeySelective(invoice);
            
            if (result > 0) {
                // 如果发票已开具，更新交易状态为已完成
                if ("ISSUED".equals(status)) {
                    TTranInvoice currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
                    if (currentInvoice != null) {
                        TTran tran = new TTran();
                        tran.setId(currentInvoice.getTranId());
                        tran.setStage(46); // 已完成
                        tran.setEditTime(now);
                        tran.setEditBy(updateBy);
                        
                        int tranResult = tranMapper.updateByPrimaryKeySelective(tran);
                        
                        if (tranResult > 0) {
                            // 创建历史记录
                            TTran current = tranMapper.selectByPrimaryKey(currentInvoice.getTranId());
                            TTranHistory history = new TTranHistory();
                            history.setTranId(currentInvoice.getTranId());
                            history.setStage(46);
                            history.setMoney(current.getMoney());
                            history.setExpectedDate(current.getExpectedDate());
                            history.setCreateTime(now);
                            history.setCreateBy(updateBy);
                            tranHistoryMapper.insert(history);
                            
                            // 清除缓存
                            clearTransactionCache(currentInvoice.getTranId());
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成交易编号
     * 格式：年月日 + 6位随机数
     */
    private String generateTranNo() {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomStr = String.format("%06d", new java.util.Random().nextInt(1000000));
        return dateStr + randomStr;
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
} 