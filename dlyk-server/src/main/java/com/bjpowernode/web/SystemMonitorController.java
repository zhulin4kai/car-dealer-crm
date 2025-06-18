package com.bjpowernode.web;

import com.bjpowernode.result.R;
import com.bjpowernode.service.SystemMonitorService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 系统监控控制器
 * 用于获取真实的系统硬件信息和运行状态
 */
@RestController
@RequestMapping("/api/monitor")
@CrossOrigin
public class SystemMonitorController {

    @Resource
    private SystemMonitorService systemMonitorService;

    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public R getSystemInfo() {
        return R.OK(systemMonitorService.getSystemInfo());
    }

    /**
     * 获取内存信息
     */
    @GetMapping("/memory-info")
    public R getMemoryInfo() {
        return R.OK(systemMonitorService.getMemoryInfo());
    }

    /**
     * 获取CPU信息
     */
    @GetMapping("/cpu-info")
    public R getCpuInfo() {
        return R.OK(systemMonitorService.getCpuInfo());
    }

    /**
     * 获取磁盘信息
     */
    @GetMapping("/disk-info")
    public R getDiskInfo() {
        return R.OK(systemMonitorService.getDiskInfo());
    }

    /**
     * 获取网络信息
     */
    @GetMapping("/network-info")
    public R getNetworkInfo() {
        return R.OK(systemMonitorService.getNetworkInfo());
    }

    /**
     * 获取JVM信息
     */
    @GetMapping("/jvm-info")
    public R getJvmInfo() {
        return R.OK(systemMonitorService.getJvmInfo());
    }

    /**
     * 获取完整的系统监控数据
     */
    @GetMapping("/all")
    public R getAllMonitorData() {
        return R.OK(systemMonitorService.getAllMonitorData());
    }
}
