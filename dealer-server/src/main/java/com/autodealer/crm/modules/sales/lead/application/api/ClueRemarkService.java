package com.autodealer.crm.modules.sales.lead.application.api;

import com.autodealer.crm.modules.sales.lead.application.api.model.TClueRemark;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueRemarkQuery;
import com.github.pagehelper.PageInfo;

public interface ClueRemarkService {

    int saveClueRemark(ClueRemarkQuery clueRemarkQuery);

    PageInfo<TClueRemark> getClueRemarkByPage(Integer current, Integer pageSize, ClueRemarkQuery clueRemarkQuery);
}
