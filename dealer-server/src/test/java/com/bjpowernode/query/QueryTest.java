package com.bjpowernode.query;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

    // ==================== BaseQuery ====================

    @Test
    void testBaseQueryDefaultValues() {
        BaseQuery query = new BaseQuery();

        assertNull(query.getToken());
        assertNull(query.getFilterSQL());
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getPageSize());
    }

    @Test
    void testBaseQueryBuilder() {
        BaseQuery query = BaseQuery.builder()
                .token("test-token")
                .filterSQL("and ta.owner_id = 1")
                .current(2)
                .pageSize(20)
                .build();

        assertEquals("test-token", query.getToken());
        assertEquals("and ta.owner_id = 1", query.getFilterSQL());
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getPageSize());
    }

    @Test
    void testBaseQuerySetterGetter() {
        BaseQuery query = new BaseQuery();
        query.setToken("new-token");
        query.setFilterSQL("and tu.id = 5");
        query.setCurrent(3);
        query.setPageSize(50);

        assertEquals("new-token", query.getToken());
        assertEquals("and tu.id = 5", query.getFilterSQL());
        assertEquals(3, query.getCurrent());
        assertEquals(50, query.getPageSize());
    }

    @Test
    void testBaseQueryAllArgsConstructor() {
        BaseQuery query = new BaseQuery("token", "filter", 5, 25);

        assertEquals("token", query.getToken());
        assertEquals("filter", query.getFilterSQL());
        assertEquals(5, query.getCurrent());
        assertEquals(25, query.getPageSize());
    }

    // ==================== CustomerQuery ====================

    @Test
    void testCustomerQueryGetterSetter() {
        CustomerQuery query = new CustomerQuery();
        query.setCustomerName("张三");
        query.setProductId(5);
        query.setCreateBy(1);
        query.setClueId(10);
        query.setProduct(5);
        query.setDescription("测试客户");
        Date nextContactTime = new Date();
        query.setNextContactTime(nextContactTime);

        assertEquals("张三", query.getCustomerName());
        assertEquals(5, query.getProductId());
        assertEquals(1, query.getCreateBy());
        assertEquals(10, query.getClueId());
        assertEquals(5, query.getProduct());
        assertEquals("测试客户", query.getDescription());
        assertEquals(nextContactTime, query.getNextContactTime());
    }

    @Test
    void testCustomerQueryDefaultConstructor() {
        CustomerQuery query = new CustomerQuery();
        assertNull(query.getCustomerName());
        assertNull(query.getProductId());
        assertNull(query.getCreateBy());
    }

    // ==================== ClueQuery ====================

    @Test
    void testClueQueryExtendsBaseQuery() {
        ClueQuery query = new ClueQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testClueQueryDefaultValues() {
        ClueQuery query = new ClueQuery();

        assertNull(query.getId());
        assertNull(query.getOwnerId());
        assertNull(query.getActivityId());
        assertNull(query.getFullName());
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getPageSize());
    }

    @Test
    void testClueQueryGetterSetter() {
        ClueQuery query = new ClueQuery();
        query.setId(1);
        query.setOwnerId(1);
        query.setActivityId(10);
        query.setFullName("张三");
        query.setAppellation(18);
        query.setPhone("13800138000");
        query.setWeixin("zhangsan");
        query.setQq("123456");
        query.setEmail("zhangsan@example.com");
        query.setAge(30);
        query.setJob("工程师");
        query.setYearIncome(new BigDecimal("200000"));
        query.setAddress("北京市");
        query.setNeedLoan(1);
        query.setIntentionState(48);
        query.setIntentionProduct(2);
        query.setState(1);
        query.setSource(3);
        query.setDescription("有购车意向");
        Date nextContactTime = new Date();
        query.setNextContactTime(nextContactTime);

        assertEquals(1, query.getId());
        assertEquals(1, query.getOwnerId());
        assertEquals(10, query.getActivityId());
        assertEquals("张三", query.getFullName());
        assertEquals(18, query.getAppellation());
        assertEquals("13800138000", query.getPhone());
        assertEquals("zhangsan", query.getWeixin());
        assertEquals("123456", query.getQq());
        assertEquals("zhangsan@example.com", query.getEmail());
        assertEquals(30, query.getAge());
        assertEquals("工程师", query.getJob());
        assertEquals(new BigDecimal("200000"), query.getYearIncome());
        assertEquals("北京市", query.getAddress());
        assertEquals(1, query.getNeedLoan());
        assertEquals(48, query.getIntentionState());
        assertEquals(2, query.getIntentionProduct());
        assertEquals(1, query.getState());
        assertEquals(3, query.getSource());
        assertEquals("有购车意向", query.getDescription());
        assertEquals(nextContactTime, query.getNextContactTime());
    }

    @Test
    void testClueQueryInheritsBaseQueryFields() {
        ClueQuery query = new ClueQuery();
        query.setToken("test-token");
        query.setFilterSQL("filter");
        query.setCurrent(2);
        query.setPageSize(20);

        assertEquals("test-token", query.getToken());
        assertEquals("filter", query.getFilterSQL());
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getPageSize());
    }

    // ==================== TranQuery ====================

    @Test
    void testTranQueryExtendsBaseQuery() {
        TranQuery query = new TranQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testTranQueryGetterSetter() {
        TranQuery query = new TranQuery();
        query.setTranNo("20240101000001");
        query.setCustomerId(1);
        query.setCustomerName("张三");
        query.setStage(41);
        query.setMinMoney(new BigDecimal("10000"));
        query.setMaxMoney(new BigDecimal("100000"));
        Date expectedDateStart = new Date();
        Date expectedDateEnd = new Date();
        Date createTimeStart = new Date();
        Date createTimeEnd = new Date();
        query.setExpectedDateStart(expectedDateStart);
        query.setExpectedDateEnd(expectedDateEnd);
        query.setCreateTimeStart(createTimeStart);
        query.setCreateTimeEnd(createTimeEnd);
        query.setCreateBy(1);
        query.setProductId(5);
        query.setProductName("比亚迪e2");
        query.setProductionStatus("生产中");
        query.setInvoiceStatus("已开票");

        assertEquals("20240101000001", query.getTranNo());
        assertEquals(1, query.getCustomerId());
        assertEquals("张三", query.getCustomerName());
        assertEquals(41, query.getStage());
        assertEquals(new BigDecimal("10000"), query.getMinMoney());
        assertEquals(new BigDecimal("100000"), query.getMaxMoney());
        assertEquals(expectedDateStart, query.getExpectedDateStart());
        assertEquals(expectedDateEnd, query.getExpectedDateEnd());
        assertEquals(createTimeStart, query.getCreateTimeStart());
        assertEquals(createTimeEnd, query.getCreateTimeEnd());
        assertEquals(1, query.getCreateBy());
        assertEquals(5, query.getProductId());
        assertEquals("比亚迪e2", query.getProductName());
        assertEquals("生产中", query.getProductionStatus());
        assertEquals("已开票", query.getInvoiceStatus());
    }

    @Test
    void testTranQueryDefaultConstructor() {
        TranQuery query = new TranQuery();
        assertNull(query.getTranNo());
        assertNull(query.getCustomerId());
        assertNull(query.getStage());
        assertNull(query.getMinMoney());
        assertNull(query.getMaxMoney());
    }

    @Test
    void testTranQueryInheritsBaseQueryFields() {
        TranQuery query = new TranQuery();
        query.setToken("token");
        query.setFilterSQL("filter");
        query.setCurrent(3);
        query.setPageSize(50);

        assertEquals("token", query.getToken());
        assertEquals("filter", query.getFilterSQL());
        assertEquals(3, query.getCurrent());
        assertEquals(50, query.getPageSize());
    }

    // ==================== ActivityQuery ====================

    @Test
    void testActivityQueryExtendsBaseQuery() {
        ActivityQuery query = new ActivityQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testActivityQueryGetterSetter() {
        ActivityQuery query = new ActivityQuery();
        query.setId(1);
        query.setOwnerId(1);
        query.setName("春节促销");
        Date startTime = new Date();
        Date endTime = new Date();
        Date createTime = new Date();
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setCost(new BigDecimal("50000"));
        query.setCreateTime(createTime);
        query.setDescription("春节促销活动");

        assertEquals(1, query.getId());
        assertEquals(1, query.getOwnerId());
        assertEquals("春节促销", query.getName());
        assertEquals(startTime, query.getStartTime());
        assertEquals(endTime, query.getEndTime());
        assertEquals(new BigDecimal("50000"), query.getCost());
        assertEquals(createTime, query.getCreateTime());
        assertEquals("春节促销活动", query.getDescription());
    }

    @Test
    void testActivityQueryDefaultConstructor() {
        ActivityQuery query = new ActivityQuery();
        assertNull(query.getId());
        assertNull(query.getName());
        assertNull(query.getCost());
    }

    @Test
    void testActivityQueryInheritsBaseQueryFields() {
        ActivityQuery query = new ActivityQuery();
        query.setToken("token");
        query.setCurrent(2);
        query.setPageSize(20);

        assertEquals("token", query.getToken());
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getPageSize());
    }

    // ==================== ActivityRemarkQuery ====================

    @Test
    void testActivityRemarkQueryExtendsBaseQuery() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testActivityRemarkQueryGetterSetter() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setId(1);
        query.setActivityId(10);
        query.setNoteContent("备注内容");

        assertEquals(1, query.getId());
        assertEquals(10, query.getActivityId());
        assertEquals("备注内容", query.getNoteContent());
    }

    @Test
    void testActivityRemarkQueryDefaultConstructor() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        assertNull(query.getId());
        assertNull(query.getActivityId());
        assertNull(query.getNoteContent());
    }

    @Test
    void testActivityRemarkQueryInheritsBaseQueryFields() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setToken("token");
        query.setCurrent(2);

        assertEquals("token", query.getToken());
        assertEquals(2, query.getCurrent());
    }

    // ==================== ClueRemarkQuery ====================

    @Test
    void testClueRemarkQueryExtendsBaseQuery() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testClueRemarkQueryGetterSetter() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(10);
        query.setNoteContent("跟踪内容");
        query.setNoteWay(1);

        assertEquals(10, query.getClueId());
        assertEquals("跟踪内容", query.getNoteContent());
        assertEquals(1, query.getNoteWay());
    }

    @Test
    void testClueRemarkQueryDefaultConstructor() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        assertNull(query.getClueId());
        assertNull(query.getNoteContent());
        assertNull(query.getNoteWay());
    }

    @Test
    void testClueRemarkQueryInheritsBaseQueryFields() {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setToken("token");
        query.setPageSize(20);

        assertEquals("token", query.getToken());
        assertEquals(20, query.getPageSize());
    }

    // ==================== DicQuery ====================

    @Test
    void testDicQueryExtendsBaseQuery() {
        DicQuery query = new DicQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testDicQueryGetterSetter() {
        DicQuery query = new DicQuery();
        query.setId(1);
        query.setTypeCode("appellation");
        query.setTypeName("称呼");
        query.setValueId("10");
        query.setTypeValue("先生");
        query.setText("text");
        query.setOrderNo("1");
        query.setRemark("备注");
        query.setPage(1);
        query.setSize(10);

        assertEquals(1, query.getId());
        assertEquals("appellation", query.getTypeCode());
        assertEquals("称呼", query.getTypeName());
        assertEquals("10", query.getValueId());
        assertEquals("先生", query.getTypeValue());
        assertEquals("text", query.getText());
        assertEquals("1", query.getOrderNo());
        assertEquals("备注", query.getRemark());
        assertEquals(1, query.getPage());
        assertEquals(10, query.getSize());
    }

    @Test
    void testDicQueryDefaultConstructor() {
        DicQuery query = new DicQuery();
        assertNull(query.getId());
        assertNull(query.getTypeCode());
        assertNull(query.getPage());
        assertNull(query.getSize());
    }

    @Test
    void testDicQueryInheritsBaseQueryFields() {
        DicQuery query = new DicQuery();
        query.setToken("token");
        query.setCurrent(2);
        query.setPageSize(20);

        assertEquals("token", query.getToken());
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getPageSize());
    }

    // ==================== SystemQuery ====================

    @Test
    void testSystemQueryExtendsBaseQuery() {
        SystemQuery query = new SystemQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testSystemQueryGetterSetter() {
        SystemQuery query = new SystemQuery();
        query.setSystemCode("SYS001");
        query.setName("系统名称");
        query.setSite("http://localhost");
        query.setLogo("logo.png");
        query.setTitle("标题");
        query.setDescription("描述");
        query.setKeywords("关键词");
        query.setShortcuticon("icon.ico");
        query.setTel("123456");
        query.setWeixin("wx");
        query.setEmail("e@e.com");
        query.setAddress("地址");
        query.setVersion("1.0");
        query.setCloseMsg("关闭消息");
        query.setIsopen("1");

        assertEquals("SYS001", query.getSystemCode());
        assertEquals("系统名称", query.getName());
        assertEquals("http://localhost", query.getSite());
        assertEquals("logo.png", query.getLogo());
        assertEquals("标题", query.getTitle());
        assertEquals("描述", query.getDescription());
        assertEquals("关键词", query.getKeywords());
        assertEquals("icon.ico", query.getShortcuticon());
        assertEquals("123456", query.getTel());
        assertEquals("wx", query.getWeixin());
        assertEquals("e@e.com", query.getEmail());
        assertEquals("地址", query.getAddress());
        assertEquals("1.0", query.getVersion());
        assertEquals("关闭消息", query.getCloseMsg());
        assertEquals("1", query.getIsopen());
    }

    @Test
    void testSystemQueryDefaultConstructor() {
        SystemQuery query = new SystemQuery();
        assertNull(query.getSystemCode());
        assertNull(query.getName());
        assertNull(query.getIsopen());
    }

    @Test
    void testSystemQueryInheritsBaseQueryFields() {
        SystemQuery query = new SystemQuery();
        query.setToken("token");
        query.setCurrent(3);
        query.setPageSize(50);

        assertEquals("token", query.getToken());
        assertEquals(3, query.getCurrent());
        assertEquals(50, query.getPageSize());
    }

    // ==================== UserQuery ====================

    @Test
    void testUserQueryExtendsBaseQuery() {
        UserQuery query = new UserQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testUserQueryGetterSetter() {
        UserQuery query = new UserQuery();
        query.setId(1);
        query.setLoginAct("admin");
        query.setLoginPwd("password");
        query.setName("管理员");
        query.setPhone("13800138000");
        query.setEmail("admin@example.com");
        query.setAccountNoExpired(1);
        query.setCredentialsNoExpired(1);
        query.setAccountNoLocked(1);
        query.setAccountEnabled(1);

        assertEquals(1, query.getId());
        assertEquals("admin", query.getLoginAct());
        assertEquals("password", query.getLoginPwd());
        assertEquals("管理员", query.getName());
        assertEquals("13800138000", query.getPhone());
        assertEquals("admin@example.com", query.getEmail());
        assertEquals(1, query.getAccountNoExpired());
        assertEquals(1, query.getCredentialsNoExpired());
        assertEquals(1, query.getAccountNoLocked());
        assertEquals(1, query.getAccountEnabled());
    }

    @Test
    void testUserQueryDefaultConstructor() {
        UserQuery query = new UserQuery();
        assertNull(query.getId());
        assertNull(query.getLoginAct());
        assertNull(query.getName());
    }

    @Test
    void testUserQueryInheritsBaseQueryFields() {
        UserQuery query = new UserQuery();
        query.setToken("token");
        query.setCurrent(2);
        query.setPageSize(20);

        assertEquals("token", query.getToken());
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getPageSize());
    }

    // ==================== TranProductQuery ====================

    @Test
    void testTranProductQueryExtendsBaseQuery() {
        TranProductQuery query = new TranProductQuery();
        assertTrue(query instanceof BaseQuery);
    }

    @Test
    void testTranProductQueryGetterSetter() {
        TranProductQuery query = new TranProductQuery();
        query.setId(1);
        query.setTranId(10);
        query.setProductId(5);
        query.setProductName("比亚迪e2");
        query.setQuantity(2);
        query.setPrice(new BigDecimal("120000"));
        query.setCreateTime(new Date());
        query.setCreateBy(1);

        assertEquals(1, query.getId());
        assertEquals(10, query.getTranId());
        assertEquals(5, query.getProductId());
        assertEquals("比亚迪e2", query.getProductName());
        assertEquals(2, query.getQuantity());
        assertEquals(new BigDecimal("120000"), query.getPrice());
        assertNotNull(query.getCreateTime());
        assertEquals(1, query.getCreateBy());
    }

    @Test
    void testTranProductQueryDefaultConstructor() {
        TranProductQuery query = new TranProductQuery();
        assertNull(query.getId());
        assertNull(query.getTranId());
        assertNull(query.getProductId());
    }

    @Test
    void testTranProductQueryInheritsBaseQueryFields() {
        TranProductQuery query = new TranProductQuery();
        query.setToken("token");
        query.setCurrent(3);
        query.setPageSize(50);

        assertEquals("token", query.getToken());
        assertEquals(3, query.getCurrent());
        assertEquals(50, query.getPageSize());
    }
}
