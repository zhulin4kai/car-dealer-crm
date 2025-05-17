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
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
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
                PageHelper.startPage(query.getPage(), query.getSize());
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
        // 记录接收到的参数
        System.out.println("Adding dictionary value: " + dicValue);
        
        // 验证字典类型是否存在
        TDicType existingType = dicMapper.selectDicTypeByCode(dicValue.getTypeCode());
        if (existingType == null) {
            throw new RuntimeException("无法添加字典值：字典类型 " + dicValue.getTypeCode() + " 不存在");
        }
        
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
    public boolean updateDicValue(Integer id, TDicValue dicValue) {
        // Get the old value before update for cache management
        TDicValue oldDicValue = getDicValueById(id);
        if (oldDicValue == null) {
            return false;
        }
        
        // Set the ID for the update
        dicValue.setId(id);
        
        boolean result = dicMapper.updateDicValue(dicValue) > 0;
        if (result) {
            // Clear all value-related caches
            clearCache(CACHE_KEY_PREFIX + "values:*");
            clearCache(CACHE_KEY_PREFIX + "value:" + id);
            
            // Clear old type code cache if it changed
            if (!Objects.equals(oldDicValue.getTypeCode(), dicValue.getTypeCode())) {
                clearCache(CACHE_KEY_PREFIX + "type:code:" + oldDicValue.getTypeCode());
                clearCache(CACHE_KEY_PREFIX + "values:type:" + oldDicValue.getTypeCode());
            }
            
            // Clear new type code cache
            clearCache(CACHE_KEY_PREFIX + "type:code:" + dicValue.getTypeCode());
            clearCache(CACHE_KEY_PREFIX + "values:type:" + dicValue.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicType(Integer id) {
        try {
            // 1. 获取字典类型代码
            String typeCode = dicMapper.selectTypeCodeById(id);
            if (typeCode == null) {
                return false;
            }
            
            // 2. 获取关联的字典值ID列表
            List<Integer> dicValueIds = dicMapper.selectDicValueIdsByTypeCode(typeCode);
            
            // 3. 先删除关联的备注记录 (t_tran_remark中的note_way引用t_dic_value的id)
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteRemarksByDicValueIds(dicValueIds);
            }
            
            // 4. 再删除字典值 (t_dic_value中的type_code引用t_dic_type的type_code)
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteDicValuesByIds(dicValueIds);
            }
            
            // 5. 最后删除字典类型
            boolean result = dicMapper.deleteDicType(id) > 0;
            if (result) {
                clearCache(CACHE_KEY_PREFIX + "*");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("删除字典类型失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicValue(Integer id) {
        try {
            // 获取字典值，用于后续清除缓存
            TDicValue dicValue = dicMapper.selectDicValueById(id);
            if (dicValue == null) {
                return false;
            }
            
            // 1. 先删除关联的备注记录
            dicMapper.deleteRemarksByDicValueId(id);
            // 2. 再删除字典值
            boolean result = dicMapper.deleteDicValue(id) > 0;
            if (result) {
                clearCache(CACHE_KEY_PREFIX + "*");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("删除字典值失败: " + e.getMessage(), e);
        }
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
        // 明确指定需要清除的缓存key
        String[] cachePatterns = {
            "dic:type:*",      // 字典类型缓存
            "dic:value:*",     // 字典值缓存
            "dic:list:*"       // 字典列表缓存
        };
        
        for (String cachePattern : cachePatterns) {
            redisManager.deletePattern(cachePattern);
        }
        
        // 记录缓存清除日志
        log.info("Dictionary cache cleared with patterns: {}", Arrays.toString(cachePatterns));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicTypesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        try {
            // 1. 获取所有要删除的类型代码
            List<String> typeCodes = dicMapper.selectTypeCodesByIds(ids);
            if (typeCodes == null || typeCodes.isEmpty()) {
                return false;
            }
            
            // 2. 针对每个类型代码获取关联的字典值ID
            for (String typeCode : typeCodes) {
                List<Integer> dicValueIds = dicMapper.selectDicValueIdsByTypeCode(typeCode);
                
                // 3. 先删除关联的备注记录
                if (dicValueIds != null && !dicValueIds.isEmpty()) {
                    dicMapper.deleteRemarksByDicValueIds(dicValueIds);
                }
                
                // 4. 再删除字典值
                if (dicValueIds != null && !dicValueIds.isEmpty()) {
                    dicMapper.deleteDicValuesByIds(dicValueIds);
                }
            }
            
            // 5. 最后删除字典类型
            boolean result = dicMapper.deleteDicTypesByIds(ids) > 0;
            if (result) {
                clearCache(CACHE_KEY_PREFIX + "*");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("批量删除字典类型失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicValuesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        try {
            // 1. 先删除关联的备注记录
            dicMapper.deleteRemarksByDicValueIds(ids);
            // 2. 再删除字典值
            boolean result = dicMapper.deleteDicValuesByIds(ids) > 0;
            if (result) {
                clearCache(CACHE_KEY_PREFIX + "*");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("批量删除字典值失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void refreshTypeCache() {
        // 清除字典类型相关缓存
        redisManager.deletePattern("dic:type:*");
        // 重新加载字典类型数据到缓存
        DicQuery query = new DicQuery();
        List<TDicType> types = dicMapper.selectDicTypes(query);
        for (TDicType type : types) {
            String cacheKey = "dic:type:" + type.getTypeCode();
            redisManager.set(cacheKey, type, 24 * 60 * 60); // 24小时过期
        }
        log.info("Dictionary type cache refreshed, {} types loaded", types.size());
    }

    @Override
    public void refreshValueCache() {
        // 清除字典值相关缓存
        redisManager.deletePattern("dic:value:*");
        // 重新加载字典值数据到缓存
        DicQuery query = new DicQuery();
        List<TDicValue> values = dicMapper.selectDicValues(query);
        for (TDicValue value : values) {
            String cacheKey = "dic:value:" + value.getTypeCode() + ":" + value.getId();
            redisManager.set(cacheKey, value, 24 * 60 * 60); // 24小时过期
        }
        log.info("Dictionary value cache refreshed, {} values loaded", values.size());
    }
} 