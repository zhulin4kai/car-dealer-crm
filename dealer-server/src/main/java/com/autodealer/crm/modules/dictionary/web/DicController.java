package com.autodealer.crm.modules.dictionary.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.dictionary.application.api.request.CreateDicTypeRequest;
import com.autodealer.crm.modules.dictionary.application.api.request.CreateDicValueRequest;
import com.autodealer.crm.modules.dictionary.application.api.request.UpdateDicTypeRequest;
import com.autodealer.crm.modules.dictionary.application.api.request.UpdateDicValueRequest;
import com.autodealer.crm.modules.dictionary.application.api.model.TDicType;
import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;
import com.autodealer.crm.modules.dictionary.application.api.query.DicQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.dictionary.application.api.DicService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class DicController {

    @Autowired
    private DicService dicService;

    /**
     * 字典类型
     */
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_TYPE_LIST + "')")
    public Result getDicTypes(DicQuery query) {
        if (query.getPage() == null) {
            query.setPage(1);
        }
        if (query.getSize() == null) {
            query.setSize(10);
        }
        return Result.OK(dicService.getDicTypes(query));
    }

    @GetMapping("/type/get/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_TYPE_VIEW + "')")
    public Result getDicTypeById(@PathVariable Integer id) {
        return Result.OK(dicService.getDicTypeById(id));
    }

    @PostMapping("/type/create")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_TYPE_ADD + "')")
    public Result addDicType(@Valid @RequestBody CreateDicTypeRequest req) {
        TDicType dicType = new TDicType();
        dicType.setTypeCode(req.getTypeCode());
        dicType.setTypeName(req.getTypeName());
        dicType.setApplicableModule(req.getApplicableModule());
        dicType.setEnabled(req.getEnabled());
        dicType.setRemark(req.getRemark());
        return dicService.addDicType(dicType) ? Result.OK() : Result.FAIL("添加字典类型失败");
    }

    @PutMapping("/type/update/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_TYPE_EDIT + "')")
    public Result updateDicType(@PathVariable Integer id, @Valid @RequestBody UpdateDicTypeRequest req) {
        TDicType dicType = new TDicType();
        dicType.setTypeCode(req.getTypeCode());
        dicType.setTypeName(req.getTypeName());
        dicType.setApplicableModule(req.getApplicableModule());
        dicType.setEnabled(req.getEnabled());
        dicType.setDisableReason(req.getDisableReason());
        dicType.setRemark(req.getRemark());
        return dicService.updateDicType(id, dicType) ? Result.OK() : Result.FAIL("更新字典类型失败");
    }

    @DeleteMapping("/type/delete/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_TYPE_DELETE + "')")
    public Result deleteDicType(@PathVariable Integer id) {
        return dicService.deleteDicType(id) ? Result.OK() : Result.FAIL("删除字典类型失败");
    }

    @DeleteMapping("/types/batch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_TYPE_DELETE + "')")
    public Result batchDeleteDicTypes(@RequestBody List<Integer> ids) {
        return dicService.deleteDicTypesByIds(ids) ? Result.OK() : Result.FAIL("批量删除字典类型失败");
    }

     /**
     * 字典值
     */
    @GetMapping("/values")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_VALUE_LIST + "')")
    public Result getDicValues(DicQuery query) {
        if (query.getPage() == null) {
            query.setPage(1);
        }
        if (query.getSize() == null) {
            query.setSize(10);
        }
        return Result.OK(dicService.getDicValues(query));
    }

    @GetMapping("/value/get/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_VALUE_VIEW + "')")
    public Result getDicValueById(@PathVariable Integer id) {
        return Result.OK(dicService.getDicValueById(id));
    }

    @PostMapping("/value/create")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_VALUE_ADD + "')")
    public Result addDicValue(@Valid @RequestBody CreateDicValueRequest req) {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode(req.getTypeCode());
        dicValue.setTypeValue(req.getTypeValue());
        dicValue.setValueCode(req.getValueCode());
        dicValue.setOrder(req.getOrder());
        dicValue.setApplicableModule(req.getApplicableModule());
        dicValue.setEnabled(req.getEnabled());
        dicValue.setRemark(req.getRemark());
        return dicService.addDicValue(dicValue) ? Result.OK() : Result.FAIL("添加字典值失败");
    }

    @PutMapping("/value/update/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_VALUE_EDIT + "')")
    public Result updateDicValue(@PathVariable Integer id, @Valid @RequestBody UpdateDicValueRequest req) {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode(req.getTypeCode());
        dicValue.setTypeValue(req.getTypeValue());
        dicValue.setValueCode(req.getValueCode());
        dicValue.setOrder(req.getOrder());
        dicValue.setApplicableModule(req.getApplicableModule());
        dicValue.setEnabled(req.getEnabled());
        dicValue.setDisableReason(req.getDisableReason());
        dicValue.setRemark(req.getRemark());
        return dicService.updateDicValue(id, dicValue) ? Result.OK() : Result.FAIL("更新字典值失败");
    }

    @DeleteMapping("/value/delete/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_VALUE_DELETE + "')")
    public Result deleteDicValue(@PathVariable Integer id) {
        return dicService.deleteDicValue(id) ? Result.OK() : Result.FAIL("删除字典值失败");
    }

    @DeleteMapping("/value/batch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_VALUE_DELETE + "')")
    public Result batchDeleteDicValues(@RequestBody List<Integer> ids) {
        return dicService.deleteDicValuesByIds(ids) ? Result.OK() : Result.FAIL("批量删除字典值失败");
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_CACHE_REFRESH + "')")
    @GetMapping("/clear")
    public Result clearCache(@RequestParam(required = false) Boolean forceRefresh) {
        dicService.evictDictionaryCaches();
        if (Boolean.TRUE.equals(forceRefresh)) {
            // 如果需要强制刷新，则重新加载所有缓存
            dicService.refreshTypeCache();
            dicService.refreshValueCache();
        }
        return Result.OK();
    }

    @GetMapping("/refresh")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DICT_CACHE_REFRESH + "')")
    public Result refreshDictData(@RequestParam(required = false) String type) {
        if ("type".equals(type)) {
            // 刷新字典类型数据
            dicService.refreshTypeCache();
        } else if ("value".equals(type)) {
            // 刷新字典值数据
            dicService.refreshValueCache();
        } else {
            // 刷新所有数据
            dicService.refreshTypeCache();
            dicService.refreshValueCache();
        }
        return Result.OK();
    }
}
