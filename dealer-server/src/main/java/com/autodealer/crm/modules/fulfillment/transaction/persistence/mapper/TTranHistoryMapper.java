package com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper;

import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TTranHistoryMapper {
    int insert(TTranHistory record);

    List<TTranHistory> selectByTranId(@Param("tranId") Integer tranId);
}
