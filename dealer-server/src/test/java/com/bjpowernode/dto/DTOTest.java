package com.bjpowernode.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    // ==================== SystemMonitorDTO.SystemInfo ====================

    @Test
    void testSystemInfoGetterSetter() {
        SystemMonitorDTO.SystemInfo info = new SystemMonitorDTO.SystemInfo();
        info.setPlatform("Mac OS X");
        info.setOsName("Mac OS X");
        info.setOsVersion("14.0");
        info.setArch("aarch64");
        info.setHostname("localhost");
        info.setManufacturer("Apple");
        info.setModel("MacBook Pro");
        info.setUptime(3600L);
        info.setUptimeFormatted("1小时");

        assertEquals("Mac OS X", info.getPlatform());
        assertEquals("Mac OS X", info.getOsName());
        assertEquals("14.0", info.getOsVersion());
        assertEquals("aarch64", info.getArch());
        assertEquals("localhost", info.getHostname());
        assertEquals("Apple", info.getManufacturer());
        assertEquals("MacBook Pro", info.getModel());
        assertEquals(3600L, info.getUptime());
        assertEquals("1小时", info.getUptimeFormatted());
    }

    @Test
    void testSystemInfoDefaultConstructor() {
        SystemMonitorDTO.SystemInfo info = new SystemMonitorDTO.SystemInfo();
        assertNull(info.getPlatform());
        assertNull(info.getHostname());
        assertEquals(0L, info.getUptime());
    }

    // ==================== SystemMonitorDTO.MemoryInfo ====================

    @Test
    void testMemoryInfoGetterSetter() {
        SystemMonitorDTO.MemoryInfo mem = new SystemMonitorDTO.MemoryInfo();
        mem.setTotalMemory(16L * 1024 * 1024 * 1024);
        mem.setAvailableMemory(8L * 1024 * 1024 * 1024);
        mem.setUsedMemory(8L * 1024 * 1024 * 1024);
        mem.setUsagePercentage(50.0);
        mem.setTotalMemoryFormatted("16 GB");
        mem.setAvailableMemoryFormatted("8 GB");
        mem.setUsedMemoryFormatted("8 GB");

        assertEquals(16L * 1024 * 1024 * 1024, mem.getTotalMemory());
        assertEquals(8L * 1024 * 1024 * 1024, mem.getAvailableMemory());
        assertEquals(8L * 1024 * 1024 * 1024, mem.getUsedMemory());
        assertEquals(50.0, mem.getUsagePercentage());
        assertEquals("16 GB", mem.getTotalMemoryFormatted());
        assertEquals("8 GB", mem.getAvailableMemoryFormatted());
        assertEquals("8 GB", mem.getUsedMemoryFormatted());
    }

    @Test
    void testMemoryInfoDefaultConstructor() {
        SystemMonitorDTO.MemoryInfo mem = new SystemMonitorDTO.MemoryInfo();
        assertEquals(0L, mem.getTotalMemory());
        assertEquals(0.0, mem.getUsagePercentage());
    }

    // ==================== SystemMonitorDTO.CpuInfo ====================

    @Test
    void testCpuInfoGetterSetter() {
        SystemMonitorDTO.CpuInfo cpu = new SystemMonitorDTO.CpuInfo();
        cpu.setName("Apple M1");
        cpu.setVendor("Apple");
        cpu.setFamily("ARM");
        cpu.setModel("M1");
        cpu.setIdentifier("ARM64");
        cpu.setPhysicalProcessors(8);
        cpu.setLogicalProcessors(8);
        cpu.setMaxFreq(3200L);
        cpu.setSystemCpuLoad(25.5);
        cpu.setProcessorCpuLoad(new double[]{10.0, 20.0, 30.0});

        SystemMonitorDTO.CpuCore core = new SystemMonitorDTO.CpuCore();
        core.setCoreId(0);
        core.setLoadPercentage(15.0);
        core.setFrequency(3200L);
        cpu.setCores(Arrays.asList(core));

        assertEquals("Apple M1", cpu.getName());
        assertEquals("Apple", cpu.getVendor());
        assertEquals(8, cpu.getPhysicalProcessors());
        assertEquals(8, cpu.getLogicalProcessors());
        assertEquals(3200L, cpu.getMaxFreq());
        assertEquals(25.5, cpu.getSystemCpuLoad());
        assertEquals(3, cpu.getProcessorCpuLoad().length);
        assertNotNull(cpu.getCores());
        assertEquals(1, cpu.getCores().size());
    }

    @Test
    void testCpuInfoDefaultConstructor() {
        SystemMonitorDTO.CpuInfo cpu = new SystemMonitorDTO.CpuInfo();
        assertNull(cpu.getName());
        assertEquals(0, cpu.getPhysicalProcessors());
    }

    // ==================== SystemMonitorDTO.CpuCore ====================

    @Test
    void testCpuCoreGetterSetter() {
        SystemMonitorDTO.CpuCore core = new SystemMonitorDTO.CpuCore();
        core.setCoreId(1);
        core.setLoadPercentage(50.0);
        core.setFrequency(2400L);

        assertEquals(1, core.getCoreId());
        assertEquals(50.0, core.getLoadPercentage());
        assertEquals(2400L, core.getFrequency());
    }

    @Test
    void testCpuCoreDefaultConstructor() {
        SystemMonitorDTO.CpuCore core = new SystemMonitorDTO.CpuCore();
        assertEquals(0, core.getCoreId());
        assertEquals(0.0, core.getLoadPercentage());
        assertEquals(0L, core.getFrequency());
    }

    // ==================== SystemMonitorDTO.DiskInfo ====================

    @Test
    void testDiskInfoGetterSetter() {
        SystemMonitorDTO.DiskInfo disk = new SystemMonitorDTO.DiskInfo();
        disk.setTotalSpace(500L * 1024 * 1024 * 1024);
        disk.setUsedSpace(200L * 1024 * 1024 * 1024);
        disk.setFreeSpace(300L * 1024 * 1024 * 1024);
        disk.setUsagePercentage(40.0);

        SystemMonitorDTO.DiskPartition partition = new SystemMonitorDTO.DiskPartition();
        partition.setName("/dev/disk1");
        partition.setType("apfs");
        partition.setMountPoint("/");
        partition.setTotalSpace(500L * 1024 * 1024 * 1024);
        partition.setUsedSpace(200L * 1024 * 1024 * 1024);
        partition.setFreeSpace(300L * 1024 * 1024 * 1024);
        partition.setUsagePercentage(40.0);
        partition.setTotalSpaceFormatted("500 GB");
        partition.setUsedSpaceFormatted("200 GB");
        partition.setFreeSpaceFormatted("300 GB");
        disk.setPartitions(Arrays.asList(partition));

        assertEquals(500L * 1024 * 1024 * 1024, disk.getTotalSpace());
        assertEquals(200L * 1024 * 1024 * 1024, disk.getUsedSpace());
        assertEquals(300L * 1024 * 1024 * 1024, disk.getFreeSpace());
        assertEquals(40.0, disk.getUsagePercentage());
        assertNotNull(disk.getPartitions());
        assertEquals(1, disk.getPartitions().size());
    }

    @Test
    void testDiskInfoDefaultConstructor() {
        SystemMonitorDTO.DiskInfo disk = new SystemMonitorDTO.DiskInfo();
        assertNull(disk.getPartitions());
        assertEquals(0L, disk.getTotalSpace());
    }

    // ==================== SystemMonitorDTO.DiskPartition ====================

    @Test
    void testDiskPartitionGetterSetter() {
        SystemMonitorDTO.DiskPartition part = new SystemMonitorDTO.DiskPartition();
        part.setName("disk0");
        part.setType("apfs");
        part.setMountPoint("/Volumes/Data");
        part.setTotalSpace(1000L);
        part.setUsedSpace(500L);
        part.setFreeSpace(500L);
        part.setUsagePercentage(50.0);
        part.setTotalSpaceFormatted("1000 GB");
        part.setUsedSpaceFormatted("500 GB");
        part.setFreeSpaceFormatted("500 GB");

        assertEquals("disk0", part.getName());
        assertEquals("apfs", part.getType());
        assertEquals("/Volumes/Data", part.getMountPoint());
        assertEquals(1000L, part.getTotalSpace());
        assertEquals(500L, part.getUsedSpace());
        assertEquals(500L, part.getFreeSpace());
        assertEquals(50.0, part.getUsagePercentage());
        assertEquals("1000 GB", part.getTotalSpaceFormatted());
        assertEquals("500 GB", part.getUsedSpaceFormatted());
        assertEquals("500 GB", part.getFreeSpaceFormatted());
    }

    @Test
    void testDiskPartitionDefaultConstructor() {
        SystemMonitorDTO.DiskPartition part = new SystemMonitorDTO.DiskPartition();
        assertNull(part.getName());
        assertEquals(0L, part.getTotalSpace());
    }

    // ==================== SystemMonitorDTO.JvmInfo ====================

    @Test
    void testJvmInfoGetterSetter() {
        SystemMonitorDTO.JvmInfo jvm = new SystemMonitorDTO.JvmInfo();
        jvm.setJavaVersion("17.0.8");
        jvm.setJavaVendor("Oracle");
        jvm.setJvmName("OpenJDK 64-Bit Server VM");
        jvm.setJvmVersion("17.0.8+7");
        jvm.setMaxMemory(512L * 1024 * 1024);
        jvm.setTotalMemory(256L * 1024 * 1024);
        jvm.setUsedMemory(128L * 1024 * 1024);
        jvm.setFreeMemory(128L * 1024 * 1024);
        jvm.setMemoryUsagePercentage(50.0);
        jvm.setMaxMemoryFormatted("512 MB");
        jvm.setTotalMemoryFormatted("256 MB");
        jvm.setUsedMemoryFormatted("128 MB");
        jvm.setFreeMemoryFormatted("128 MB");
        jvm.setStartTime(System.currentTimeMillis());
        jvm.setUptime(3600000L);
        jvm.setUptimeFormatted("1小时");

        assertEquals("17.0.8", jvm.getJavaVersion());
        assertEquals("Oracle", jvm.getJavaVendor());
        assertEquals("OpenJDK 64-Bit Server VM", jvm.getJvmName());
        assertEquals("17.0.8+7", jvm.getJvmVersion());
        assertEquals(512L * 1024 * 1024, jvm.getMaxMemory());
        assertEquals(256L * 1024 * 1024, jvm.getTotalMemory());
        assertEquals(128L * 1024 * 1024, jvm.getUsedMemory());
        assertEquals(128L * 1024 * 1024, jvm.getFreeMemory());
        assertEquals(50.0, jvm.getMemoryUsagePercentage());
        assertEquals("512 MB", jvm.getMaxMemoryFormatted());
        assertEquals("256 MB", jvm.getTotalMemoryFormatted());
        assertEquals("128 MB", jvm.getUsedMemoryFormatted());
        assertEquals("128 MB", jvm.getFreeMemoryFormatted());
        assertTrue(jvm.getStartTime() > 0);
        assertEquals(3600000L, jvm.getUptime());
        assertEquals("1小时", jvm.getUptimeFormatted());
    }

    @Test
    void testJvmInfoDefaultConstructor() {
        SystemMonitorDTO.JvmInfo jvm = new SystemMonitorDTO.JvmInfo();
        assertNull(jvm.getJavaVersion());
        assertEquals(0L, jvm.getMaxMemory());
    }

    // ==================== SystemMonitorDTO.NetworkInfo ====================

    @Test
    void testNetworkInfoGetterSetter() {
        SystemMonitorDTO.NetworkInfo network = new SystemMonitorDTO.NetworkInfo();
        network.setTotalBytesReceived(1000000L);
        network.setTotalBytesSent(500000L);

        SystemMonitorDTO.NetworkInterface iface = new SystemMonitorDTO.NetworkInterface();
        iface.setName("en0");
        iface.setDisplayName("Wi-Fi");
        iface.setIpAddresses(new String[]{"192.168.1.100"});
        iface.setMacAddress("AA:BB:CC:DD:EE:FF");
        iface.setBytesReceived(500000L);
        iface.setBytesSent(250000L);
        iface.setPacketsReceived(1000L);
        iface.setPacketsSent(500L);
        iface.setUp(true);
        iface.setSpeed(1000000000L);
        network.setInterfaces(Arrays.asList(iface));

        assertEquals(1000000L, network.getTotalBytesReceived());
        assertEquals(500000L, network.getTotalBytesSent());
        assertNotNull(network.getInterfaces());
        assertEquals(1, network.getInterfaces().size());
    }

    @Test
    void testNetworkInfoDefaultConstructor() {
        SystemMonitorDTO.NetworkInfo network = new SystemMonitorDTO.NetworkInfo();
        assertNull(network.getInterfaces());
        assertEquals(0L, network.getTotalBytesReceived());
    }

    // ==================== SystemMonitorDTO.NetworkInterface ====================

    @Test
    void testNetworkInterfaceGetterSetter() {
        SystemMonitorDTO.NetworkInterface iface = new SystemMonitorDTO.NetworkInterface();
        iface.setName("eth0");
        iface.setDisplayName("Ethernet");
        iface.setIpAddresses(new String[]{"10.0.0.1", "10.0.0.2"});
        iface.setMacAddress("11:22:33:44:55:66");
        iface.setBytesReceived(1000000L);
        iface.setBytesSent(500000L);
        iface.setPacketsReceived(2000L);
        iface.setPacketsSent(1000L);
        iface.setUp(true);
        iface.setSpeed(10000000L);

        assertEquals("eth0", iface.getName());
        assertEquals("Ethernet", iface.getDisplayName());
        assertArrayEquals(new String[]{"10.0.0.1", "10.0.0.2"}, iface.getIpAddresses());
        assertEquals("11:22:33:44:55:66", iface.getMacAddress());
        assertEquals(1000000L, iface.getBytesReceived());
        assertEquals(500000L, iface.getBytesSent());
        assertEquals(2000L, iface.getPacketsReceived());
        assertEquals(1000L, iface.getPacketsSent());
        assertTrue(iface.isUp());
        assertEquals(10000000L, iface.getSpeed());
    }

    @Test
    void testNetworkInterfaceDefaultConstructor() {
        SystemMonitorDTO.NetworkInterface iface = new SystemMonitorDTO.NetworkInterface();
        assertNull(iface.getName());
        assertFalse(iface.isUp());
        assertEquals(0L, iface.getSpeed());
    }

    // ==================== SystemMonitorDTO.AllMonitorData ====================

    @Test
    void testAllMonitorDataGetterSetter() {
        SystemMonitorDTO.AllMonitorData data = new SystemMonitorDTO.AllMonitorData();
        data.setSystemInfo(new SystemMonitorDTO.SystemInfo());
        data.setMemoryInfo(new SystemMonitorDTO.MemoryInfo());
        data.setCpuInfo(new SystemMonitorDTO.CpuInfo());
        data.setDiskInfo(new SystemMonitorDTO.DiskInfo());
        data.setJvmInfo(new SystemMonitorDTO.JvmInfo());
        data.setNetworkInfo(new SystemMonitorDTO.NetworkInfo());
        data.setTimestamp(System.currentTimeMillis());

        assertNotNull(data.getSystemInfo());
        assertNotNull(data.getMemoryInfo());
        assertNotNull(data.getCpuInfo());
        assertNotNull(data.getDiskInfo());
        assertNotNull(data.getJvmInfo());
        assertNotNull(data.getNetworkInfo());
        assertTrue(data.getTimestamp() > 0);
    }

    @Test
    void testAllMonitorDataDefaultConstructor() {
        SystemMonitorDTO.AllMonitorData data = new SystemMonitorDTO.AllMonitorData();
        assertNull(data.getSystemInfo());
        assertNull(data.getMemoryInfo());
        assertNull(data.getCpuInfo());
        assertNull(data.getDiskInfo());
        assertNull(data.getJvmInfo());
        assertNull(data.getNetworkInfo());
        assertEquals(0L, data.getTimestamp());
    }
}
