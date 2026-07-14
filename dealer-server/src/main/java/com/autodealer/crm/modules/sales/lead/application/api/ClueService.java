package com.autodealer.crm.modules.sales.lead.application.api;

import com.autodealer.crm.modules.sales.lead.application.api.dto.ImportResult;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ClueLifecycleRequest;
import com.autodealer.crm.modules.sales.lead.application.api.dto.TransferClueOwnerRequest;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClueOwnerHistory;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueQuery;
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

    boolean transferOwner(Integer id, TransferClueOwnerRequest request);

    List<TClueOwnerHistory> getOwnerHistory(Integer id);

    boolean closeClue(Integer id, ClueLifecycleRequest request);

    boolean restoreClue(Integer id, ClueLifecycleRequest request);
}
