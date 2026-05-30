package com.bjpowernode.web;

import com.bjpowernode.dto.SystemMonitorDTO;
import com.bjpowernode.service.SystemMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SystemMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemMonitorService systemMonitorService;

    @Test
    void getSystemInfo_returnsSystemInfo() throws Exception {
        SystemMonitorDTO.SystemInfo systemInfo = new SystemMonitorDTO.SystemInfo();
        systemInfo.setPlatform("macOS");
        systemInfo.setOsName("Mac OS X");
        systemInfo.setOsVersion("14.0");
        systemInfo.setArch("aarch64");
        systemInfo.setHostname("test-host");

        when(systemMonitorService.getSystemInfo()).thenReturn(systemInfo);

        mockMvc.perform(get("/api/monitor/system-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.platform").value("macOS"));
    }

    @Test
    void getMemoryInfo_returnsMemoryInfo() throws Exception {
        SystemMonitorDTO.MemoryInfo memoryInfo = new SystemMonitorDTO.MemoryInfo();
        memoryInfo.setTotalMemory(16L * 1024 * 1024 * 1024);
        memoryInfo.setUsedMemory(8L * 1024 * 1024 * 1024);
        memoryInfo.setAvailableMemory(8L * 1024 * 1024 * 1024);
        memoryInfo.setUsagePercentage(50.0);

        when(systemMonitorService.getMemoryInfo()).thenReturn(memoryInfo);

        mockMvc.perform(get("/api/monitor/memory-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usagePercentage").value(50.0));
    }

    @Test
    void getCpuInfo_returnsCpuInfo() throws Exception {
        SystemMonitorDTO.CpuInfo cpuInfo = new SystemMonitorDTO.CpuInfo();
        cpuInfo.setName("Apple M1");
        cpuInfo.setVendor("Apple");
        cpuInfo.setPhysicalProcessors(8);
        cpuInfo.setLogicalProcessors(8);
        cpuInfo.setSystemCpuLoad(25.5);

        when(systemMonitorService.getCpuInfo()).thenReturn(cpuInfo);

        mockMvc.perform(get("/api/monitor/cpu-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Apple M1"))
                .andExpect(jsonPath("$.data.physicalProcessors").value(8));
    }

    @Test
    void getDiskInfo_returnsDiskInfo() throws Exception {
        SystemMonitorDTO.DiskInfo diskInfo = new SystemMonitorDTO.DiskInfo();
        diskInfo.setTotalSpace(500L * 1024 * 1024 * 1024);
        diskInfo.setUsedSpace(250L * 1024 * 1024 * 1024);
        diskInfo.setFreeSpace(250L * 1024 * 1024 * 1024);
        diskInfo.setUsagePercentage(50.0);
        diskInfo.setPartitions(Collections.emptyList());

        when(systemMonitorService.getDiskInfo()).thenReturn(diskInfo);

        mockMvc.perform(get("/api/monitor/disk-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usagePercentage").value(50.0));
    }

    @Test
    void getNetworkInfo_returnsNetworkInfo() throws Exception {
        SystemMonitorDTO.NetworkInfo networkInfo = new SystemMonitorDTO.NetworkInfo();
        networkInfo.setTotalBytesReceived(1024000);
        networkInfo.setTotalBytesSent(512000);
        networkInfo.setInterfaces(Collections.emptyList());

        when(systemMonitorService.getNetworkInfo()).thenReturn(networkInfo);

        mockMvc.perform(get("/api/monitor/network-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalBytesReceived").value(1024000));
    }

    @Test
    void getJvmInfo_returnsJvmInfo() throws Exception {
        SystemMonitorDTO.JvmInfo jvmInfo = new SystemMonitorDTO.JvmInfo();
        jvmInfo.setJavaVersion("17.0.9");
        jvmInfo.setJvmName("OpenJDK 64-Bit Server VM");
        jvmInfo.setMaxMemory(4L * 1024 * 1024 * 1024);
        jvmInfo.setTotalMemory(256L * 1024 * 1024);
        jvmInfo.setUsedMemory(128L * 1024 * 1024);

        when(systemMonitorService.getJvmInfo()).thenReturn(jvmInfo);

        mockMvc.perform(get("/api/monitor/jvm-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javaVersion").value("17.0.9"));
    }

    @Test
    void getAllMonitorData_returnsAllData() throws Exception {
        SystemMonitorDTO.AllMonitorData allData = new SystemMonitorDTO.AllMonitorData();
        SystemMonitorDTO.SystemInfo systemInfo = new SystemMonitorDTO.SystemInfo();
        systemInfo.setPlatform("macOS");
        allData.setSystemInfo(systemInfo);
        allData.setTimestamp(System.currentTimeMillis());

        when(systemMonitorService.getAllMonitorData()).thenReturn(allData);

        mockMvc.perform(get("/api/monitor/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.systemInfo.platform").value("macOS"));
    }
}
