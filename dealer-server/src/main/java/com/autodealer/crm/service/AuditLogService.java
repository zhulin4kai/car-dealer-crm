package com.autodealer.crm.service;

import com.autodealer.crm.model.TLoginLog;
import com.autodealer.crm.model.TOperationLog;
import com.autodealer.crm.query.AuditLoginLogQuery;
import com.autodealer.crm.query.AuditOperationLogQuery;
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
