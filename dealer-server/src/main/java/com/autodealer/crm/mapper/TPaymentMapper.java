package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

@Mapper
public interface TPaymentMapper {
    int insert(TPayment record);

    int insertSelective(TPayment record);

    TPayment selectByPrimaryKey(Integer id);

    List<TPayment> selectByTranId(@Param("tranId") Integer tranId);

    int updateByPrimaryKeySelective(TPayment record);

    int markRefundedIfCompleted(@Param("id") Integer id,
                                @Param("editTime") Date editTime,
                                @Param("editBy") Integer editBy);

    int deleteByPrimaryKey(Integer id);

    int deleteByTranId(@Param("tranId") Integer tranId);
}
