package com.autodealer.crm.config.converter;

import com.alibaba.excel.metadata.data.ReadCellData;
import com.autodealer.crm.DealerCRMApplication;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.result.DicEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConverterTest {

    @BeforeEach
    void setUp() {
        DealerCRMApplication.cacheMap.clear();
    }

    @Test
    void testStateConverterWithMatchingValue() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(27);
        dicValue.setTypeValue("已联系");
        List<TDicValue> list = Collections.singletonList(dicValue);
        DealerCRMApplication.cacheMap.put(DicEnum.STATE.getCode(), list);

        StateConverter converter = new StateConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("已联系");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(27, result);
    }

    @Test
    void testStateConverterWithNoMatch() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(27);
        dicValue.setTypeValue("已联系");
        List<TDicValue> list = Collections.singletonList(dicValue);
        DealerCRMApplication.cacheMap.put(DicEnum.STATE.getCode(), list);

        StateConverter converter = new StateConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("未联系");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testStateConverterWithNullCache() throws Exception {
        StateConverter converter = new StateConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("已联系");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testAppellationConverterWithMatchingValue() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(18);
        dicValue.setTypeValue("先生");
        List<TDicValue> list = Collections.singletonList(dicValue);
        DealerCRMApplication.cacheMap.put(DicEnum.APPELLATION.getCode(), list);

        AppellationConverter converter = new AppellationConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("先生");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(18, result);
    }

    @Test
    void testAppellationConverterWithNullCache() throws Exception {
        AppellationConverter converter = new AppellationConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("女士");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testSourceConverterWithMatchingValue() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(3);
        dicValue.setTypeValue("车展会");
        List<TDicValue> list = Collections.singletonList(dicValue);
        DealerCRMApplication.cacheMap.put(DicEnum.SOURCE.getCode(), list);

        SourceConverter converter = new SourceConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("车展会");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(3, result);
    }

    @Test
    void testSourceConverterWithNullCache() throws Exception {
        SourceConverter converter = new SourceConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("网络广告");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testNeedLoanConverterWithMatchingValue() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(49);
        dicValue.setTypeValue("需要");
        List<TDicValue> list = Collections.singletonList(dicValue);
        DealerCRMApplication.cacheMap.put(DicEnum.NEEDLOAN.getCode(), list);

        NeedLoanConverter converter = new NeedLoanConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("需要");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(49, result);
    }

    @Test
    void testNeedLoanConverterWithNullCache() throws Exception {
        NeedLoanConverter converter = new NeedLoanConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("不需要");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testIntentionStateConverterWithMatchingValue() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(48);
        dicValue.setTypeValue("意向不明");
        List<TDicValue> list = Collections.singletonList(dicValue);
        DealerCRMApplication.cacheMap.put(DicEnum.INTENTIONSTATE.getCode(), list);

        IntentionStateConverter converter = new IntentionStateConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("意向不明");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(48, result);
    }

    @Test
    void testIntentionStateConverterWithNullCache() throws Exception {
        IntentionStateConverter converter = new IntentionStateConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("有意向");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testIntentionProductConverterWithMatchingValue() throws Exception {
        TProduct product = new TProduct();
        product.setId(2);
        product.setName("比亚迪e2");
        List<TProduct> list = Collections.singletonList(product);
        DealerCRMApplication.cacheMap.put(DicEnum.PRODUCT.getCode(), list);

        IntentionProductConverter converter = new IntentionProductConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("比亚迪e2");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(2, result);
    }

    @Test
    void testIntentionProductConverterWithNullCache() throws Exception {
        IntentionProductConverter converter = new IntentionProductConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("秦PLUS EV");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }

    @Test
    void testIntentionProductConverterWithNoMatch() throws Exception {
        TProduct product = new TProduct();
        product.setId(2);
        product.setName("比亚迪e2");
        List<TProduct> list = Collections.singletonList(product);
        DealerCRMApplication.cacheMap.put(DicEnum.PRODUCT.getCode(), list);

        IntentionProductConverter converter = new IntentionProductConverter();
        ReadCellData<?> cellData = mock(ReadCellData.class);
        when(cellData.getStringValue()).thenReturn("秦PLUS EV");

        Integer result = converter.convertToJavaData(cellData, null, null);

        assertEquals(-1, result);
    }
}
