package com.autodealer.crm.config.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.autodealer.crm.config.converter.ClueExcelConverter;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.ClueExcel;
import com.autodealer.crm.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    private String token;

    @BeforeEach
    void setUp() {
        token = "test-token";
    }

    @Test
    void testConstructor() {
        UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);
        assertNotNull(listener);
    }

    @Test
    void testInvokeSingleRecord() {
        TUser mockUser = new TUser();
        mockUser.setId(1);

        ClueExcel clueExcel = new ClueExcel();
        clueExcel.setFullName("张三");

        TClue tClue = new TClue();
        tClue.setFullName("张三");

        when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT(token)).thenReturn(mockUser);

            UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);
            listener.invoke(clueExcel, analysisContext);

            verify(clueExcelConverter).convertToTClue(clueExcel);
            assertEquals("张三", tClue.getFullName());
            assertNotNull(tClue.getCreateTime());
            assertEquals(1, tClue.getCreateBy());
        }
    }

    @Test
    void testInvokeMultipleRecords() {
        TUser mockUser = new TUser();
        mockUser.setId(1);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT(token)).thenReturn(mockUser);

            UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);

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
    }

    @Test
    void testInvokeTriggersBatchSaveAt100Records() {
        TUser mockUser = new TUser();
        mockUser.setId(1);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT(token)).thenReturn(mockUser);

            UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);

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
    }

    @Test
    void testDoAfterAllAnalysedSavesRemainingData() {
        TUser mockUser = new TUser();
        mockUser.setId(1);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT(token)).thenReturn(mockUser);

            UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);

            ClueExcel clueExcel = new ClueExcel();
            clueExcel.setFullName("张三");

            TClue tClue = new TClue();
            tClue.setFullName("张三");

            when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);
            listener.invoke(clueExcel, analysisContext);

            listener.doAfterAllAnalysed(analysisContext);

            verify(tClueMapper, times(1)).saveClue(anyList());
        }
    }

    @Test
    void testDoAfterAllAnalysedWithEmptyCache() {
        UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);

        listener.doAfterAllAnalysed(analysisContext);

        verify(tClueMapper, times(1)).saveClue(anyList());
    }

    @Test
    void testInvokeSetsCreateTimeAndCreateBy() {
        TUser mockUser = new TUser();
        mockUser.setId(42);

        ClueExcel clueExcel = new ClueExcel();
        clueExcel.setFullName("李四");

        TClue tClue = new TClue();

        when(clueExcelConverter.convertToTClue(clueExcel)).thenReturn(tClue);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT(token)).thenReturn(mockUser);

            UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);
            listener.invoke(clueExcel, analysisContext);

            assertNotNull(tClue.getCreateTime());
            assertEquals(42, tClue.getCreateBy());
        }
    }

    @Test
    void testMultipleBatchSaves() {
        TUser mockUser = new TUser();
        mockUser.setId(1);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT(token)).thenReturn(mockUser);

            UploadDataListener listener = new UploadDataListener(tClueMapper, token, clueExcelConverter);

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
}
