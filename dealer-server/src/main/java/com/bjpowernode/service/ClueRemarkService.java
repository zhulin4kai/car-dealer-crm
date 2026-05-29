package com.bjpowernode.service;

import com.bjpowernode.model.TClueRemark;
import com.bjpowernode.query.ClueRemarkQuery;
import com.github.pagehelper.PageInfo;

public interface ClueRemarkService {

    int saveClueRemark(ClueRemarkQuery clueRemarkQuery);

    PageInfo<TClueRemark> getClueRemarkByPage(Integer current, ClueRemarkQuery clueRemarkQuery);
}
