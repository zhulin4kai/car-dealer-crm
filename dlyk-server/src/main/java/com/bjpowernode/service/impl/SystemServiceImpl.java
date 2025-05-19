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
    private RedisManager redisManager;

    @Override
    public List<TSystem> getAllList() {
        return CacheUtils.getCacheData(
            () -> redisManager.get(Constants.REDIS_SYSTEM_LIST_KEY),
            () -> systemMapper.selectAll(),
            data -> redisManager.set(Constants.REDIS_SYSTEM_LIST_KEY, data, Constants.SYSTEM_CACHE_EXPIRE_TIME)
        );
    }

    @Override
    public TSystem getById(Integer id) {
        String cacheKey = Constants.REDIS_SYSTEM_DETAIL_KEY + id;
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> systemMapper.selectById(id),
            data -> redisManager.set(cacheKey, data, Constants.SYSTEM_CACHE_EXPIRE_TIME)
        );
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
    }

    private void clearCache() {
        redisManager.delete(Constants.REDIS_SYSTEM_LIST_KEY);
        redisManager.deletePattern(Constants.REDIS_SYSTEM_DETAIL_KEY + "*");
    }
}
