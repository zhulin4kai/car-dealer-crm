package com.autodealer.crm.modules.sales.lead.application.internal;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueMapper;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueRemarkMapper;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClueRemark;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueRemarkQuery;
import com.autodealer.crm.modules.sales.lead.application.internal.ClueRemarkServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void testSaveClueRemark() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(1);
        query.setNoteContent("Test note");
        query.setNoteWay(1);

            when(currentUserProvider.getCurrentUserId()).thenReturn(100);
            when(currentUserProvider.getDataScopeUserId()).thenReturn(100);
            when(tClueMapper.selectScopedByPrimaryKey(1, 100)).thenReturn(clue(1, 100));
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

    @Test
    void testSaveClueRemarkWithDifferentNoteWay() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(2);
        query.setNoteContent("Phone call note");
        query.setNoteWay(2);

            when(currentUserProvider.getCurrentUserId()).thenReturn(200);
            when(currentUserProvider.getDataScopeUserId()).thenReturn(200);
            when(tClueMapper.selectScopedByPrimaryKey(2, 200)).thenReturn(clue(2, 200));
            when(tClueRemarkMapper.insertSelective(any(TClueRemark.class))).thenReturn(1);

            int result = clueRemarkService.saveClueRemark(query);

            assertEquals(1, result);
            verify(tClueRemarkMapper).insertSelective(argThat(remark ->
                    remark.getCreateBy().equals(200) &&
                            remark.getNoteWay().equals(2)
            ));
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

        PageInfo<TClueRemark> result = clueRemarkService.getClueRemarkByPage(1, 10, query);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(tClueRemarkMapper).selectClueRemarkByPage(query);
    }

    @Test
    void testGetClueRemarkByPageEmpty() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(999);

        when(tClueRemarkMapper.selectClueRemarkByPage(any(ClueRemarkQuery.class))).thenReturn(Collections.emptyList());

        PageInfo<TClueRemark> result = clueRemarkService.getClueRemarkByPage(1, 10, query);

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

        PageInfo<TClueRemark> result = clueRemarkService.getClueRemarkByPage(2, 10, query);

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

    private TClue clue(Integer id, Integer ownerId) {
        TClue clue = new TClue();
        clue.setId(id);
        clue.setOwnerId(ownerId);
        return clue;
    }
}
