package com.autodealer.crm.web;

import com.autodealer.crm.model.TSystem;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.SystemService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {
   
    @Resource
    private SystemService systemService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:list')")
    public R getAllList() {
        return R.OK(systemService.getAllList());
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:view')")
    public R getSystemDetail(@PathVariable Integer id) {
        return R.OK(systemService.getById(id));
    }
    
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('system:add')")
    public R createSystem(@RequestBody TSystem system) {
        systemService.create(system);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:edit')")
    public R updateSystem(@PathVariable Integer id, @RequestBody TSystem system) {
        systemService.update(id, system);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:delete')")
    public R deleteSystem(@PathVariable Integer id) {
        systemService.delete(id);
        return R.OK();
    }
    
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('system:delete')")
    public R batchDeleteSystems(@RequestBody List<Integer> ids) {
        systemService.batchDelete(ids);
        return R.OK();
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:edit')")
    public R toggleSystemStatus(@PathVariable Integer id, @RequestBody TSystem system) {
        systemService.toggleStatus(id, system.getIsopen());
        return R.OK();
    }
}
