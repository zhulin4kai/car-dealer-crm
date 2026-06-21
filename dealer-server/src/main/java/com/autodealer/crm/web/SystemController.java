package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.CreateSystemRequest;
import com.autodealer.crm.dto.SystemResponse;
import com.autodealer.crm.dto.ToggleSystemStatusRequest;
import com.autodealer.crm.dto.UpdateSystemRequest;
import com.autodealer.crm.model.TSystem;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.SystemService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Resource
    private SystemService systemService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_LIST + "')")
    public R<List<SystemResponse>> getAllList() {
        List<SystemResponse> list = systemService.getAllList().stream()
                .map(SystemResponse::from)
                .collect(Collectors.toList());
        return R.OK(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_VIEW + "')")
    public R<SystemResponse> getSystemDetail(@PathVariable Integer id) {
        return R.OK(SystemResponse.from(systemService.getById(id)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_ADD + "')")
    public R<Void> createSystem(@Valid @RequestBody CreateSystemRequest req) {
        TSystem system = new TSystem();
        system.setSystemCode(req.getSystemCode());
        system.setName(req.getName());
        system.setSite(req.getSite());
        system.setLogo(req.getLogo());
        system.setTitle(req.getTitle());
        system.setDescription(req.getDescription());
        system.setKeywords(req.getKeywords());
        system.setShortcuticon(req.getShortcuticon());
        system.setTel(req.getTel());
        system.setWeixin(req.getWeixin());
        system.setEmail(req.getEmail());
        system.setAddress(req.getAddress());
        system.setVersion(req.getVersion());
        system.setCloseMsg(req.getCloseMsg());
        system.setIsopen(req.getIsopen());
        systemService.create(system);
        return R.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_EDIT + "')")
    public R<Void> updateSystem(@PathVariable Integer id, @Valid @RequestBody UpdateSystemRequest req) {
        TSystem system = new TSystem();
        system.setSystemCode(req.getSystemCode());
        system.setName(req.getName());
        system.setSite(req.getSite());
        system.setLogo(req.getLogo());
        system.setTitle(req.getTitle());
        system.setDescription(req.getDescription());
        system.setKeywords(req.getKeywords());
        system.setShortcuticon(req.getShortcuticon());
        system.setTel(req.getTel());
        system.setWeixin(req.getWeixin());
        system.setEmail(req.getEmail());
        system.setAddress(req.getAddress());
        system.setVersion(req.getVersion());
        system.setCloseMsg(req.getCloseMsg());
        systemService.update(id, system);
        return R.OK();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_DELETE + "')")
    public R<Void> deleteSystem(@PathVariable Integer id) {
        systemService.delete(id);
        return R.OK();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_DELETE + "')")
    public R<Void> batchDeleteSystems(@RequestBody List<Integer> ids) {
        systemService.batchDelete(ids);
        return R.OK();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_EDIT + "')")
    public R<Void> toggleSystemStatus(@PathVariable Integer id, @Valid @RequestBody ToggleSystemStatusRequest req) {
        systemService.toggleStatus(id, req.getIsopen());
        return R.OK();
    }
}
