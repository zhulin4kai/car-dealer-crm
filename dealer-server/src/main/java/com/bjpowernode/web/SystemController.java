package com.bjpowernode.web;

import com.bjpowernode.model.TSystem;
import com.bjpowernode.result.R;
import com.bjpowernode.service.SystemService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {
   
    @Resource
    private SystemService systemService;

    @GetMapping("/list")
    public R getAllList() {
        return R.OK(systemService.getAllList());
    }
    
    @GetMapping("/{id}")
    public R getSystemDetail(@PathVariable Integer id) {
        return R.OK(systemService.getById(id));
    }
    
    @PostMapping("/create")
    public R createSystem(@RequestBody TSystem system) {
        systemService.create(system);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    public R updateSystem(@PathVariable Integer id, @RequestBody TSystem system) {
        systemService.update(id, system);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    public R deleteSystem(@PathVariable Integer id) {
        systemService.delete(id);
        return R.OK();
    }
    
    @DeleteMapping("/batch")
    public R batchDeleteSystems(@RequestBody List<Integer> ids) {
        systemService.batchDelete(ids);
        return R.OK();
    }
    
    @PutMapping("/{id}/status")
    public R toggleSystemStatus(@PathVariable Integer id, @RequestBody TSystem system) {
        systemService.toggleStatus(id, system.getIsopen());
        return R.OK();
    }
}
