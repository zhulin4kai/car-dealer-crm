package com.autodealer.crm.modules.ai.application.internal.tool.executor;

import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutor;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;
import com.autodealer.crm.modules.ai.application.internal.AiSensitiveDataSanitizer;
import com.autodealer.crm.modules.ai.application.internal.tool.AiToolArgumentBinder;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDrive;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.testdrive.application.api.TestDriveService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetTestDriveDetailToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_test_drive_detail", "查询当前用户可见的试驾安排和结果摘要",
            PermissionCodes.TEST_DRIVE_VIEW, ToolRiskLevel.READONLY,
            true, false, 1, "AI_TOOL_GET_TEST_DRIVE_DETAIL");

    private final TestDriveService testDriveService;
    private final AiToolArgumentBinder argumentBinder;
    private final AiSensitiveDataSanitizer sanitizer;

    public GetTestDriveDetailToolExecutor(TestDriveService testDriveService,
                                          AiToolArgumentBinder argumentBinder,
                                          AiSensitiveDataSanitizer sanitizer) {
        this.testDriveService = testDriveService;
        this.argumentBinder = argumentBinder;
        this.sanitizer = sanitizer;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetTestDriveDetailRequest request = argumentBinder.bind(
                arguments, AiToolDtos.GetTestDriveDetailRequest.class);
        // VIN 和签名类证据不进入模型上下文，联系方式只保留掩码。
        TTestDrive drive = testDriveService.getTestDrive(request.getTestDriveId());
        if (drive == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "试驾记录不存在");
        }
        AiToolDtos.TestDriveDetail data = new AiToolDtos.TestDriveDetail(
                drive.getTestDriveNo(), drive.getCustomerName(), drive.getOpportunityNo(),
                drive.getVehicleName(), drive.getOwnerName(), drive.getPlannedStartTime(),
                drive.getPlannedEndTime(), drive.getActualArriveTime(), drive.getActualStartTime(),
                drive.getActualEndTime(), drive.getStatus(),
                sanitizer.sanitizeDisplayText(drive.getContactName(), 64),
                sanitizer.sanitize(drive.getContactPhone(), 32),
                sanitizer.sanitizeDisplayText(drive.getResult(), 500),
                sanitizer.sanitizeDisplayText(drive.getCustomerFeedback(), 500),
                sanitizer.sanitizeDisplayText(drive.getNextAction(), 500), drive.getCancelType(),
                sanitizer.sanitizeDisplayText(drive.getCancelReason(), 500),
                sanitizer.sanitizeDisplayText(drive.getRemark(), 500));
        return ToolExecutionResult.of(data, "返回试驾详情", "TEST_DRIVE:" + drive.getId());
    }
}
