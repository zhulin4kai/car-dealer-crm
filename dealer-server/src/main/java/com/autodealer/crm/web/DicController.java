package com.autodealer.crm.web;

import com.autodealer.crm.model.TDicType;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.query.DicQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.DicService;
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
    public R getDicTypes(DicQuery query) {
        if (query.getPage() == null) {
            query.setPage(1);
        }
        if (query.getSize() == null) {
            query.setSize(10);
        }
        return R.OK(dicService.getDicTypes(query));
    }

    @GetMapping("/type/get/{id}")
    public R getDicTypeById(@PathVariable Integer id) {
        return R.OK(dicService.getDicTypeById(id));
    }

    @PostMapping("/type/create")
    public R addDicType(@RequestBody TDicType dicType) {
        return dicService.addDicType(dicType) ? R.OK() : R.FAIL("添加字典类型失败");
    }

    @PutMapping("/type/update/{id}")
    public R updateDicType(@PathVariable Integer id, @RequestBody TDicType dicType) {
        return dicService.updateDicType(id, dicType) ? R.OK() : R.FAIL("更新字典类型失败");
    }

    @DeleteMapping("/type/delete/{id}")
    public R deleteDicType(@PathVariable Integer id) {
        return dicService.deleteDicType(id) ? R.OK() : R.FAIL("删除字典类型失败");
    }

    @DeleteMapping("/types/batch")
    public R batchDeleteDicTypes(@RequestBody List<Integer> ids) {
        return dicService.deleteDicTypesByIds(ids) ? R.OK() : R.FAIL("批量删除字典类型失败");
    }

     /**
     * 字典值
     */
    @GetMapping("/values")
    public R getDicValues(DicQuery query) {
        if (query.getPage() == null) {
            query.setPage(1);
        }
        if (query.getSize() == null) {
            query.setSize(10);
        }
        return R.OK(dicService.getDicValues(query));
    }

    @GetMapping("/value/get/{id}")
    public R getDicValueById(@PathVariable Integer id) {
        return R.OK(dicService.getDicValueById(id));
    }

    @PostMapping("/value/create")
    public R addDicValue(@RequestBody TDicValue dicValue) {
        return dicService.addDicValue(dicValue) ? R.OK() : R.FAIL("添加字典值失败");
    }

    @PutMapping("/value/update/{id}")
    public R updateDicValue(@PathVariable Integer id, @RequestBody TDicValue dicValue) {
        return dicService.updateDicValue(id, dicValue) ? R.OK() : R.FAIL("更新字典值失败");
    }

    @DeleteMapping("/value/delete/{id}")
    public R deleteDicValue(@PathVariable Integer id) {
        return dicService.deleteDicValue(id) ? R.OK() : R.FAIL("删除字典值失败");
    }

    @DeleteMapping("/value/batch")
    public R batchDeleteDicValues(@RequestBody List<Integer> ids) {
        return dicService.deleteDicValuesByIds(ids) ? R.OK() : R.FAIL("批量删除字典值失败");
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/clear")
    public R clearCache(@RequestParam(required = false) Boolean forceRefresh) {
        dicService.clearCache("*");
        if (Boolean.TRUE.equals(forceRefresh)) {
            // 如果需要强制刷新，则重新加载所有缓存
            dicService.refreshTypeCache();
            dicService.refreshValueCache();
        }
        return R.OK();
    }

    @GetMapping("/refresh")
    public R refreshDictData(@RequestParam(required = false) String type) {
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
        return R.OK();
    }
}
