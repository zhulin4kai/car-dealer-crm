package com.autodealer.crm.modules.sales.customer.application.api.port;

import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomerOwnerHistory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CustomerOwnerHistoryDataPort {

    int insert(TCustomerOwnerHistory history);

    List<TCustomerOwnerHistory> selectByCustomerId(@Param("customerId") Integer customerId);
}
