package com.autodealer.crm.service;

import com.autodealer.crm.dto.CancelTestDriveRequest;
import com.autodealer.crm.dto.CheckInTestDriveRequest;
import com.autodealer.crm.dto.CompleteTestDriveRequest;
import com.autodealer.crm.dto.CreateTestDriveRequest;
import com.autodealer.crm.dto.RescheduleTestDriveRequest;
import com.autodealer.crm.model.TTestDrive;
import com.autodealer.crm.model.TTestDriveStatusHistory;
import com.autodealer.crm.query.TestDriveQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface TestDriveService {
    PageInfo<TTestDrive> getTestDrivePage(TestDriveQuery query);

    TTestDrive createTestDrive(CreateTestDriveRequest request);

    TTestDrive getTestDrive(Long id);

    List<TTestDriveStatusHistory> getHistory(Long id);

    TTestDrive reschedule(Long id, RescheduleTestDriveRequest request);

    TTestDrive cancel(Long id, CancelTestDriveRequest request);

    TTestDrive markNoShow(Long id, CancelTestDriveRequest request);

    TTestDrive checkIn(Long id, CheckInTestDriveRequest request);

    TTestDrive complete(Long id, CompleteTestDriveRequest request);
}
