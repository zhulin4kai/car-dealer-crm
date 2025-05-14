package com.bjpowernode.service.impl;

import com.bjpowernode.mapper.ProductStockRecordMapper;
import com.bjpowernode.model.ProductStockRecord;
import com.bjpowernode.service.ProductStockRecordService;
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