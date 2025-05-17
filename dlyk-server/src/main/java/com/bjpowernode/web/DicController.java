package com.bjpowernode.web;

import com.bjpowernode.model.TDicType;
import com.bjpowernode.model.TDicValue;
import com.bjpowernode.query.DicQuery;
import com.bjpowernode.result.R;
import com.bjpowernode.service.DicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class DicController {
    private static final Logger logger = LoggerFactory.getLogger(DicController.class);

    @Autowired
    private DicService dicService;

    /**
     * 字典类型
     */
    @GetMapping("/debug/authorities")
    public R getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            logger.info("Current user: {}", authentication.getName());
            logger.info("Authorities: {}", authentication.getAuthorities());
            return R.OK(authentication.getAuthorities());
        }
        return R.FAIL("No authentication found");
    }

    @GetMapping("/type/all")
    public R getDicAllTypes(DicQuery query) {
        if (query.getPage() == null) {
            query.setPage(1);
        }
        if (query.getSize() == null) {
            query.setSize(10);
        }
        logger.debug("Querying dictionary types with params: {}", query);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("Current user authorities: {}", auth != null ? auth.getAuthorities() : "none");
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
        return R.OK(dicService.getDicValues(query));
    }

    @GetMapping("/value/{id}")
    public R getDicValueById(@PathVariable Integer id) {
        return R.OK(dicService.getDicValueById(id));
    }

    @PostMapping("/value")
    public R addDicValue(@RequestBody TDicValue dicValue) {
        return dicService.addDicValue(dicValue) ? R.OK() : R.FAIL("添加字典值失败");
    }

    @PutMapping("/value")
    public R updateDicValue(@RequestBody TDicValue dicValue) {
        return dicService.updateDicValue(dicValue) ? R.OK() : R.FAIL("更新字典值失败");
    }

    @DeleteMapping("/value/{id}")
    public R deleteDicValue(@PathVariable Integer id) {
        return dicService.deleteDicValue(id) ? R.OK() : R.FAIL("删除字典值失败");
    }

    @GetMapping("/values/type/{typeId}")
    public R getDicValuesByTypeId(@PathVariable Integer typeId) {
        return R.OK(dicService.getDicValuesByTypeId(typeId));
    }

}
