package com.autodealer.crm.service;

import com.autodealer.crm.dto.SystemMonitorDTO;
import com.autodealer.crm.service.impl.SystemMonitorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SystemMonitorServiceImplTest {

    @InjectMocks
    private SystemMonitorServiceImpl systemMonitorService;

    @Test
    void testGetJvmInfo() {
        SystemMonitorDTO.JvmInfo result = systemMonitorService.getJvmInfo();

        assertNotNull(result);
        assertNotNull(result.getJavaVersion());
        assertNotNull(result.getJavaVendor());
        assertNotNull(result.getJvmName());
        assertNotNull(result.getJvmVersion());
        assertTrue(result.getMaxMemory() > 0);
        assertTrue(result.getTotalMemory() > 0);
        assertTrue(result.getUsedMemory() >= 0);
        assertTrue(result.getFreeMemory() >= 0);
        assertTrue(result.getMemoryUsagePercentage() >= 0);
        assertNotNull(result.getMaxMemoryFormatted());
        assertNotNull(result.getTotalMemoryFormatted());
        assertNotNull(result.getUsedMemoryFormatted());
        assertNotNull(result.getFreeMemoryFormatted());
        assertTrue(result.getStartTime() > 0);
        assertTrue(result.getUptime() >= 0);
        assertNotNull(result.getUptimeFormatted());
    }

    @Test
    void testGetJvmInfoMemoryConsistency() {
        SystemMonitorDTO.JvmInfo result = systemMonitorService.getJvmInfo();

        assertNotNull(result);
        assertEquals(result.getTotalMemory() - result.getFreeMemory(), result.getUsedMemory());
        assertTrue(result.getMaxMemory() >= result.getTotalMemory());
    }

    @Test
    void testGetSystemInfo() {
        SystemMonitorDTO.SystemInfo result = systemMonitorService.getSystemInfo();

        assertNotNull(result);
        assertNotNull(result.getPlatform());
        assertNotNull(result.getOsName());
        assertNotNull(result.getOsVersion());
        assertNotNull(result.getArch());
        assertTrue(result.getUptime() >= 0);
        assertNotNull(result.getUptimeFormatted());
    }

    @Test
    void testGetMemoryInfo() {
        SystemMonitorDTO.MemoryInfo result = systemMonitorService.getMemoryInfo();

        assertNotNull(result);
        assertTrue(result.getTotalMemory() > 0);
        assertTrue(result.getAvailableMemory() > 0);
        assertTrue(result.getUsedMemory() >= 0);
        assertTrue(result.getUsagePercentage() >= 0);
        assertNotNull(result.getTotalMemoryFormatted());
        assertNotNull(result.getAvailableMemoryFormatted());
        assertNotNull(result.getUsedMemoryFormatted());
    }

    @Test
    void testGetMemoryInfoConsistency() {
        SystemMonitorDTO.MemoryInfo result = systemMonitorService.getMemoryInfo();

        assertNotNull(result);
        assertEquals(result.getTotalMemory() - result.getAvailableMemory(), result.getUsedMemory());
    }

    @Test
    void testGetDiskInfo() {
        SystemMonitorDTO.DiskInfo result = systemMonitorService.getDiskInfo();

        assertNotNull(result);
        assertNotNull(result.getPartitions());
        assertTrue(result.getTotalSpace() >= 0);
        assertTrue(result.getUsedSpace() >= 0);
        assertTrue(result.getFreeSpace() >= 0);
        assertTrue(result.getUsagePercentage() >= 0);
    }

    @Test
    void testGetAllMonitorData() {
        SystemMonitorDTO.AllMonitorData result = systemMonitorService.getAllMonitorData();

        assertNotNull(result);
        assertNotNull(result.getSystemInfo());
        assertNotNull(result.getMemoryInfo());
        assertNotNull(result.getCpuInfo());
        assertNotNull(result.getDiskInfo());
        assertNotNull(result.getJvmInfo());
        assertNotNull(result.getNetworkInfo());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void testGetAllMonitorDataTimestamp() {
        long before = System.currentTimeMillis();
        SystemMonitorDTO.AllMonitorData result = systemMonitorService.getAllMonitorData();
        long after = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.getTimestamp() >= before);
        assertTrue(result.getTimestamp() <= after);
    }
}
