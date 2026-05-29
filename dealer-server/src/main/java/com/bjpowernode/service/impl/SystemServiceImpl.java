package com.bjpowernode.service.impl;

import com.bjpowernode.constant.Constants;
import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.TSystemMapper;
import com.bjpowernode.model.TSystem;
import com.bjpowernode.service.SystemService;
import com.bjpowernode.util.CacheUtils;
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
    private RedisManager redisManager;    @Override
    public List<TSystem> getAllList() {
        // 暂时禁用Redis缓存，直接从数据库获取，避免LocalDateTime序列化问题
        return systemMapper.selectAll();
    }    @Override
    public TSystem getById(Integer id) {
        // 暂时禁用Redis缓存，直接从数据库获取，避免LocalDateTime序列化问题
        return systemMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(TSystem system) {
        system.setCreateTime(LocalDateTime.now());
        systemMapper.insert(system);
        clearCache();
    }

    @Override
    @Transactional
    public void update(Integer id, TSystem system) {
        system.setId(id);
        system.setEditTime(LocalDateTime.now());
        systemMapper.update(system);
        clearCache();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        systemMapper.deleteById(id);
        clearCache();
    }

    @Override
    @Transactional
    public void batchDelete(List<Integer> ids) {
        systemMapper.batchDelete(ids);
        clearCache();
    }

    @Override
    @Transactional
    public void toggleStatus(Integer id, String isOpen) {
        systemMapper.updateStatus(id, isOpen);
        clearCache();
    }    private void clearCache() {
        // 暂时禁用缓存清理，因为我们没有使用Redis缓存
        // redisManager.delete(Constants.REDIS_SYSTEM_LIST_KEY);
        // redisManager.deletePattern(Constants.REDIS_SYSTEM_DETAIL_KEY + "*");
    }
}
