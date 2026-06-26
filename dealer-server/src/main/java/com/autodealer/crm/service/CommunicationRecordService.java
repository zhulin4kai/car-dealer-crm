package com.autodealer.crm.service;

import com.autodealer.crm.dto.CorrectCommunicationRecordRequest;
import com.autodealer.crm.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.dto.VoidCommunicationRecordRequest;
import com.autodealer.crm.model.TCommunicationRecord;
import com.autodealer.crm.query.CommunicationRecordQuery;
import com.github.pagehelper.PageInfo;

public interface CommunicationRecordService {
    PageInfo<TCommunicationRecord> getCommunicationRecordPage(CommunicationRecordQuery query);

    TCommunicationRecord createCommunicationRecord(CreateCommunicationRecordRequest request);

    TCommunicationRecord correctCommunicationRecord(Long id, CorrectCommunicationRecordRequest request);

    TCommunicationRecord voidCommunicationRecord(Long id, VoidCommunicationRecordRequest request);
}
