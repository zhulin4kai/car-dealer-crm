package com.autodealer.crm.modules.commerce.inventory.application.internal;

import com.autodealer.crm.modules.commerce.inventory.persistence.mapper.TProductStockRecordMapper;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductStockRecord;
import com.autodealer.crm.modules.commerce.inventory.application.api.ProductStockRecordService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductStockRecordServiceImpl implements ProductStockRecordService {

    @Autowired
    private TProductStockRecordMapper stockRecordMapper;

    @Override
    public PageInfo<TProductStockRecord> getStockRecordsByProductId(Long productId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProductStockRecord> records = stockRecordMapper.selectByProductId(productId, (pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(records);
    }
}
