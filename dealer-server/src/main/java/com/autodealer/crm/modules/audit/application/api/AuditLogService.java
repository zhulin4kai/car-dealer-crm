package com.autodealer.crm.modules.audit.application.api;

import com.autodealer.crm.modules.audit.persistence.model.TLoginLog;
import com.autodealer.crm.modules.audit.persistence.model.TOperationLog;
import com.autodealer.crm.modules.audit.application.api.query.AuditLoginLogQuery;
import com.autodealer.crm.modules.audit.application.api.query.AuditOperationLogQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface AuditLogService {

    PageInfo<TLoginLog> listLoginLogs(AuditLoginLogQuery query);

    TLoginLog getLoginLog(Integer id);

    List<TLoginLog> exportLoginLogs(AuditLoginLogQuery query);

    PageInfo<TOperationLog> listOperationLogs(AuditOperationLogQuery query);

    TOperationLog getOperationLog(Integer id);

    List<TOperationLog> exportOperationLogs(AuditOperationLogQuery query);
}
