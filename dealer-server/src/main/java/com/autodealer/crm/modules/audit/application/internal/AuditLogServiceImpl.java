package com.autodealer.crm.modules.audit.application.internal;

import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.audit.persistence.mapper.TLoginLogMapper;
import com.autodealer.crm.modules.audit.persistence.mapper.TOperationLogMapper;
import com.autodealer.crm.modules.audit.persistence.model.TLoginLog;
import com.autodealer.crm.modules.audit.persistence.model.TOperationLog;
import com.autodealer.crm.modules.audit.application.api.query.AuditLoginLogQuery;
import com.autodealer.crm.modules.audit.application.api.query.AuditOperationLogQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.audit.application.api.AuditLogService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10000;

    private final TLoginLogMapper loginLogMapper;
    private final TOperationLogMapper operationLogMapper;

    public AuditLogServiceImpl(TLoginLogMapper loginLogMapper,
                               TOperationLogMapper operationLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public PageInfo<TLoginLog> listLoginLogs(AuditLoginLogQuery query) {
        AuditLoginLogQuery actualQuery = query == null ? new AuditLoginLogQuery() : query;
        PageHelper.startPage(normalizePage(actualQuery.getCurrent()), normalizePageSize(actualQuery.getPageSize()));
        return new PageInfo<>(loginLogMapper.selectByQuery(actualQuery));
    }

    @Override
    public TLoginLog getLoginLog(Integer id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "登录记录 ID 不能为空");
        }
        TLoginLog log = loginLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "登录记录不存在");
        }
        return log;
    }

    @Override
    public List<TLoginLog> exportLoginLogs(AuditLoginLogQuery query) {
        return loginLogMapper.selectForExport(query == null ? new AuditLoginLogQuery() : query, EXPORT_LIMIT);
    }

    @Override
    public PageInfo<TOperationLog> listOperationLogs(AuditOperationLogQuery query) {
        AuditOperationLogQuery actualQuery = query == null ? new AuditOperationLogQuery() : query;
        PageHelper.startPage(normalizePage(actualQuery.getCurrent()), normalizePageSize(actualQuery.getPageSize()));
        return new PageInfo<>(operationLogMapper.selectByQuery(actualQuery));
    }

    @Override
    public TOperationLog getOperationLog(Integer id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "操作记录 ID 不能为空");
        }
        TOperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "操作记录不存在");
        }
        return log;
    }

    @Override
    public List<TOperationLog> exportOperationLogs(AuditOperationLogQuery query) {
        return operationLogMapper.selectForExport(query == null ? new AuditOperationLogQuery() : query, EXPORT_LIMIT);
    }

    private int normalizePage(Integer value) {
        return value == null || value < 1 ? DEFAULT_PAGE : value;
    }

    private int normalizePageSize(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        if (value > MAX_PAGE_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过 " + MAX_PAGE_SIZE);
        }
        return value;
    }
}
