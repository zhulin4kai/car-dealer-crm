package com.bjpowernode.service;

import com.bjpowernode.model.ProductStockRecord;
import com.github.pagehelper.PageInfo;

public interface ProductStockRecordService {
    
    /**
     * 根据产品ID查询库存记录
     * @param productId 产品ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 库存记录分页结果
     */
    PageInfo<ProductStockRecord> getStockRecordsByProductId(Long productId, Integer pageNum, Integer pageSize);
    
} 