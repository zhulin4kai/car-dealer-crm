package com.bjpowernode.service;

import com.bjpowernode.model.TSystemMonitor;

/**
 * 系统监控服务接口
 */
public interface SystemMonitorService {

    /**
     * 获取系统基本信息
     */
    TSystemMonitor.SystemInfo getSystemInfo();

    /**
     * 获取内存信息
     */
    TSystemMonitor.MemoryInfo getMemoryInfo();

    /**
     * 获取CPU信息
     */
    TSystemMonitor.CpuInfo getCpuInfo();

    /**
     * 获取磁盘信息
     */
    TSystemMonitor.DiskInfo getDiskInfo();

    /**
     * 获取JVM信息
     */
    TSystemMonitor.JvmInfo getJvmInfo();

    /**
     * 获取网络信息
     */
    TSystemMonitor.NetworkInfo getNetworkInfo();

    /**
     * 获取所有监控数据
     */
    TSystemMonitor.AllMonitorData getAllMonitorData();
}
