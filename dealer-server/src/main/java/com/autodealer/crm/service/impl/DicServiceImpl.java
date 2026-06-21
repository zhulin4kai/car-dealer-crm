package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.model.TDicType;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.query.DicQuery;
import com.autodealer.crm.service.DicService;
import com.autodealer.crm.util.CacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
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

    @Resource
    private OperationAuditRecorder auditRecorder;

    private static final long CACHE_EXPIRE_SECONDS = 24 * 60 * 60; // 24小时

    @Override
    public PageInfo<TDicType> getDicTypes(DicQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<TDicType> list = dicMapper.selectDicTypes(query);
        return new PageInfo<>(list);
    }

    @Override
    public PageInfo<TDicValue> getDicValues(DicQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<TDicValue> list = dicMapper.selectDicValues(query);
        return new PageInfo<>(list);
    }

    @Override
    public TDicType getDicTypeById(Integer id) {
        String cacheKey = RedisKeys.dictTypeDetail(id);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicTypeById(id),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicValue getDicValueById(Integer id) {
        String cacheKey = RedisKeys.dictValueDetail(id);
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
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_TYPE_SAVE, dicType.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDicValue(TDicValue dicValue) {
        // 验证字典类型是否存在
        TDicType existingType = dicMapper.selectDicTypeByCode(dicValue.getTypeCode());
        if (existingType == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "无法添加字典值：字典类型 " + dicValue.getTypeCode() + " 不存在");
        }

        boolean result = dicMapper.insertDicValue(dicValue) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_VALUE_SAVE, dicValue.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDicType(Integer id, TDicType dicType) {
        // 更新前先获取旧类型，用于缓存管理
        TDicType oldDicType = getDicTypeById(id);
        if (oldDicType == null) {
            return false;
        }

        boolean result = dicMapper.updateDicType(id, dicType) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_TYPE_SAVE, String.valueOf(id));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDicValue(Integer id, TDicValue dicValue) {
        // 更新前先获取旧值，用于缓存管理
        TDicValue oldDicValue = getDicValueById(id);
        if (oldDicValue == null) {
            return false;
        }

        // 设置 ID 用于更新
        dicValue.setId(id);

        boolean result = dicMapper.updateDicValue(dicValue) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_VALUE_SAVE, String.valueOf(id));
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

            // 3. 检查是否有业务数据引用
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                int remarkCount = dicMapper.selectRemarkCountByDicValueIds(dicValueIds);
                if (remarkCount > 0) {
                    throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "该字典类型下有业务数据引用，无法删除");
                }
            }

            // 4. 删除字典值 (t_dic_value中的type_code引用t_dic_type的type_code)
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteDicValuesByIds(dicValueIds);
            }

            // 5. 最后删除字典类型
            boolean result = dicMapper.deleteDicType(id) > 0;
            if (result) {
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_TYPE_DELETE, String.valueOf(id));
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "删除字典类型失败", e);
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
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_VALUE_DELETE, String.valueOf(id));
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "删除字典值失败", e);
        }
    }

    @Override
    public List<TDicValue> getDicValuesByTypeId(Integer typeId) {
        String cacheKey = RedisKeys.dictValuesByType(typeId);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicValuesByTypeId(typeId),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicType getDicTypeByCode(String typeCode) {
        String cacheKey = RedisKeys.dictTypeByCode(typeCode);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicTypeByCode(typeCode),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public void evictDictionaryCaches() {
        redisManager.deletePattern(RedisKeys.dictTypePattern());
        redisManager.deletePattern(RedisKeys.dictValuePattern());
        redisManager.deletePattern(RedisKeys.dictListPattern());
        log.info("Dictionary cache evicted");
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

            // 2. 批量获取关联的字典值ID
            List<Integer> dicValueIds = dicMapper.selectDicValueIdsByTypeCodes(typeCodes);

            // 3. 先删除关联的备注记录
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteRemarksByDicValueIds(dicValueIds);
            }

            // 4. 再删除字典值
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteDicValuesByIds(dicValueIds);
            }

            // 5. 最后删除字典类型
            boolean result = dicMapper.deleteDicTypesByIds(ids) > 0;
            if (result) {
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_TYPE_DELETE, ids.toString());
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "批量删除字典类型失败", e);
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
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_VALUE_DELETE, ids.toString());
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "批量删除字典值失败", e);
        }
    }

    @Override
    public void refreshTypeCache() {
        // 清除字典类型相关缓存
        redisManager.deletePattern(RedisKeys.dictTypePattern());
        // 重新加载字典类型数据到缓存
        DicQuery query = new DicQuery();
        List<TDicType> types = dicMapper.selectDicTypes(query);
        for (TDicType type : types) {
            String cacheKey = RedisKeys.dictTypeByCode(type.getTypeCode());
            redisManager.set(cacheKey, type, CACHE_EXPIRE_SECONDS);
        }
        log.info("Dictionary type cache refreshed, {} types loaded", types.size());
    }

    @Override
    public void refreshValueCache() {
        // 清除字典值相关缓存
        redisManager.deletePattern(RedisKeys.dictValuePattern());
        // 重新加载字典值数据到缓存
        DicQuery query = new DicQuery();
        List<TDicValue> values = dicMapper.selectDicValues(query);
        for (TDicValue value : values) {
            String cacheKey = RedisKeys.dictValueDetail(value.getId());
            redisManager.set(cacheKey, value, CACHE_EXPIRE_SECONDS);
        }
        log.info("Dictionary value cache refreshed, {} values loaded", values.size());
    }
}
