package com.bjpowernode.dto;

import lombok.Data;
import java.util.List;

/**
 * 系统监控数据传输对象
 */
@Data
public class SystemMonitorDTO {
    
    /**
     * 系统基本信息
     */
    @Data
    public static class SystemInfo {
        private String platform;          // 操作系统平台
        private String osName;            // 操作系统名称
        private String osVersion;         // 操作系统版本
        private String arch;              // 系统架构
        private String hostname;          // 主机名
        private String manufacturer;      // 制造商
        private String model;             // 型号
        private long uptime;              // 系统运行时间（秒）
        private String uptimeFormatted;   // 格式化的运行时间
    }

    /**
     * 内存信息
     */
    @Data
    public static class MemoryInfo {
        private long totalMemory;         // 总内存（字节）
        private long availableMemory;     // 可用内存（字节）
        private long usedMemory;          // 已用内存（字节）
        private double usagePercentage;   // 使用率百分比
        private String totalMemoryFormatted;     // 格式化的总内存
        private String availableMemoryFormatted; // 格式化的可用内存
        private String usedMemoryFormatted;      // 格式化的已用内存
    }

    /**
     * CPU信息
     */
    @Data
    public static class CpuInfo {
        private String name;              // CPU名称
        private String vendor;            // CPU厂商
        private String family;            // CPU系列
        private String model;             // CPU型号
        private String identifier;        // CPU标识符
        private int physicalProcessors;   // 物理处理器数
        private int logicalProcessors;    // 逻辑处理器数
        private long maxFreq;             // 最大频率
        private double systemCpuLoad;     // 系统CPU负载
        private double[] processorCpuLoad; // 各处理器负载
        private List<CpuCore> cores;      // CPU核心详情
    }

    /**
     * CPU核心信息
     */
    @Data
    public static class CpuCore {
        private int coreId;               // 核心ID
        private double loadPercentage;    // 负载百分比
        private long frequency;           // 频率
    }

    /**
     * 磁盘信息
     */
    @Data
    public static class DiskInfo {
        private List<DiskPartition> partitions; // 磁盘分区列表
        private long totalSpace;                // 总空间
        private long usedSpace;                 // 已用空间
        private long freeSpace;                 // 可用空间
        private double usagePercentage;         // 使用率百分比
    }

    /**
     * 磁盘分区信息
     */
    @Data
    public static class DiskPartition {
        private String name;              // 分区名称
        private String type;              // 文件系统类型
        private String mountPoint;        // 挂载点
        private long totalSpace;          // 总空间
        private long usedSpace;           // 已用空间
        private long freeSpace;           // 可用空间
        private double usagePercentage;   // 使用率百分比
        private String totalSpaceFormatted;   // 格式化的总空间
        private String usedSpaceFormatted;    // 格式化的已用空间
        private String freeSpaceFormatted;    // 格式化的可用空间
    }

    /**
     * JVM信息
     */
    @Data
    public static class JvmInfo {
        private String javaVersion;       // Java版本
        private String javaVendor;        // Java厂商
        private String jvmName;           // JVM名称
        private String jvmVersion;        // JVM版本
        private long maxMemory;           // 最大内存
        private long totalMemory;         // 总内存
        private long usedMemory;          // 已用内存
        private long freeMemory;          // 可用内存
        private double memoryUsagePercentage; // 内存使用率
        private String maxMemoryFormatted;    // 格式化的最大内存
        private String totalMemoryFormatted;  // 格式化的总内存
        private String usedMemoryFormatted;   // 格式化的已用内存
        private String freeMemoryFormatted;   // 格式化的可用内存
        private long startTime;               // JVM启动时间
        private long uptime;                  // JVM运行时间
        private String uptimeFormatted;       // 格式化的运行时间
    }

    /**
     * 网络信息
     */
    @Data
    public static class NetworkInfo {
        private List<NetworkInterface> interfaces; // 网络接口列表
        private long totalBytesReceived;          // 总接收字节数
        private long totalBytesSent;              // 总发送字节数
    }

    /**
     * 网络接口信息
     */
    @Data
    public static class NetworkInterface {
        private String name;              // 接口名称
        private String displayName;       // 显示名称
        private String[] ipAddresses;     // IP地址列表
        private String macAddress;        // MAC地址
        private long bytesReceived;       // 接收字节数
        private long bytesSent;           // 发送字节数
        private long packetsReceived;     // 接收包数
        private long packetsSent;         // 发送包数
        private boolean isUp;             // 是否活跃
        private long speed;               // 速度
    }

    /**
     * 完整监控数据
     */
    @Data
    public static class AllMonitorData {
        private SystemInfo systemInfo;
        private MemoryInfo memoryInfo;
        private CpuInfo cpuInfo;
        private DiskInfo diskInfo;
        private JvmInfo jvmInfo;
        private NetworkInfo networkInfo;
        private long timestamp;           // 数据时间戳
    }
}
