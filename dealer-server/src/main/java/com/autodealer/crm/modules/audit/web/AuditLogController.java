package com.autodealer.crm.modules.audit.web;

import com.autodealer.crm.shared.pagination.BaseQuery;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.audit.persistence.model.TLoginLog;
import com.autodealer.crm.modules.audit.persistence.model.TOperationLog;
import com.autodealer.crm.modules.audit.application.api.query.AuditLoginLogQuery;
import com.autodealer.crm.modules.audit.application.api.query.AuditOperationLogQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.audit.application.api.AuditLogService;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@RestController
public class AuditLogController {

    private static final String CSV_CONTENT_TYPE = "text/csv;charset=UTF-8";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final AuditLogService auditLogService;
    private final OperationAuditRecorder auditRecorder;

    public AuditLogController(AuditLogService auditLogService,
                              OperationAuditRecorder auditRecorder) {
        this.auditLogService = auditLogService;
        this.auditRecorder = auditRecorder;
    }

    @GetMapping("/api/audit/login-logs")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_LOGIN_LIST + "')")
    public Result<PageInfo<TLoginLog>> listLoginLogs(@RequestParam(value = "page", required = false) Integer page,
                                                @RequestParam(value = "size", required = false) Integer size,
                                                AuditLoginLogQuery query) {
        applyPagination(query, page, size);
        return Result.OK(auditLogService.listLoginLogs(query));
    }

    @GetMapping("/api/audit/login-logs/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_LOGIN_DETAIL + "')")
    public Result<TLoginLog> getLoginLog(@PathVariable Integer id) {
        return Result.OK(auditLogService.getLoginLog(id));
    }

    @GetMapping("/api/audit/login-logs/export")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_LOGIN_EXPORT + "')")
    public void exportLoginLogs(AuditLoginLogQuery query, HttpServletResponse response) throws IOException {
        List<TLoginLog> rows = auditLogService.exportLoginLogs(query);
        setCsvResponseHeaders(response, "登录记录");
        try (PrintWriter writer = response.getWriter()) {
            writer.print('\uFEFF');
            writer.println("ID,登录账号,用户ID,用户姓名,结果,原因编码,原因说明,IP,浏览器,操作系统,请求ID,登录时间");
            for (TLoginLog row : rows) {
                writer.println(csvLine(row.getId(), row.getLoginAct(), row.getUserId(), row.getUserName(),
                        row.getResult(), row.getReasonCode(), row.getReasonMessage(), row.getIp(),
                        row.getBrowser(), row.getOs(), row.getRequestId(), formatDate(row.getCreateTime())));
            }
        }
        auditRecorder.recordQuietly(AuditActionEnum.AUDIT_LOGIN_EXPORT, "export",
                "SUCCESS", "{\"count\":" + rows.size() + "}");
    }

    @GetMapping("/api/audit/operation-logs")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_OPERATION_LIST + "')")
    public Result<PageInfo<TOperationLog>> listOperationLogs(@RequestParam(value = "page", required = false) Integer page,
                                                        @RequestParam(value = "size", required = false) Integer size,
                                                        AuditOperationLogQuery query) {
        applyPagination(query, page, size);
        return Result.OK(auditLogService.listOperationLogs(query));
    }

    @GetMapping("/api/audit/operation-logs/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_OPERATION_DETAIL + "')")
    public Result<TOperationLog> getOperationLog(@PathVariable Integer id) {
        return Result.OK(auditLogService.getOperationLog(id));
    }

    @GetMapping("/api/audit/operation-logs/export")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_OPERATION_EXPORT + "')")
    public void exportOperationLogs(AuditOperationLogQuery query, HttpServletResponse response) throws IOException {
        List<TOperationLog> rows = auditLogService.exportOperationLogs(query);
        setCsvResponseHeaders(response, "操作记录");
        try (PrintWriter writer = response.getWriter()) {
            writer.print('\uFEFF');
            writer.println("ID,用户ID,用户姓名,动作编码,模块,对象类型,资源ID,结果,摘要,IP,请求ID,操作时间");
            for (TOperationLog row : rows) {
                writer.println(csvLine(row.getId(), row.getUserId(), row.getUserName(), row.getActionCode(),
                        row.getModuleName(), row.getObjectType(), row.getResourceId(), row.getResult(),
                        row.getDetail(), row.getIp(), row.getRequestId(), formatDate(row.getCreateTime())));
            }
        }
        auditRecorder.recordQuietly(AuditActionEnum.AUDIT_OPERATION_EXPORT, "export",
                "SUCCESS", "{\"count\":" + rows.size() + "}");
    }

    private void applyPagination(com.autodealer.crm.shared.pagination.BaseQuery query,
                                 Integer page,
                                 Integer size) {
        Integer resolvedPage = page;
        Integer resolvedSize = size;
        if (resolvedPage != null) {
            query.setCurrent(resolvedPage);
        }
        if (resolvedSize != null) {
            query.setPageSize(resolvedSize);
        }
    }

    private void setCsvResponseHeaders(HttpServletResponse response, String filePrefix) {
        response.setContentType(CSV_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(filePrefix + System.currentTimeMillis(),
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + ".csv\"; filename*=UTF-8''" + fileName + ".csv");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }

    private String csvLine(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csvCell(values[i]));
        }
        return sb.toString();
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String formatDate(Date value) {
        return value == null ? "" : DATE_FORMAT.format(value.toInstant().atZone(SYSTEM_ZONE));
    }
}
