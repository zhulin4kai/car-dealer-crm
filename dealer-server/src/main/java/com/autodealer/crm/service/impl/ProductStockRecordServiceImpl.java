package com.autodealer.crm.service.impl;

import com.autodealer.crm.mapper.ProductStockRecordMapper;
import com.autodealer.crm.model.ProductStockRecord;
import com.autodealer.crm.service.ProductStockRecordService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductStockRecordServiceImpl implements ProductStockRecordService {

    @Autowired
    private ProductStockRecordMapper stockRecordMapper;
    
    @Override
    public PageInfo<ProductStockRecord> getStockRecordsByProductId(Long productId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductStockRecord> records = stockRecordMapper.selectByProductId(productId, (pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(records);
    }
} 