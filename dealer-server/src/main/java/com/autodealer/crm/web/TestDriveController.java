package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.CancelTestDriveRequest;
import com.autodealer.crm.dto.CheckInTestDriveRequest;
import com.autodealer.crm.dto.CompleteTestDriveRequest;
import com.autodealer.crm.dto.CreateTestDriveRequest;
import com.autodealer.crm.dto.RescheduleTestDriveRequest;
import com.autodealer.crm.model.TTestDrive;
import com.autodealer.crm.model.TTestDriveStatusHistory;
import com.autodealer.crm.query.TestDriveQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.TestDriveService;
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
    public R<PageInfo<TTestDrive>> list(TestDriveQuery query) {
        return R.OK(testDriveService.getTestDrivePage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CREATE + "')")
    public R<TTestDrive> create(@Valid @RequestBody CreateTestDriveRequest request) {
        return R.OK(testDriveService.createTestDrive(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_VIEW + "')")
    public R<TTestDrive> detail(@PathVariable Long id) {
        return R.OK(testDriveService.getTestDrive(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_VIEW + "')")
    public R<List<TTestDriveStatusHistory>> history(@PathVariable Long id) {
        return R.OK(testDriveService.getHistory(id));
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_RESCHEDULE + "')")
    public R<TTestDrive> reschedule(@PathVariable Long id,
                                    @Valid @RequestBody RescheduleTestDriveRequest request) {
        return R.OK(testDriveService.reschedule(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CANCEL + "')")
    public R<TTestDrive> cancel(@PathVariable Long id,
                                @Valid @RequestBody CancelTestDriveRequest request) {
        return R.OK(testDriveService.cancel(id, request));
    }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CANCEL + "')")
    public R<TTestDrive> noShow(@PathVariable Long id,
                                @Valid @RequestBody CancelTestDriveRequest request) {
        return R.OK(testDriveService.markNoShow(id, request));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_CHECK_IN + "')")
    public R<TTestDrive> checkIn(@PathVariable Long id,
                                 @Valid @RequestBody CheckInTestDriveRequest request) {
        return R.OK(testDriveService.checkIn(id, request));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('" + PermissionCodes.TEST_DRIVE_COMPLETE + "')")
    public R<TTestDrive> complete(@PathVariable Long id,
                                  @Valid @RequestBody CompleteTestDriveRequest request) {
        return R.OK(testDriveService.complete(id, request));
    }
}
