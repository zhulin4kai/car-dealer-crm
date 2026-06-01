package com.autodealer.crm.service;

import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TSystemMapper;
import com.autodealer.crm.model.TSystem;
import com.autodealer.crm.service.impl.SystemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemServiceImplTest {

    @InjectMocks
    private SystemServiceImpl systemService;

    @Mock
    private TSystemMapper systemMapper;

    @Mock
    private RedisManager redisManager;

    @Test
    void testGetAllList() {
        List<TSystem> systems = Arrays.asList(
                createSystem(1, "SYS001", "System A"),
                createSystem(2, "SYS002", "System B")
        );
        when(systemMapper.selectAll()).thenReturn(systems);

        List<TSystem> result = systemService.getAllList();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(systemMapper).selectAll();
    }

    @Test
    void testGetAllListEmpty() {
        when(systemMapper.selectAll()).thenReturn(Collections.emptyList());

        List<TSystem> result = systemService.getAllList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetById() {
        TSystem system = createSystem(1, "SYS001", "System A");
        when(systemMapper.selectById(1)).thenReturn(system);

        TSystem result = systemService.getById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("SYS001", result.getSystemCode());
        assertEquals("System A", result.getName());
    }

    @Test
    void testGetByIdNotFound() {
        when(systemMapper.selectById(999)).thenReturn(null);

        TSystem result = systemService.getById(999);

        assertNull(result);
    }

    @Test
    void testCreate() {
        TSystem system = new TSystem();
        system.setSystemCode("NEW_SYS");
        system.setName("New System");
        when(systemMapper.insert(any(TSystem.class))).thenReturn(1);

        systemService.create(system);

        assertNotNull(system.getCreateTime());
        verify(systemMapper).insert(system);
    }

    @Test
    void testUpdate() {
        TSystem system = new TSystem();
        system.setSystemCode("UPDATED_SYS");
        system.setName("Updated System");
        when(systemMapper.update(any(TSystem.class))).thenReturn(1);

        systemService.update(1, system);

        assertEquals(1, system.getId());
        assertNotNull(system.getEditTime());
        verify(systemMapper).update(system);
    }

    @Test
    void testDelete() {
        when(systemMapper.deleteById(1)).thenReturn(1);

        systemService.delete(1);

        verify(systemMapper).deleteById(1);
    }

    @Test
    void testBatchDelete() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(systemMapper.batchDelete(ids)).thenReturn(3);

        systemService.batchDelete(ids);

        verify(systemMapper).batchDelete(ids);
    }

    @Test
    void testToggleStatus() {
        when(systemMapper.updateStatus(1, "Y")).thenReturn(1);

        systemService.toggleStatus(1, "Y");

        verify(systemMapper).updateStatus(1, "Y");
    }

    private TSystem createSystem(Integer id, String code, String name) {
        TSystem system = new TSystem();
        system.setId(id);
        system.setSystemCode(code);
        system.setName(name);
        system.setSite("http://example.com");
        system.setTitle("Test Title");
        system.setDescription("Test Description");
        system.setIsopen("Y");
        system.setCreateTime(LocalDateTime.now());
        return system;
    }
}
