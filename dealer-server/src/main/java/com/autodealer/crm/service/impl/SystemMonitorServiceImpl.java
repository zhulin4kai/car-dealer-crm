package com.autodealer.crm.service.impl;

import com.autodealer.crm.dto.SystemMonitorDTO;
import com.autodealer.crm.service.SystemMonitorService;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统监控服务实现类
 */
@Service
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private final SystemInfo systemInfo = new SystemInfo();
    private final DecimalFormat df = new DecimalFormat("#.##");

    @Override
    public SystemMonitorDTO.SystemInfo getSystemInfo() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        ComputerSystem computerSystem = systemInfo.getHardware().getComputerSystem();
        
        SystemMonitorDTO.SystemInfo info = new SystemMonitorDTO.SystemInfo();
        info.setPlatform(os.getFamily());
        info.setOsName(os.toString());
        info.setOsVersion(os.getVersionInfo().toString());
        info.setArch(System.getProperty("os.arch"));
        info.setHostname(computerSystem.getBaseboard().getSerialNumber());
        info.setManufacturer(computerSystem.getManufacturer());
        info.setModel(computerSystem.getModel());
        
        long uptimeSeconds = os.getSystemUptime();
        info.setUptime(uptimeSeconds);
        info.setUptimeFormatted(formatUptime(uptimeSeconds));
        
        return info;
    }

    @Override
    public SystemMonitorDTO.MemoryInfo getMemoryInfo() {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        
        SystemMonitorDTO.MemoryInfo info = new SystemMonitorDTO.MemoryInfo();
        info.setTotalMemory(memory.getTotal());
        info.setAvailableMemory(memory.getAvailable());
        info.setUsedMemory(memory.getTotal() - memory.getAvailable());
        info.setUsagePercentage(((double) (memory.getTotal() - memory.getAvailable()) / memory.getTotal()) * 100);
        
        info.setTotalMemoryFormatted(formatBytes(memory.getTotal()));
        info.setAvailableMemoryFormatted(formatBytes(memory.getAvailable()));
        info.setUsedMemoryFormatted(formatBytes(memory.getTotal() - memory.getAvailable()));
        
        return info;
    }

    @Override
    public SystemMonitorDTO.CpuInfo getCpuInfo() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        
        SystemMonitorDTO.CpuInfo info = new SystemMonitorDTO.CpuInfo();
        info.setName(processor.getProcessorIdentifier().getName());
        info.setVendor(processor.getProcessorIdentifier().getVendor());
        info.setFamily(processor.getProcessorIdentifier().getFamily());
        info.setModel(processor.getProcessorIdentifier().getModel());
        info.setIdentifier(processor.getProcessorIdentifier().getIdentifier());
        info.setPhysicalProcessors(processor.getPhysicalProcessorCount());
        info.setLogicalProcessors(processor.getLogicalProcessorCount());
        info.setMaxFreq(processor.getMaxFreq());
        
        // 获取CPU负载
        double systemCpuLoad = processor.getSystemCpuLoad(1000);
        info.setSystemCpuLoad(systemCpuLoad * 100);
        
        double[] processorCpuLoad = processor.getProcessorCpuLoad(1000);
        double[] loadPercentages = new double[processorCpuLoad.length];
        for (int i = 0; i < processorCpuLoad.length; i++) {
            loadPercentages[i] = processorCpuLoad[i] * 100;
        }
        info.setProcessorCpuLoad(loadPercentages);
        
        // 构建CPU核心信息
        List<SystemMonitorDTO.CpuCore> cores = new ArrayList<>();
        for (int i = 0; i < processorCpuLoad.length; i++) {
            SystemMonitorDTO.CpuCore core = new SystemMonitorDTO.CpuCore();
            core.setCoreId(i);
            core.setLoadPercentage(processorCpuLoad[i] * 100);
            core.setFrequency(processor.getCurrentFreq()[i]);
            cores.add(core);
        }
        info.setCores(cores);
        
        return info;
    }

    @Override
    public SystemMonitorDTO.DiskInfo getDiskInfo() {
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        
        SystemMonitorDTO.DiskInfo info = new SystemMonitorDTO.DiskInfo();
        List<SystemMonitorDTO.DiskPartition> partitions = new ArrayList<>();
        
        long totalSpace = 0;
        long usedSpace = 0;
        long freeSpace = 0;
        
        for (HWDiskStore disk : diskStores) {
            for (HWPartition partition : disk.getPartitions()) {
                SystemMonitorDTO.DiskPartition partInfo = new SystemMonitorDTO.DiskPartition();
                partInfo.setName(partition.getName());
                partInfo.setType(partition.getType());
                partInfo.setMountPoint(partition.getMountPoint());
                
                // 获取文件系统信息
                try {
                    java.io.File file = new java.io.File(partition.getMountPoint());
                    if (file.exists()) {
                        long total = file.getTotalSpace();
                        long free = file.getFreeSpace();
                        long used = total - free;
                        
                        partInfo.setTotalSpace(total);
                        partInfo.setUsedSpace(used);
                        partInfo.setFreeSpace(free);
                        partInfo.setUsagePercentage(total > 0 ? ((double) used / total) * 100 : 0);
                        
                        partInfo.setTotalSpaceFormatted(formatBytes(total));
                        partInfo.setUsedSpaceFormatted(formatBytes(used));
                        partInfo.setFreeSpaceFormatted(formatBytes(free));
                        
                        totalSpace += total;
                        usedSpace += used;
                        freeSpace += free;
                    }
                } catch (Exception e) {
                    // 忽略无法访问的分区
                }
                
                partitions.add(partInfo);
            }
        }
        
        info.setPartitions(partitions);
        info.setTotalSpace(totalSpace);
        info.setUsedSpace(usedSpace);
        info.setFreeSpace(freeSpace);
        info.setUsagePercentage(totalSpace > 0 ? ((double) usedSpace / totalSpace) * 100 : 0);
        
        return info;
    }

    @Override
    public SystemMonitorDTO.JvmInfo getJvmInfo() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        SystemMonitorDTO.JvmInfo info = new SystemMonitorDTO.JvmInfo();
        info.setJavaVersion(System.getProperty("java.version"));
        info.setJavaVendor(System.getProperty("java.vendor"));
        info.setJvmName(System.getProperty("java.vm.name"));
        info.setJvmVersion(System.getProperty("java.vm.version"));
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        info.setMaxMemory(maxMemory);
        info.setTotalMemory(totalMemory);
        info.setUsedMemory(usedMemory);
        info.setFreeMemory(freeMemory);
        info.setMemoryUsagePercentage(((double) usedMemory / maxMemory) * 100);
        
        info.setMaxMemoryFormatted(formatBytes(maxMemory));
        info.setTotalMemoryFormatted(formatBytes(totalMemory));
        info.setUsedMemoryFormatted(formatBytes(usedMemory));
        info.setFreeMemoryFormatted(formatBytes(freeMemory));
        
        info.setStartTime(runtimeBean.getStartTime());
        info.setUptime(runtimeBean.getUptime() / 1000); // 转换为秒
        info.setUptimeFormatted(formatUptime(runtimeBean.getUptime() / 1000));
        
        return info;
    }

    @Override
    public SystemMonitorDTO.NetworkInfo getNetworkInfo() {
        List<NetworkIF> networkIFs = systemInfo.getHardware().getNetworkIFs();
        
        SystemMonitorDTO.NetworkInfo info = new SystemMonitorDTO.NetworkInfo();
        List<SystemMonitorDTO.NetworkInterface> interfaces = new ArrayList<>();
        
        long totalBytesReceived = 0;
        long totalBytesSent = 0;
        
        for (NetworkIF net : networkIFs) {
            net.updateAttributes(); // 更新网络接口统计信息
            
            SystemMonitorDTO.NetworkInterface netInfo = new SystemMonitorDTO.NetworkInterface();
            netInfo.setName(net.getName());
            netInfo.setDisplayName(net.getDisplayName());
            netInfo.setIpAddresses(net.getIPv4addr());
            netInfo.setMacAddress(net.getMacaddr());
            netInfo.setBytesReceived(net.getBytesRecv());
            netInfo.setBytesSent(net.getBytesSent());
            netInfo.setPacketsReceived(net.getPacketsRecv());
            netInfo.setPacketsSent(net.getPacketsSent());
            netInfo.setUp(net.isKnownVmMacAddr());
            netInfo.setSpeed(net.getSpeed());
            
            totalBytesReceived += net.getBytesRecv();
            totalBytesSent += net.getBytesSent();
            
            interfaces.add(netInfo);
        }
        
        info.setInterfaces(interfaces);
        info.setTotalBytesReceived(totalBytesReceived);
        info.setTotalBytesSent(totalBytesSent);
        
        return info;
    }

    @Override
    public SystemMonitorDTO.AllMonitorData getAllMonitorData() {
        SystemMonitorDTO.AllMonitorData data = new SystemMonitorDTO.AllMonitorData();
        data.setSystemInfo(getSystemInfo());
        data.setMemoryInfo(getMemoryInfo());
        data.setCpuInfo(getCpuInfo());
        data.setDiskInfo(getDiskInfo());
        data.setJvmInfo(getJvmInfo());
        data.setNetworkInfo(getNetworkInfo());
        data.setTimestamp(System.currentTimeMillis());
        
        return data;
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return df.format(bytes / Math.pow(1024, exp)) + " " + pre + "B";
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptimeSeconds) {
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        if (hours > 0) {
            sb.append(hours).append("小时 ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟 ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("秒");
        }
        
        return sb.toString().trim();
    }
}
