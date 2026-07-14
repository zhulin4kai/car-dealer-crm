package com.autodealer.crm.modules.sales.testdrive.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CancelTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CheckInTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CompleteTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CreateTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.RescheduleTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDrive;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDriveStatusHistory;
import com.autodealer.crm.modules.sales.testdrive.application.api.query.TestDriveQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.testdrive.application.api.TestDriveService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test-drives")
public class TestDriveController {

    private final TestDriveService testDriveService;

    public TestDriveController(TestDriveService testDriveService) {
        this.testDriveService = testDriveService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_LIST + "')")
    public Result<PageInfo<TTestDrive>> list(TestDriveQuery query) {
        return Result.OK(testDriveService.getTestDrivePage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CREATE + "')")
    public Result<TTestDrive> create(@Valid @RequestBody CreateTestDriveRequest request) {
        return Result.OK(testDriveService.createTestDrive(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_VIEW + "')")
    public Result<TTestDrive> detail(@PathVariable Long id) {
        return Result.OK(testDriveService.getTestDrive(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_VIEW + "')")
    public Result<List<TTestDriveStatusHistory>> history(@PathVariable Long id) {
        return Result.OK(testDriveService.getHistory(id));
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_RESCHEDULE + "')")
    public Result<TTestDrive> reschedule(@PathVariable Long id,
                                    @Valid @RequestBody RescheduleTestDriveRequest request) {
        return Result.OK(testDriveService.reschedule(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CANCEL + "')")
    public Result<TTestDrive> cancel(@PathVariable Long id,
                                @Valid @RequestBody CancelTestDriveRequest request) {
        return Result.OK(testDriveService.cancel(id, request));
    }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CANCEL + "')")
    public Result<TTestDrive> noShow(@PathVariable Long id,
                                @Valid @RequestBody CancelTestDriveRequest request) {
        return Result.OK(testDriveService.markNoShow(id, request));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CHECK_IN + "')")
    public Result<TTestDrive> checkIn(@PathVariable Long id,
                                 @Valid @RequestBody CheckInTestDriveRequest request) {
        return Result.OK(testDriveService.checkIn(id, request));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_COMPLETE + "')")
    public Result<TTestDrive> complete(@PathVariable Long id,
                                  @Valid @RequestBody CompleteTestDriveRequest request) {
        return Result.OK(testDriveService.complete(id, request));
    }
}
