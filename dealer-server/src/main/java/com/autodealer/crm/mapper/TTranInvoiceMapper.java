package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TTranInvoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

@Mapper
public interface TTranInvoiceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranInvoice record);

    int insertSelective(TTranInvoice record);

    TTranInvoice selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranInvoice record);

    int updateByPrimaryKey(TTranInvoice record);

    int updateStatusIfCurrent(@Param("id") Integer id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("newStatus") String newStatus,
                              @Param("issueTime") Date issueTime,
                              @Param("remark") String remark,
                              @Param("editTime") Date editTime,
                              @Param("editBy") Integer editBy);
    
    /**
     * 根据交易ID查询发票列表
     */
    List<TTranInvoice> selectByTranId(Integer tranId);

    /**
     * 根据交易ID删除所有发票
     */
    int deleteByTranId(Integer tranId);
} 
