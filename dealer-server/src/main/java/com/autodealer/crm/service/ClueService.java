package com.autodealer.crm.service;

import com.autodealer.crm.dto.ImportResult;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.query.ClueQuery;
import com.github.pagehelper.PageInfo;

import java.io.InputStream;
import java.util.List;

public interface ClueService {

    PageInfo<TClue> getClueByPage(Integer current, Integer pageSize);

    ImportResult importExcel(InputStream inputStream);

    Boolean checkPhone(String phone);

    int saveClue(ClueQuery clueQuery);

    TClue getClueById(Integer id);

    int updateClue(ClueQuery clueQuery);

    int delClueById(Integer id);

    int batchDelClueByIds(List<Integer> ids);
}
