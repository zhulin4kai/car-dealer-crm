package com.autodealer.crm.modules.sales.followup.application.api;

import com.autodealer.crm.modules.sales.followup.application.api.dto.CorrectCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.VoidCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.model.TCommunicationRecord;
import com.autodealer.crm.modules.sales.followup.application.api.query.CommunicationRecordQuery;
import com.github.pagehelper.PageInfo;

public interface CommunicationRecordService {
    PageInfo<TCommunicationRecord> getCommunicationRecordPage(CommunicationRecordQuery query);

    TCommunicationRecord createCommunicationRecord(CreateCommunicationRecordRequest request);

    TCommunicationRecord correctCommunicationRecord(Long id, CorrectCommunicationRecordRequest request);

    TCommunicationRecord voidCommunicationRecord(Long id, VoidCommunicationRecordRequest request);
}
