package com.bjpowernode.service.impl;

import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.DicMapper;
import com.bjpowernode.model.TDicType;
import com.bjpowernode.model.TDicValue;
import com.bjpowernode.query.DicQuery;
import com.bjpowernode.service.DicService;
import com.bjpowernode.util.CacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DicServiceImpl implements DicService {

    @Resource
    private DicMapper dicMapper;

    @Resource
    private RedisManager redisManager;

    private static final String CACHE_KEY_PREFIX = "dic:";
    private static final long CACHE_EXPIRE_SECONDS = 24 * 60 * 60; // 24 hours in seconds

    @Override
    public PageInfo<TDicType> getDicTypes(DicQuery query) {
        String cacheKey = CACHE_KEY_PREFIX + "types:" + query.toString();
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> {
                PageHelper.startPage(query.getPage(), query.getSize());
                List<TDicType> list = dicMapper.selectDicTypes(query);
                return new PageInfo<>(list);
            },
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public PageInfo<TDicValue> getDicValues(DicQuery query) {
        String cacheKey = CACHE_KEY_PREFIX + "values:" + query.toString();
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> {
                PageHelper.startPage(query.getCurrent(), query.getPageSize());
                List<TDicValue> list = dicMapper.selectDicValues(query);
                return new PageInfo<>(list);
            },
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicType getDicTypeById(Integer id) {
        String cacheKey = CACHE_KEY_PREFIX + "type:" + id;
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicTypeById(id),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicValue getDicValueById(Integer id) {
        String cacheKey = CACHE_KEY_PREFIX + "value:" + id;
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicValueById(id),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDicType(TDicType dicType) {
        boolean result = dicMapper.insertDicType(dicType) > 0;
        if (result) {
            clearCache(CACHE_KEY_PREFIX + "types:*");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDicValue(TDicValue dicValue) {
        boolean result = dicMapper.insertDicValue(dicValue) > 0;
        if (result) {
            clearCache(CACHE_KEY_PREFIX + "values:*");
            clearCache(CACHE_KEY_PREFIX + "type:" + dicValue.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDicType(Integer id, TDicType dicType) {
        // Get the old type code before update for cache management
        TDicType oldDicType = getDicTypeById(id);
        if (oldDicType == null) {
            return false;
        }
        
        boolean result = dicMapper.updateDicType(id, dicType) > 0;
        if (result) {
            // Clear all type-related caches
            clearCache(CACHE_KEY_PREFIX + "types:*");
            clearCache(CACHE_KEY_PREFIX + "type:" + id);
            
            // Clear old type code cache if it changed
            if (!Objects.equals(oldDicType.getTypeCode(), dicType.getTypeCode())) {
                clearCache(CACHE_KEY_PREFIX + "type:code:" + oldDicType.getTypeCode());
            }
            
            // Clear new type code cache
            clearCache(CACHE_KEY_PREFIX + "type:code:" + dicType.getTypeCode());
            
            // Clear related values cache
            clearCache(CACHE_KEY_PREFIX + "values:type:" + id);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDicValue(TDicValue dicValue) {
        boolean result = dicMapper.updateDicValue(dicValue) > 0;
        if (result) {
            clearCache(CACHE_KEY_PREFIX + "values:*");
            clearCache(CACHE_KEY_PREFIX + "value:" + dicValue.getId());
            clearCache(CACHE_KEY_PREFIX + "type:" + dicValue.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicType(Integer id) {
        dicMapper.deleteDicValuesByTypeId(id);
        boolean result = dicMapper.deleteDicType(id) > 0;
        if (result) {
            clearCache(CACHE_KEY_PREFIX + "*");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicValue(Integer id) {
        TDicValue dicValue = getDicValueById(id);
        if (dicValue != null) {
            boolean result = dicMapper.deleteDicValue(id) > 0;
            if (result) {
                clearCache(CACHE_KEY_PREFIX + "values:*");
                clearCache(CACHE_KEY_PREFIX + "value:" + id);
                clearCache(CACHE_KEY_PREFIX + "type:" + dicValue.getTypeCode());
            }
            return result;
        }
        return false;
    }

    @Override
    public List<TDicValue> getDicValuesByTypeId(Integer typeId) {
        String cacheKey = CACHE_KEY_PREFIX + "values:type:" + typeId;
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicValuesByTypeId(typeId),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicType getDicTypeByCode(String typeCode) {
        String cacheKey = CACHE_KEY_PREFIX + "type:code:" + typeCode;
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicTypeByCode(typeCode),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public void clearCache(String pattern) {
        redisManager.deletePattern(pattern);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicTypesByIds(List<Integer> ids) {
        // 先获取要删除的类型信息，用于后续清除缓存
        List<TDicType> types = dicMapper.selectDicTypesByIds(ids);
        
        // 删除相关的字典值
        for (Integer id : ids) {
            dicMapper.deleteDicValuesByTypeId(id);
        }
        
        // 删除字典类型
        boolean result = dicMapper.deleteDicTypesByIds(ids) > 0;
        
        if (result) {
            // 清除相关缓存
            clearCache(CACHE_KEY_PREFIX + "types:*");
            for (TDicType type : types) {
                clearCache(CACHE_KEY_PREFIX + "type:" + type.getId());
                clearCache(CACHE_KEY_PREFIX + "type:code:" + type.getTypeCode());
            }
        }
        
        return result;
    }
} 