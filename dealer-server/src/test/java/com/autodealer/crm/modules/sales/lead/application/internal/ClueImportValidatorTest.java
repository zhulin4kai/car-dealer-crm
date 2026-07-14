package com.autodealer.crm.modules.sales.lead.application.internal;

import com.autodealer.crm.modules.sales.lead.application.api.dto.ImportContext;
import com.autodealer.crm.modules.commerce.catalog.application.api.dto.ProductSimpleDTO;
import com.autodealer.crm.modules.sales.activity.application.api.model.TActivity;
import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.sales.lead.application.api.result.ClueExcelRaw;
import com.autodealer.crm.modules.sales.lead.application.internal.ClueImportValidator.ValidatedClueImport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClueImportValidatorTest {

    private ClueImportValidator validator;
    private ImportContext context;

    @BeforeEach
    void setUp() {
        validator = new ClueImportValidator();
        context = buildTestContext();
    }

    // ==================== 空数据 ====================

    @Test
    void emptyList_shouldReturnZeroRows() {
        ValidatedClueImport result = validator.validateAndTransform(
                Collections.emptyList(), context, 1);

        assertEquals(0, result.getResult().getTotalRows());
        // 空工作表必须是明确的导入失败
        assertTrue(result.getResult().getFailedRows() > 0
                || !result.getResult().getErrors().isEmpty(),
                "空工作表必须返回错误信息");
        assertEquals(0, result.getResult().getImportedCount());
        assertTrue(result.getClues().isEmpty());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getReason().contains("空") || e.getReason().contains("没有数据")));
    }

    // ==================== 手机号校验 ====================

    @Test
    void validRow_shouldPassAndTransform() {
        ClueExcelRaw raw = buildValidRaw();
        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 42);

        assertEquals(1, result.getResult().getTotalRows());
        assertEquals(0, result.getResult().getFailedRows());
        assertEquals(1, result.getClues().size());
        assertEquals("13800138000", result.getClues().get(0).getPhone());
        assertEquals(10, result.getClues().get(0).getOwnerId());
        assertEquals(42, result.getClues().get(0).getCreateBy());
        assertNotNull(result.getClues().get(0).getCreateTime());
    }

    @Test
    void phoneWithSeparators_shouldNormalizeAndPass() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setPhone("138 0013-8000");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 42);

        assertEquals(0, result.getResult().getFailedRows());
        assertEquals("13800138000", result.getClues().get(0).getPhone());
    }

    @Test
    void emptyPhone_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setPhone("");
        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertTrue(result.getClues().isEmpty());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getColumn().equals("手机号") && e.getReason().contains("不能为空")));
    }

    @Test
    void invalidPhoneFormat_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setPhone("12345");
        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getColumn().equals("手机号") && e.getReason().contains("格式不正确")));
    }

    @Test
    void phoneWithFormula_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setPhone("=13800138000");
        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
    }

    @Test
    void duplicatePhoneInSameFile_shouldFail() {
        ClueExcelRaw raw1 = buildValidRaw();
        raw1.setPhone("13800138000");
        ClueExcelRaw raw2 = buildValidRaw();
        raw2.setPhone("13800138000");
        raw2.setFullName("另一个人");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw1, raw2), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertEquals(1, result.getResult().getValidRows());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getReason().contains("重复")));
    }

    @Test
    void duplicatePhoneInSameFile_afterNormalization_shouldFail() {
        ClueExcelRaw raw1 = buildValidRaw();
        raw1.setPhone("13800138000");
        ClueExcelRaw raw2 = buildValidRaw();
        raw2.setPhone("138 0013-8000");
        raw2.setFullName("另一个人");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw1, raw2), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertEquals(1, result.getResult().getValidRows());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getReason().contains("重复")));
    }

    // ==================== 负责人校验 ====================

    @Test
    void unknownOwner_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setOwnerName("不存在的人");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getColumn().equals("负责人") && e.getReason().contains("未找到")));
    }

    @Test
    void emptyOwner_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setOwnerName("");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
    }

    // ==================== 字典字段校验 ====================

    @Test
    void unknownDicValue_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setAppellation("未知称呼");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getColumn().equals("称呼") && e.getReason().contains("未找到")));
    }

    @Test
    void validDicValue_shouldTransform() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setAppellation("先生");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(0, result.getResult().getFailedRows());
        assertEquals(18, result.getClues().get(0).getAppellation());
    }

    @Test
    void emptyOptionalDicField_shouldPass() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setAppellation(null);
        raw.setState(null);
        raw.setSource(null);
        raw.setNeedLoan(null);
        raw.setIntentionState(null);
        raw.setIntentionProduct(null);

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(0, result.getResult().getFailedRows());
        assertNull(result.getClues().get(0).getAppellation());
        assertNull(result.getClues().get(0).getState());
    }

    // ==================== 商品校验 ====================

    @Test
    void unknownProduct_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setIntentionProduct("不存在的车");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
        assertTrue(result.getResult().getErrors().stream()
                .anyMatch(e -> e.getColumn().equals("意向产品")));
    }

    @Test
    void validProduct_shouldTransform() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setIntentionProduct("比亚迪e2");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(0, result.getResult().getFailedRows());
        assertEquals(2, result.getClues().get(0).getIntentionProduct());
    }

    // ==================== 活动校验 ====================

    @Test
    void unknownActivity_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setActivityName("不存在的活动");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
    }

    // ==================== 公式注入检测 ====================

    @Test
    void formulaInFullName_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setFullName("=CMD");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
    }

    @Test
    void formulaInWeixin_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setWeixin("+123456");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
    }

    // ==================== 超行数限制 ====================

    @Test
    void exceedMaxRows_shouldFailAll() {
        List<ClueExcelRaw> rows = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            rows.add(buildValidRaw());
        }

        ValidatedClueImport result = validator.validateAndTransform(rows, context, 1);

        assertEquals(1001, result.getResult().getTotalRows());
        assertEquals(1001, result.getResult().getFailedRows());
        assertTrue(result.getClues().isEmpty());
    }

    // ==================== 任一行失败时零写入 ====================

    @Test
    void oneRowFails_shouldReturnZeroValidClues() {
        ClueExcelRaw valid = buildValidRaw();
        ClueExcelRaw invalid = buildValidRaw();
        invalid.setPhone("bad");
        invalid.setFullName("另一个人");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(valid, invalid), context, 1);

        assertEquals(2, result.getResult().getTotalRows());
        assertEquals(1, result.getResult().getFailedRows());
        assertEquals(1, result.getResult().getValidRows());
        // 只返回有效行对应的 clue
        assertEquals(1, result.getClues().size());
    }

    // ==================== 年龄校验 ====================

    @Test
    void invalidAge_shouldFail() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setAge(200);

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 1);

        assertEquals(1, result.getResult().getFailedRows());
    }

    // ==================== 转换完整性 ====================

    @Test
    void transformRow_shouldSetAllFieldsCorrectly() {
        ClueExcelRaw raw = buildValidRaw();
        raw.setAppellation("先生");
        raw.setState("未联系");
        raw.setSource("网络广告");
        raw.setNeedLoan("需要");
        raw.setIntentionState("有意向");
        raw.setIntentionProduct("比亚迪e2");
        raw.setActivityName("春季促销");

        ValidatedClueImport result = validator.validateAndTransform(
                List.of(raw), context, 99);

        assertEquals(0, result.getResult().getFailedRows());
        var clue = result.getClues().get(0);
        assertEquals(10, clue.getOwnerId());
        assertEquals(100, clue.getActivityId());
        assertEquals("张三", clue.getFullName());
        assertEquals(18, clue.getAppellation());
        assertEquals("13800138000", clue.getPhone());
        assertEquals("wx123", clue.getWeixin());
        assertEquals("12345", clue.getQq());
        assertEquals("test@test.com", clue.getEmail());
        assertEquals(30, clue.getAge());
        assertEquals("工程师", clue.getJob());
        assertEquals("北京市", clue.getAddress());
        assertEquals(49, clue.getNeedLoan());
        assertEquals(48, clue.getIntentionState());
        assertEquals(2, clue.getIntentionProduct());
        assertEquals(26, clue.getState());
        assertEquals(16, clue.getSource());
        assertEquals("想买车", clue.getDescription());
        assertEquals(99, clue.getCreateBy());
        assertNotNull(clue.getCreateTime());
    }

    // ==================== 辅助方法 ====================

    private ClueExcelRaw buildValidRaw() {
        ClueExcelRaw raw = new ClueExcelRaw();
        raw.setOwnerName("张三丰");
        raw.setFullName("张三");
        raw.setPhone("13800138000");
        raw.setWeixin("wx123");
        raw.setQq("12345");
        raw.setEmail("test@test.com");
        raw.setAge(30);
        raw.setJob("工程师");
        raw.setAddress("北京市");
        raw.setDescription("想买车");
        return raw;
    }

    private ImportContext buildTestContext() {
        // 字典值
        Map<String, List<TDicValue>> dicMap = new HashMap<>();

        // 称呼
        List<TDicValue> appellations = new ArrayList<>();
        TDicValue mr = new TDicValue();
        mr.setId(18);
        mr.setTypeCode("appellation");
        mr.setTypeValue("先生");
        appellations.add(mr);
        TDicValue ms = new TDicValue();
        ms.setId(41);
        ms.setTypeCode("appellation");
        ms.setTypeValue("女士");
        appellations.add(ms);
        dicMap.put("appellation", appellations);

        // 线索状态
        List<TDicValue> states = new ArrayList<>();
        TDicValue contacted = new TDicValue();
        contacted.setId(27);
        contacted.setTypeCode("clueState");
        contacted.setTypeValue("已联系");
        states.add(contacted);
        TDicValue uncontacted = new TDicValue();
        uncontacted.setId(26);
        uncontacted.setTypeCode("clueState");
        uncontacted.setTypeValue("未联系");
        states.add(uncontacted);
        dicMap.put("clueState", states);

        // 线索来源
        List<TDicValue> sources = new ArrayList<>();
        TDicValue expo = new TDicValue();
        expo.setId(3);
        expo.setTypeCode("source");
        expo.setTypeValue("车展会");
        sources.add(expo);
        TDicValue online = new TDicValue();
        online.setId(16);
        online.setTypeCode("source");
        online.setTypeValue("网络广告");
        sources.add(online);
        dicMap.put("source", sources);

        // 贷款
        List<TDicValue> loans = new ArrayList<>();
        TDicValue need = new TDicValue();
        need.setId(49);
        need.setTypeCode("needLoan");
        need.setTypeValue("需要");
        loans.add(need);
        TDicValue noNeed = new TDicValue();
        noNeed.setId(50);
        noNeed.setTypeCode("needLoan");
        noNeed.setTypeValue("不需要");
        loans.add(noNeed);
        dicMap.put("needLoan", loans);

        // 意向状态
        List<TDicValue> intentionStates = new ArrayList<>();
        TDicValue interested = new TDicValue();
        interested.setId(48);
        interested.setTypeCode("intentionState");
        interested.setTypeValue("有意向");
        intentionStates.add(interested);
        TDicValue unclear = new TDicValue();
        unclear.setId(47);
        unclear.setTypeCode("intentionState");
        unclear.setTypeValue("意向不明");
        intentionStates.add(unclear);
        dicMap.put("intentionState", intentionStates);

        // 商品
        Map<String, ProductSimpleDTO> productMap = new HashMap<>();
        ProductSimpleDTO product = new ProductSimpleDTO();
        product.setId(2);
        product.setName("比亚迪e2");
        productMap.put("比亚迪e2", product);

        // 负责人
        Map<String, TUser> ownerMap = new HashMap<>();
        TUser owner = new TUser();
        owner.setId(10);
        owner.setName("张三丰");
        ownerMap.put("张三丰", owner);

        // 活动
        Map<String, TActivity> activityMap = new HashMap<>();
        TActivity activity = new TActivity();
        activity.setId(100);
        activity.setName("春季促销");
        activityMap.put("春季促销", activity);

        return new ImportContext(dicMap, productMap, ownerMap, activityMap);
    }
}
