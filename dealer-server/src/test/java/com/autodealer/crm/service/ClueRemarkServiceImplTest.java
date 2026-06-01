package com.autodealer.crm.service;

import com.autodealer.crm.mapper.TClueRemarkMapper;
import com.autodealer.crm.model.TClueRemark;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.ClueRemarkQuery;
import com.autodealer.crm.service.impl.ClueRemarkServiceImpl;
import com.autodealer.crm.util.JWTUtils;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClueRemarkServiceImplTest {

    @InjectMocks
    private ClueRemarkServiceImpl clueRemarkService;

    @Mock
    private TClueRemarkMapper tClueRemarkMapper;

    @Test
    void testSaveClueRemark() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(1);
        query.setNoteContent("Test note");
        query.setNoteWay(1);
        query.setToken("valid-token");

        TUser mockUser = new TUser();
        mockUser.setId(100);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(mockUser);
            when(tClueRemarkMapper.insertSelective(any(TClueRemark.class))).thenReturn(1);

            int result = clueRemarkService.saveClueRemark(query);

            assertEquals(1, result);
            verify(tClueRemarkMapper).insertSelective(argThat(remark ->
                    remark.getClueId().equals(1) &&
                            remark.getNoteContent().equals("Test note") &&
                            remark.getNoteWay().equals(1) &&
                            remark.getCreateBy().equals(100) &&
                            remark.getCreateTime() != null
            ));
        }
    }

    @Test
    void testSaveClueRemarkWithDifferentNoteWay() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(2);
        query.setNoteContent("Phone call note");
        query.setNoteWay(2);
        query.setToken("valid-token");

        TUser mockUser = new TUser();
        mockUser.setId(200);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(mockUser);
            when(tClueRemarkMapper.insertSelective(any(TClueRemark.class))).thenReturn(1);

            int result = clueRemarkService.saveClueRemark(query);

            assertEquals(1, result);
            verify(tClueRemarkMapper).insertSelective(argThat(remark ->
                    remark.getCreateBy().equals(200) &&
                            remark.getNoteWay().equals(2)
            ));
        }
    }

    @Test
    void testGetClueRemarkByPage() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(1);

        List<TClueRemark> remarks = Arrays.asList(
                createClueRemark(1, 1, "Note 1"),
                createClueRemark(2, 1, "Note 2")
        );
        when(tClueRemarkMapper.selectClueRemarkByPage(any(ClueRemarkQuery.class))).thenReturn(remarks);

        PageInfo<TClueRemark> result = clueRemarkService.getClueRemarkByPage(1, query);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(tClueRemarkMapper).selectClueRemarkByPage(query);
    }

    @Test
    void testGetClueRemarkByPageEmpty() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(999);

        when(tClueRemarkMapper.selectClueRemarkByPage(any(ClueRemarkQuery.class))).thenReturn(Collections.emptyList());

        PageInfo<TClueRemark> result = clueRemarkService.getClueRemarkByPage(1, query);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetClueRemarkByPageMultiplePages() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(1);

        List<TClueRemark> remarks = Arrays.asList(
                createClueRemark(1, 1, "Note 1"),
                createClueRemark(2, 1, "Note 2"),
                createClueRemark(3, 1, "Note 3")
        );
        when(tClueRemarkMapper.selectClueRemarkByPage(any(ClueRemarkQuery.class))).thenReturn(remarks);

        PageInfo<TClueRemark> result = clueRemarkService.getClueRemarkByPage(2, query);

        assertNotNull(result);
        assertEquals(3, result.getList().size());
    }

    private TClueRemark createClueRemark(Integer id, Integer clueId, String content) {
        TClueRemark remark = new TClueRemark();
        remark.setId(id);
        remark.setClueId(clueId);
        remark.setNoteContent(content);
        remark.setNoteWay(1);
        remark.setCreateBy(100);
        return remark;
    }
}
