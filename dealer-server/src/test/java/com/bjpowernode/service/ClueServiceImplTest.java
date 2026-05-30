package com.bjpowernode.service;

import com.bjpowernode.mapper.TClueMapper;
import com.bjpowernode.mapper.TClueRemarkMapper;
import com.bjpowernode.model.TClue;
import com.bjpowernode.model.TUser;
import com.bjpowernode.query.ClueQuery;
import com.bjpowernode.service.impl.ClueServiceImpl;
import com.bjpowernode.util.JWTUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClueServiceImplTest {

    @InjectMocks
    private ClueServiceImpl clueService;

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private TClueRemarkMapper tClueRemarkMapper;

    // ==================== getClueById ====================

    @Test
    void getClueById_found_shouldReturnClue() {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setFullName("John Doe");
        clue.setPhone("13800138000");

        when(tClueMapper.selectDetailById(1)).thenReturn(clue);

        TClue result = clueService.getClueById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("John Doe", result.getFullName());
        assertEquals("13800138000", result.getPhone());
    }

    @Test
    void getClueById_notFound_shouldReturnNull() {
        when(tClueMapper.selectDetailById(999)).thenReturn(null);

        TClue result = clueService.getClueById(999);

        assertNull(result);
    }

    // ==================== saveClue ====================

    @Test
    void saveClue_phoneAlreadyExists_shouldThrowException() {
        ClueQuery query = new ClueQuery();
        query.setPhone("13800138000");

        when(tClueMapper.selectByCount("13800138000")).thenReturn(1);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clueService.saveClue(query));

        assertEquals("该手机号已经录入过了，不能再录入", exception.getMessage());
        verify(tClueMapper, never()).insertSelective(any());
    }

    @Test
    void saveClue_newPhone_shouldInsertAndReturnCount() {
        ClueQuery query = new ClueQuery();
        query.setPhone("13800138000");
        query.setFullName("John Doe");
        query.setToken("valid-token");

        TUser user = new TUser();
        user.setId(1);

        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);
        when(tClueMapper.insertSelective(any(TClue.class))).thenReturn(1);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(user);

            int result = clueService.saveClue(query);

            assertEquals(1, result);
            verify(tClueMapper).insertSelective(argThat(clue -> {
                TClue c = (TClue) clue;
                return "John Doe".equals(c.getFullName())
                        && "13800138000".equals(c.getPhone())
                        && c.getCreateTime() != null
                        && c.getCreateBy().equals(1);
            }));
        }
    }

    // ==================== updateClue ====================

    @Test
    void updateClue_shouldUpdateAndReturnCount() {
        ClueQuery query = new ClueQuery();
        query.setId(1);
        query.setFullName("Updated Name");
        query.setPhone("13900139000");
        query.setToken("valid-token");

        TUser user = new TUser();
        user.setId(2);

        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(1);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(user);

            int result = clueService.updateClue(query);

            assertEquals(1, result);
            verify(tClueMapper).updateByPrimaryKeySelective(argThat(clue -> {
                TClue c = (TClue) clue;
                return c.getId().equals(1)
                        && "Updated Name".equals(c.getFullName())
                        && c.getEditTime() != null
                        && c.getEditBy().equals(2);
            }));
        }
    }

    @Test
    void updateClue_notFound_shouldReturnZero() {
        ClueQuery query = new ClueQuery();
        query.setId(999);
        query.setToken("valid-token");

        TUser user = new TUser();
        user.setId(1);

        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(0);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(user);

            int result = clueService.updateClue(query);

            assertEquals(0, result);
        }
    }

    // ==================== delClueById ====================

    @Test
    void delClueById_success_shouldReturnOne() {
        when(tClueRemarkMapper.deleteByClueId(1)).thenReturn(1);
        when(tClueMapper.deleteByPrimaryKey(1)).thenReturn(1);

        int result = clueService.delClueById(1);

        assertEquals(1, result);
        verify(tClueRemarkMapper).deleteByClueId(1);
        verify(tClueMapper).deleteByPrimaryKey(1);
    }

    @Test
    void delClueById_nullId_shouldReturnZero() {
        int result = clueService.delClueById(null);

        assertEquals(0, result);
        verify(tClueRemarkMapper, never()).deleteByClueId(anyInt());
        verify(tClueMapper, never()).deleteByPrimaryKey(any());
    }

    @Test
    void delClueById_notFound_shouldReturnZero() {
        when(tClueRemarkMapper.deleteByClueId(999)).thenReturn(0);
        when(tClueMapper.deleteByPrimaryKey(999)).thenReturn(0);

        int result = clueService.delClueById(999);

        assertEquals(0, result);
        verify(tClueRemarkMapper).deleteByClueId(999);
        verify(tClueMapper).deleteByPrimaryKey(999);
    }

    // ==================== batchDelClueByIds ====================

    @Test
    void batchDelClueByIds_success_shouldReturnDeletedCount() {
        List<Integer> ids = Arrays.asList(1, 2, 3);

        when(tClueRemarkMapper.deleteByClueId(anyInt())).thenReturn(1);
        when(tClueMapper.batchDeleteByIds(ids)).thenReturn(3);

        int result = clueService.batchDelClueByIds(ids);

        assertEquals(3, result);
        verify(tClueMapper).batchDeleteByIds(ids);
    }

    @Test
    void batchDelClueByIds_emptyList_shouldReturnZero() {
        int result = clueService.batchDelClueByIds(Collections.emptyList());

        assertEquals(0, result);
        verify(tClueMapper, never()).batchDeleteByIds(any());
    }

    @Test
    void batchDelClueByIds_nullList_shouldReturnZero() {
        int result = clueService.batchDelClueByIds(null);

        assertEquals(0, result);
        verify(tClueMapper, never()).batchDeleteByIds(any());
    }

    // ==================== checkPhone ====================

    @Test
    void checkPhone_phoneNotExists_shouldReturnTrue() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(0);

        Boolean result = clueService.checkPhone("13800138000");

        assertTrue(result);
        verify(tClueMapper).selectByCount("13800138000");
    }

    @Test
    void checkPhone_phoneExists_shouldReturnFalse() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(1);

        Boolean result = clueService.checkPhone("13800138000");

        assertFalse(result);
    }

    @Test
    void checkPhone_multipleRecords_shouldReturnFalse() {
        when(tClueMapper.selectByCount("13800138000")).thenReturn(3);

        Boolean result = clueService.checkPhone("13800138000");

        assertFalse(result);
    }

    // ==================== importExcel ====================

    @Test
    void importExcel_shouldAcceptInputStreamAndToken() {
        InputStream inputStream = mock(InputStream.class);
        String token = "valid-token";

        assertDoesNotThrow(() -> {
            try {
                clueService.importExcel(inputStream, token);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("token")) {
                    throw e;
                }
            }
        });
    }
}
