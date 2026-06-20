package com.autodealer.crm.config.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.autodealer.crm.config.converter.ClueExcelConverter;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.result.ClueExcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadDataListenerTest {

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private ClueExcelConverter clueExcelConverter;

    @Mock
    private AnalysisContext analysisContext;

    private Integer operatorId;

    @BeforeEach
    void setUp() {
        operatorId = 1;
    }

    @Test
    void testConstructor() {
        UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);
        assertNotNull(listener);
    }

    @Test
    void testInvokeSingleRecord() {
        ClueExcel clueExcel = new ClueExcel();
        clueExcel.setFullName("张三");

        TClue tClue = new TClue();
        tClue.setFullName("张三");

        when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);

            UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);
            listener.invoke(clueExcel, analysisContext);

            verify(clueExcelConverter).convertToTClue(clueExcel);
            assertEquals("张三", tClue.getFullName());
            assertNotNull(tClue.getCreateTime());
            assertEquals(1, tClue.getCreateBy());
    }

    @Test
    void testInvokeMultipleRecords() {
            UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);

            for (int i = 0; i < 5; i++) {
                ClueExcel clueExcel = new ClueExcel();
                clueExcel.setFullName("用户" + i);

                TClue tClue = new TClue();
                tClue.setFullName("用户" + i);

                when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);
                listener.invoke(clueExcel, analysisContext);
            }

            verify(clueExcelConverter, times(5)).convertToTClue(any(ClueExcel.class));
    }

    @Test
    void testInvokeTriggersBatchSaveAt100Records() {
            UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);

            for (int i = 0; i < 100; i++) {
                ClueExcel clueExcel = new ClueExcel();
                clueExcel.setFullName("用户" + i);

                TClue tClue = new TClue();
                tClue.setFullName("用户" + i);

                when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);
                listener.invoke(clueExcel, analysisContext);
            }

            verify(tClueMapper, times(1)).saveClue(anyList());
    }

    @Test
    void testDoAfterAllAnalysedSavesRemainingData() {
            UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);

            ClueExcel clueExcel = new ClueExcel();
            clueExcel.setFullName("张三");

            TClue tClue = new TClue();
            tClue.setFullName("张三");

            when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);
            listener.invoke(clueExcel, analysisContext);

            listener.doAfterAllAnalysed(analysisContext);

            verify(tClueMapper, times(1)).saveClue(anyList());
    }

    @Test
    void testDoAfterAllAnalysedWithEmptyCache() {
        UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);

        listener.doAfterAllAnalysed(analysisContext);

        verify(tClueMapper, times(1)).saveClue(anyList());
    }

    @Test
    void testInvokeSetsCreateTimeAndCreateBy() {
        ClueExcel clueExcel = new ClueExcel();
        clueExcel.setFullName("李四");

        TClue tClue = new TClue();

        when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);

            UploadDataListener listener = new UploadDataListener(tClueMapper, 42, clueExcelConverter);
            listener.invoke(clueExcel, analysisContext);

            assertNotNull(tClue.getCreateTime());
            assertEquals(42, tClue.getCreateBy());
    }

    @Test
    void testMultipleBatchSaves() {
            UploadDataListener listener = new UploadDataListener(tClueMapper, operatorId, clueExcelConverter);

            for (int i = 0; i < 250; i++) {
                ClueExcel clueExcel = new ClueExcel();
                clueExcel.setFullName("用户" + i);

                TClue tClue = new TClue();
                tClue.setFullName("用户" + i);

                when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);
                listener.invoke(clueExcel, analysisContext);
            }

            verify(tClueMapper, times(2)).saveClue(anyList());
    }
}
