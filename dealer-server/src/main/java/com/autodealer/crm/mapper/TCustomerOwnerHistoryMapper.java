package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TCustomerOwnerHistory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TCustomerOwnerHistoryMapper {

    int insert(TCustomerOwnerHistory history);

    List<TCustomerOwnerHistory> selectByCustomerId(@Param("customerId") Integer customerId);
}
