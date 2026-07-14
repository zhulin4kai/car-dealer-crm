package com.autodealer.crm.modules.sales.lead.application.api.port;

import com.autodealer.crm.modules.sales.lead.application.api.model.TClueOwnerHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LeadOwnerHistoryDataPort {

    int insert(TClueOwnerHistory record);

    List<TClueOwnerHistory> selectByClueId(@Param("clueId") Integer clueId);
}
