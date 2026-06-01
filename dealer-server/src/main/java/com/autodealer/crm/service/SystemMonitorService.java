package com.autodealer.crm.service;

import com.autodealer.crm.dto.SystemMonitorDTO;

/**
 * 系统监控服务接口
 */
public interface SystemMonitorService {

    /**
     * 获取系统基本信息
     */
    SystemMonitorDTO.SystemInfo getSystemInfo();

    /**
     * 获取内存信息
     */
    SystemMonitorDTO.MemoryInfo getMemoryInfo();

    /**
     * 获取CPU信息
     */
    SystemMonitorDTO.CpuInfo getCpuInfo();

    /**
     * 获取磁盘信息
     */
    SystemMonitorDTO.DiskInfo getDiskInfo();

    /**
     * 获取JVM信息
     */
    SystemMonitorDTO.JvmInfo getJvmInfo();

    /**
     * 获取网络信息
     */
    SystemMonitorDTO.NetworkInfo getNetworkInfo();

    /**
     * 获取所有监控数据
     */
    SystemMonitorDTO.AllMonitorData getAllMonitorData();
}
