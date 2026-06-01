package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TTranInvoice;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TTranInvoiceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranInvoice record);

    int insertSelective(TTranInvoice record);

    TTranInvoice selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranInvoice record);

    int updateByPrimaryKey(TTranInvoice record);
    
    /**
     * 根据交易ID查询发票列表
     */
    List<TTranInvoice> selectByTranId(Integer tranId);
} 