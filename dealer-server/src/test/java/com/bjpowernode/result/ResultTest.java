package com.bjpowernode.result;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    // ==================== R ====================

    @Test
    void testROkWithoutData() {
        R<Object> result = R.OK();

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testROkWithData() {
        String data = "testData";
        R<String> result = R.OK(data);

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("testData", result.getData());
    }

    @Test
    void testROkWithMsgAndData() {
        R<String> result = R.OK("自定义消息", "data");

        assertEquals(200, result.getCode());
        assertEquals("自定义消息", result.getMsg());
        assertEquals("data", result.getData());
    }

    @Test
    void testRFailWithoutMsg() {
        R<Object> result = R.FAIL();

        assertEquals(500, result.getCode());
        assertEquals("操作失败", result.getMsg());
    }

    @Test
    void testRFailWithMsg() {
        R<Object> result = R.FAIL("自定义错误");

        assertEquals(500, result.getCode());
        assertEquals("自定义错误", result.getMsg());
    }

    @Test
    void testRFailWithCodeAndMsg() {
        R<Object> result = R.FAIL(404, "未找到");

        assertEquals(404, result.getCode());
        assertEquals("未找到", result.getMsg());
    }

    @Test
    void testRFailWithCodeEnum() {
        R<Object> result = R.FAIL(CodeEnum.TOKEN_IS_EMPTY);

        assertEquals(510, result.getCode());
        assertEquals("token为空", result.getMsg());
    }

    @Test
    void testROkLowercase() {
        R<Object> result = R.ok();

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
    }

    @Test
    void testROkLowercaseWithData() {
        R<String> result = R.ok("data");

        assertEquals(200, result.getCode());
        assertEquals("data", result.getData());
    }

    @Test
    void testROkLowercaseWithMsgAndData() {
        R<String> result = R.ok("msg", "data");

        assertEquals(200, result.getCode());
        assertEquals("msg", result.getMsg());
        assertEquals("data", result.getData());
    }

    @Test
    void testRErrorLowercase() {
        R<Object> result = R.error();

        assertEquals(500, result.getCode());
    }

    @Test
    void testRErrorLowercaseWithMsg() {
        R<Object> result = R.error("错误信息");

        assertEquals(500, result.getCode());
        assertEquals("错误信息", result.getMsg());
    }

    @Test
    void testRErrorLowercaseWithCodeAndMsg() {
        R<Object> result = R.error(403, "禁止访问");

        assertEquals(403, result.getCode());
        assertEquals("禁止访问", result.getMsg());
    }

    @Test
    void testRErrorLowercaseWithCodeEnum() {
        R<Object> result = R.error(CodeEnum.UNAUTHORIZED_ERROR);

        assertEquals(503, result.getCode());
        assertEquals("没有访问权限", result.getMsg());
    }

    @Test
    void testRDefaultConstructor() {
        R<Object> r = new R<>();
        assertNull(r.getCode());
        assertNull(r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void testRTwoArgConstructor() {
        R<Object> r = new R<>(200, "ok");
        assertEquals(200, r.getCode());
        assertEquals("ok", r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void testRThreeArgConstructor() {
        R<Object> r = new R<>(200, "ok", "data");
        assertEquals(200, r.getCode());
        assertEquals("ok", r.getMsg());
        assertEquals("data", r.getData());
    }

    @Test
    void testRWithListData() {
        R<Object> result = R.OK(java.util.Arrays.asList("a", "b", "c"));
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    // ==================== Result ====================

    @Test
    void testResultSuccess() {
        Result<String> result = Result.success();

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testResultSuccessWithData() {
        Result<String> result = Result.success("data");

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("data", result.getData());
    }

    @Test
    void testResultError() {
        Result<Object> result = Result.error("错误信息");

        assertEquals(500, result.getCode());
        assertEquals("错误信息", result.getMsg());
    }

    @Test
    void testResultErrorWithCode() {
        Result<Object> result = Result.error(404, "未找到");

        assertEquals(404, result.getCode());
        assertEquals("未找到", result.getMsg());
    }

    @Test
    void testResultDefaultConstructor() {
        Result<Object> result = new Result<>();
        assertNull(result.getCode());
        assertNull(result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testResultSetterGetter() {
        Result<Object> result = new Result<>();
        result.setCode(200);
        result.setMsg("ok");
        result.setData("data");

        assertEquals(200, result.getCode());
        assertEquals("ok", result.getMsg());
        assertEquals("data", result.getData());
    }

    @Test
    void testResultSuccessWithListData() {
        Result<Object> result = Result.success(java.util.Arrays.asList(1, 2, 3));
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    // ==================== CodeEnum ====================

    @Test
    void testCodeEnumValues() {
        assertEquals(200, CodeEnum.OK.getCode());
        assertEquals("操作成功", CodeEnum.OK.getMsg());

        assertEquals(500, CodeEnum.FAIL.getCode());
        assertEquals("操作失败", CodeEnum.FAIL.getMsg());

        assertEquals(501, CodeEnum.PARAM_ERROR.getCode());
        assertEquals("请求参数格式有误", CodeEnum.PARAM_ERROR.getMsg());

        assertEquals(502, CodeEnum.LOGIN_ERROR.getCode());
        assertEquals("登录失败", CodeEnum.LOGIN_ERROR.getMsg());

        assertEquals(503, CodeEnum.UNAUTHORIZED_ERROR.getCode());
        assertEquals("没有访问权限", CodeEnum.UNAUTHORIZED_ERROR.getMsg());

        assertEquals(504, CodeEnum.TOKEN_ERROR.getCode());
        assertEquals("token无效", CodeEnum.TOKEN_ERROR.getMsg());

        assertEquals(505, CodeEnum.TOKEN_EXPIRED.getCode());
        assertEquals("token已过期", CodeEnum.TOKEN_EXPIRED.getMsg());

        assertEquals(506, CodeEnum.SYSTEM_ERROR.getCode());
        assertEquals("系统异常", CodeEnum.SYSTEM_ERROR.getMsg());
    }

    @Test
    void testCodeEnumTokenErrors() {
        assertEquals(510, CodeEnum.TOKEN_IS_EMPTY.getCode());
        assertEquals("token为空", CodeEnum.TOKEN_IS_EMPTY.getMsg());

        assertEquals(511, CodeEnum.TOKEN_IS_ERROR.getCode());
        assertEquals("token无效", CodeEnum.TOKEN_IS_ERROR.getMsg());

        assertEquals(512, CodeEnum.TOKEN_IS_EXPIRED.getCode());
        assertEquals("token已过期", CodeEnum.TOKEN_IS_EXPIRED.getMsg());

        assertEquals(513, CodeEnum.TOKEN_IS_NONE_MATCH.getCode());
        assertEquals("token不匹配", CodeEnum.TOKEN_IS_NONE_MATCH.getMsg());
    }

    @Test
    void testCodeEnumAccessErrors() {
        assertEquals(520, CodeEnum.ACCESS_DENIED.getCode());
        assertEquals("没有访问权限", CodeEnum.ACCESS_DENIED.getMsg());

        assertEquals(521, CodeEnum.DATA_ACCESS_EXCEPTION.getCode());
        assertEquals("数据访问异常", CodeEnum.DATA_ACCESS_EXCEPTION.getMsg());
    }

    @Test
    void testCodeEnumUserLogout() {
        assertEquals(200, CodeEnum.USER_LOGOUT.getCode());
        assertEquals("退出成功", CodeEnum.USER_LOGOUT.getMsg());
    }

    @Test
    void testCodeEnumValuesArray() {
        CodeEnum[] values = CodeEnum.values();
        assertTrue(values.length >= 14);
    }

    @Test
    void testCodeEnumValueOf() {
        assertEquals(CodeEnum.OK, CodeEnum.valueOf("OK"));
        assertEquals(CodeEnum.FAIL, CodeEnum.valueOf("FAIL"));
        assertEquals(CodeEnum.TOKEN_IS_EMPTY, CodeEnum.valueOf("TOKEN_IS_EMPTY"));
    }

    // ==================== NameValue ====================

    @Test
    void testNameValueGetterSetter() {
        NameValue nv = new NameValue();
        nv.setName("成交");
        nv.setValue(20);

        assertEquals("成交", nv.getName());
        assertEquals(20, nv.getValue());
    }

    @Test
    void testNameValueDefaultConstructor() {
        NameValue nv = new NameValue();
        assertNull(nv.getName());
        assertNull(nv.getValue());
    }

    @Test
    void testNameValueAllArgsConstructor() {
        NameValue nv = new NameValue("线索", 100);

        assertEquals("线索", nv.getName());
        assertEquals(100, nv.getValue());
    }

    @Test
    void testNameValueBuilder() {
        NameValue nv = NameValue.builder()
                .name("客户")
                .value(50)
                .build();

        assertEquals("客户", nv.getName());
        assertEquals(50, nv.getValue());
    }

    // ==================== SummaryData ====================

    @Test
    void testSummaryDataGetterSetter() {
        SummaryData data = new SummaryData();
        data.setEffectiveActivityCount(10);
        data.setTotalActivityCount(20);
        data.setTotalClueCount(100);
        data.setTotalCustomerCount(50);
        data.setSuccessTranAmount(new BigDecimal("500000"));
        data.setTotalTranAmount(new BigDecimal("1000000"));

        assertEquals(10, data.getEffectiveActivityCount());
        assertEquals(20, data.getTotalActivityCount());
        assertEquals(100, data.getTotalClueCount());
        assertEquals(50, data.getTotalCustomerCount());
        assertEquals(new BigDecimal("500000"), data.getSuccessTranAmount());
        assertEquals(new BigDecimal("1000000"), data.getTotalTranAmount());
    }

    @Test
    void testSummaryDataDefaultConstructor() {
        SummaryData data = new SummaryData();
        assertNull(data.getEffectiveActivityCount());
        assertNull(data.getTotalActivityCount());
        assertNull(data.getTotalClueCount());
        assertNull(data.getTotalCustomerCount());
        assertNull(data.getSuccessTranAmount());
        assertNull(data.getTotalTranAmount());
    }

    @Test
    void testSummaryDataAllArgsConstructor() {
        SummaryData data = new SummaryData(10, 20, 100, 50,
                new BigDecimal("500000"), new BigDecimal("1000000"));

        assertEquals(10, data.getEffectiveActivityCount());
        assertEquals(20, data.getTotalActivityCount());
        assertEquals(100, data.getTotalClueCount());
        assertEquals(50, data.getTotalCustomerCount());
        assertEquals(new BigDecimal("500000"), data.getSuccessTranAmount());
        assertEquals(new BigDecimal("1000000"), data.getTotalTranAmount());
    }

    @Test
    void testSummaryDataBuilder() {
        SummaryData data = SummaryData.builder()
                .effectiveActivityCount(5)
                .totalActivityCount(10)
                .totalClueCount(50)
                .totalCustomerCount(25)
                .successTranAmount(new BigDecimal("200000"))
                .totalTranAmount(new BigDecimal("400000"))
                .build();

        assertEquals(5, data.getEffectiveActivityCount());
        assertEquals(10, data.getTotalActivityCount());
        assertEquals(50, data.getTotalClueCount());
        assertEquals(25, data.getTotalCustomerCount());
        assertEquals(new BigDecimal("200000"), data.getSuccessTranAmount());
        assertEquals(new BigDecimal("400000"), data.getTotalTranAmount());
    }

    // ==================== DicEnum ====================

    @Test
    void testDicEnumValues() {
        assertEquals("appellation", DicEnum.APPELLATION.getCode());
        assertEquals("source", DicEnum.SOURCE.getCode());
        assertEquals("clueState", DicEnum.STATE.getCode());
        assertEquals("intentionState", DicEnum.INTENTIONSTATE.getCode());
        assertEquals("needLoan", DicEnum.NEEDLOAN.getCode());
        assertEquals("product", DicEnum.PRODUCT.getCode());
        assertEquals("activity", DicEnum.ACTIVITY.getCode());
    }

    @Test
    void testDicEnumSetterGetter() {
        DicEnum dic = DicEnum.APPELLATION;
        dic.setCode("newCode");
        assertEquals("newCode", dic.getCode());
        dic.setCode("appellation");
    }

    @Test
    void testDicEnumValuesArray() {
        DicEnum[] values = DicEnum.values();
        assertEquals(7, values.length);
    }

    @Test
    void testDicEnumValueOf() {
        assertEquals(DicEnum.APPELLATION, DicEnum.valueOf("APPELLATION"));
        assertEquals(DicEnum.SOURCE, DicEnum.valueOf("SOURCE"));
        assertEquals(DicEnum.PRODUCT, DicEnum.valueOf("PRODUCT"));
    }

    // ==================== CustomerExcel ====================

    @Test
    void testCustomerExcelGetterSetter() {
        CustomerExcel excel = new CustomerExcel();
        excel.setOwnerName("管理员");
        excel.setActivityName("春节促销");
        excel.setFullName("张三");
        excel.setAppellationName("先生");
        excel.setPhone("13800138000");
        excel.setWeixin("zhangsan");
        excel.setQq("123456");
        excel.setEmail("zhangsan@example.com");
        excel.setAge(30);
        excel.setJob("工程师");
        excel.setYearIncome(new BigDecimal("200000"));
        excel.setAddress("北京市");
        excel.setNeedLoanName("需要");
        excel.setProductName("比亚迪e2");
        excel.setSourceName("网络");
        excel.setDescription("客户描述");
        Date nextContactTime = new Date();
        excel.setNextContactTime(nextContactTime);

        assertEquals("管理员", excel.getOwnerName());
        assertEquals("春节促销", excel.getActivityName());
        assertEquals("张三", excel.getFullName());
        assertEquals("先生", excel.getAppellationName());
        assertEquals("13800138000", excel.getPhone());
        assertEquals("zhangsan", excel.getWeixin());
        assertEquals("123456", excel.getQq());
        assertEquals("zhangsan@example.com", excel.getEmail());
        assertEquals(30, excel.getAge());
        assertEquals("工程师", excel.getJob());
        assertEquals(new BigDecimal("200000"), excel.getYearIncome());
        assertEquals("北京市", excel.getAddress());
        assertEquals("需要", excel.getNeedLoanName());
        assertEquals("比亚迪e2", excel.getProductName());
        assertEquals("网络", excel.getSourceName());
        assertEquals("客户描述", excel.getDescription());
        assertEquals(nextContactTime, excel.getNextContactTime());
    }

    @Test
    void testCustomerExcelDefaultConstructor() {
        CustomerExcel excel = new CustomerExcel();
        assertNull(excel.getOwnerName());
        assertNull(excel.getFullName());
        assertEquals(0, excel.getAge());
    }

    // ==================== ClueExcel ====================

    @Test
    void testClueExcelGetterSetter() {
        ClueExcel excel = new ClueExcel();
        excel.setOwnerId(1);
        excel.setActivityId(10);
        excel.setFullName("张三");
        excel.setAppellation(18);
        excel.setPhone("13800138000");
        excel.setWeixin("zhangsan");
        excel.setQq("123456");
        excel.setEmail("zhangsan@example.com");
        excel.setAge(30);
        excel.setJob("工程师");
        excel.setYearIncome(new BigDecimal("200000"));
        excel.setAddress("北京市");
        excel.setNeedLoan(1);
        excel.setIntentionState(48);
        excel.setIntentionProduct(2);
        excel.setState(1);
        excel.setSource(3);
        excel.setDescription("线索描述");
        Date nextContactTime = new Date();
        excel.setNextContactTime(nextContactTime);

        assertEquals(1, excel.getOwnerId());
        assertEquals(10, excel.getActivityId());
        assertEquals("张三", excel.getFullName());
        assertEquals(18, excel.getAppellation());
        assertEquals("13800138000", excel.getPhone());
        assertEquals("zhangsan", excel.getWeixin());
        assertEquals("123456", excel.getQq());
        assertEquals("zhangsan@example.com", excel.getEmail());
        assertEquals(30, excel.getAge());
        assertEquals("工程师", excel.getJob());
        assertEquals(new BigDecimal("200000"), excel.getYearIncome());
        assertEquals("北京市", excel.getAddress());
        assertEquals(1, excel.getNeedLoan());
        assertEquals(48, excel.getIntentionState());
        assertEquals(2, excel.getIntentionProduct());
        assertEquals(1, excel.getState());
        assertEquals(3, excel.getSource());
        assertEquals("线索描述", excel.getDescription());
        assertEquals(nextContactTime, excel.getNextContactTime());
    }

    @Test
    void testClueExcelDefaultConstructor() {
        ClueExcel excel = new ClueExcel();
        assertNull(excel.getOwnerId());
        assertNull(excel.getFullName());
    }
}
