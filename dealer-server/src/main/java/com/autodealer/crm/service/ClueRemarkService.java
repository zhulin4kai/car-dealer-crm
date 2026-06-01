package com.autodealer.crm.service;

import com.autodealer.crm.model.TClueRemark;
import com.autodealer.crm.query.ClueRemarkQuery;
import com.github.pagehelper.PageInfo;

public interface ClueRemarkService {

    int saveClueRemark(ClueRemarkQuery clueRemarkQuery);

    PageInfo<TClueRemark> getClueRemarkByPage(Integer current, ClueRemarkQuery clueRemarkQuery);
}
