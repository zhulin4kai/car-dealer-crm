package com.bjpowernode.service;

import com.bjpowernode.model.TClue;
import com.bjpowernode.query.ClueQuery;
import com.github.pagehelper.PageInfo;

import java.io.InputStream;
import java.util.List;

public interface ClueService {

    PageInfo<TClue> getClueByPage(Integer current);

    void importExcel(InputStream inputStreamm, String token);

    Boolean checkPhone(String phone);

    int saveClue(ClueQuery clueQuery);

    TClue getClueById(Integer id);

    int updateClue(ClueQuery clueQuery);

    int batchDelClueByIds(List<Integer> ids);
}
