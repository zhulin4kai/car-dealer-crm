package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.mapper.TSystemMapper;
import com.autodealer.crm.model.TSystem;
import com.autodealer.crm.service.SystemService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemServiceImpl implements SystemService {

    @Resource
    private TSystemMapper systemMapper;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Override
    public List<TSystem> getAllList() {
        return systemMapper.selectAll();
    }

    @Override
    public TSystem getById(Integer id) {
        return systemMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(TSystem system) {
        system.setCreateTime(LocalDateTime.now());
        systemMapper.insert(system);
        auditRecorder.record(AuditActionEnum.SYSTEM_CONFIG_UPDATE, String.valueOf(system.getId()));
    }

    @Override
    @Transactional
    public void update(Integer id, TSystem system) {
        system.setId(id);
        system.setEditTime(LocalDateTime.now());
        systemMapper.update(system);
        auditRecorder.record(AuditActionEnum.SYSTEM_CONFIG_UPDATE, String.valueOf(id));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        systemMapper.deleteById(id);
        auditRecorder.record(AuditActionEnum.SYSTEM_CONFIG_UPDATE, String.valueOf(id));
    }

    @Override
    @Transactional
    public void batchDelete(List<Integer> ids) {
        systemMapper.batchDelete(ids);
        auditRecorder.record(AuditActionEnum.SYSTEM_CONFIG_UPDATE, ids.toString());
    }

    @Override
    @Transactional
    public void toggleStatus(Integer id, String isOpen) {
        systemMapper.updateStatus(id, isOpen);
        auditRecorder.record(AuditActionEnum.SYSTEM_CONFIG_UPDATE, String.valueOf(id));
    }
}
