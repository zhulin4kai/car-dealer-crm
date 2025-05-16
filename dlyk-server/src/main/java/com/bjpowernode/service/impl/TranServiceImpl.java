package com.bjpowernode.service.impl;

import com.bjpowernode.constant.Constants;
import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.*;
import com.bjpowernode.model.*;
import com.bjpowernode.query.TranQuery;
import com.bjpowernode.service.TranService;
import com.bjpowernode.util.CacheUtils;
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
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisManager redisManager;

    @Override
    public PageInfo<TTran> getTransactionList(TranQuery query, Integer pageNum, Integer pageSize) {
        String cacheKey = Constants.CACHE_KEY_TRAN_LIST + CacheUtils.generateKey(query, pageNum, pageSize);
        PageInfo<TTran> pageInfo = redisManager.get(cacheKey);
        if (pageInfo != null) {
            return pageInfo;
        }
        
        PageHelper.startPage(pageNum, pageSize);
        List<TTran> tTranList = tranMapper.selectByQuery(query);
        pageInfo = new PageInfo<>(tTranList);
        
        redisManager.set(cacheKey, pageInfo, Constants.CACHE_EXPIRE_TIME);
        return pageInfo;
    }

    @Override
    public TTran getTransactionById(Integer id) {
        String cacheKey = Constants.CACHE_KEY_TRAN + id;
        TTran tTran = redisManager.get(cacheKey);
        if (tTran != null) {
            return tTran;
        }
        
        tTran = tranMapper.selectByPrimaryKey(id);
        if (tTran != null) {
            redisManager.set(cacheKey, tTran, Constants.CACHE_EXPIRE_TIME);
        }
        return tTran;
    }

    @Override
    public Integer createTransaction(TTran tTran, List<TTranProduct> products) {
        // 设置创建时间等
        Date now = new Date();
        tTran.setCreateTime(now);
        tTran.setTranNo(generateTranNo()); // 生成交易编号
        
        // 插入交易记录
        tranMapper.insertSelective(tTran);
        Integer tranId = tTran.getId();
        
        // 插入产品关联
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                product.setCreateTime(now);
                tranProductMapper.insertSelective(product);
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