package com.autodealer.crm.modules.sales.testdrive.application.api;

import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CancelTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CheckInTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CompleteTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CreateTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.RescheduleTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDrive;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDriveStatusHistory;
import com.autodealer.crm.modules.sales.testdrive.application.api.query.TestDriveQuery;
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
