package com.bjpowernode.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // ==================== TUser ====================

    @Test
    void testTUserGetterSetter() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setLoginPwd("password");
        user.setName("Admin");
        user.setPhone("13800138000");
        user.setEmail("admin@example.com");
        user.setAccountNoExpired(1);
        user.setCredentialsNoExpired(1);
        user.setAccountNoLocked(1);
        user.setAccountEnabled(1);
        user.setCreateTime(new Date());
        user.setCreateBy(1);
        user.setEditTime(new Date());
        user.setEditBy(1);
        user.setLastLoginTime(new Date());

        assertEquals(1, user.getId());
        assertEquals("admin", user.getLoginAct());
        assertEquals("password", user.getLoginPwd());
        assertEquals("Admin", user.getName());
        assertEquals("13800138000", user.getPhone());
        assertEquals("admin@example.com", user.getEmail());
        assertEquals(1, user.getAccountNoExpired());
        assertEquals(1, user.getCredentialsNoExpired());
        assertEquals(1, user.getAccountNoLocked());
        assertEquals(1, user.getAccountEnabled());
        assertNotNull(user.getCreateTime());
        assertEquals(1, user.getCreateBy());
    }

    @Test
    void testTUserUserDetailsMethods() {
        TUser user = new TUser();
        user.setAccountNoExpired(1);
        user.setCredentialsNoExpired(1);
        user.setAccountNoLocked(1);
        user.setAccountEnabled(1);
        user.setLoginAct("admin");
        user.setLoginPwd("password");

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isEnabled());
        assertEquals("admin", user.getUsername());
        assertEquals("password", user.getPassword());
    }

    @Test
    void testTUserAuthorities() {
        TUser user = new TUser();
        List<String> roles = Arrays.asList("admin", "user");
        List<String> permissions = Arrays.asList("user:add", "user:delete");
        user.setRoleList(roles);
        user.setPermissionList(permissions);

        assertNotNull(user.getAuthorities());
        assertEquals(4, user.getAuthorities().size());
    }

    @Test
    void testTUserDisabledState() {
        TUser user = new TUser();
        user.setAccountNoExpired(0);
        user.setCredentialsNoExpired(0);
        user.setAccountNoLocked(0);
        user.setAccountEnabled(0);

        assertFalse(user.isAccountNonExpired());
        assertFalse(user.isCredentialsNonExpired());
        assertFalse(user.isAccountNonLocked());
        assertFalse(user.isEnabled());
    }

    @Test
    void testTUserAuthoritiesEmptyLists() {
        TUser user = new TUser();
        user.setRoleList(null);
        user.setPermissionList(null);
        assertNotNull(user.getAuthorities());
        assertEquals(0, user.getAuthorities().size());
    }

    @Test
    void testTUserAuthoritiesEmptyArrayLists() {
        TUser user = new TUser();
        user.setRoleList(new ArrayList<>());
        user.setPermissionList(new ArrayList<>());
        assertNotNull(user.getAuthorities());
        assertEquals(0, user.getAuthorities().size());
    }

    @Test
    void testTUserRolesOnly() {
        TUser user = new TUser();
        user.setRoleList(Arrays.asList("ROLE_ADMIN"));
        user.setPermissionList(null);
        assertEquals(1, user.getAuthorities().size());
    }

    @Test
    void testTUserPermissionsOnly() {
        TUser user = new TUser();
        user.setRoleList(null);
        user.setPermissionList(Arrays.asList("user:view"));
        assertEquals(1, user.getAuthorities().size());
    }

    @Test
    void testTUserAssociatedObjects() {
        TUser user = new TUser();
        TUser createByDO = new TUser();
        createByDO.setName("creator");
        TUser editByDO = new TUser();
        editByDO.setName("editor");
        user.setCreateByDO(createByDO);
        user.setEditByDO(editByDO);

        assertNotNull(user.getCreateByDO());
        assertEquals("creator", user.getCreateByDO().getName());
        assertNotNull(user.getEditByDO());
        assertEquals("editor", user.getEditByDO().getName());
    }

    @Test
    void testTUserMenuPermissionList() {
        TUser user = new TUser();
        TPermission perm = new TPermission();
        perm.setId(1);
        perm.setName("用户管理");
        List<TPermission> menuList = Arrays.asList(perm);
        user.setMenuPermissionList(menuList);

        assertNotNull(user.getMenuPermissionList());
        assertEquals(1, user.getMenuPermissionList().size());
    }

    // ==================== TActivity ====================

    @Test
    void testTActivityGetterSetter() {
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setOwnerId(1);
        activity.setName("春节促销");
        Date startTime = new Date();
        Date endTime = new Date();
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setCost(new BigDecimal("50000"));
        activity.setDescription("春节促销活动");
        Date createTime = new Date();
        activity.setCreateTime(createTime);
        activity.setCreateBy(1);
        Date editTime = new Date();
        activity.setEditTime(editTime);
        activity.setEditBy(2);

        assertEquals(1, activity.getId());
        assertEquals(1, activity.getOwnerId());
        assertEquals("春节促销", activity.getName());
        assertEquals(startTime, activity.getStartTime());
        assertEquals(endTime, activity.getEndTime());
        assertEquals(new BigDecimal("50000"), activity.getCost());
        assertEquals("春节促销活动", activity.getDescription());
        assertEquals(createTime, activity.getCreateTime());
        assertEquals(1, activity.getCreateBy());
        assertEquals(editTime, activity.getEditTime());
        assertEquals(2, activity.getEditBy());
    }

    @Test
    void testTActivityDefaultConstructor() {
        TActivity activity = new TActivity();
        assertNull(activity.getId());
        assertNull(activity.getName());
        assertNull(activity.getCost());
    }

    @Test
    void testTActivityAssociatedObjects() {
        TActivity activity = new TActivity();
        TUser owner = new TUser();
        owner.setName("owner");
        TUser createByDO = new TUser();
        createByDO.setName("creator");
        TUser editByDO = new TUser();
        editByDO.setName("editor");
        activity.setOwnerDO(owner);
        activity.setCreateByDO(createByDO);
        activity.setEditByDO(editByDO);

        assertNotNull(activity.getOwnerDO());
        assertEquals("owner", activity.getOwnerDO().getName());
        assertNotNull(activity.getCreateByDO());
        assertNotNull(activity.getEditByDO());
    }

    // ==================== TActivityRemark ====================

    @Test
    void testTActivityRemarkGetterSetter() {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(1);
        remark.setActivityId(10);
        remark.setNoteContent("备注内容");
        remark.setCreateTime(new Date());
        remark.setCreateBy(1);
        remark.setEditTime(new Date());
        remark.setEditBy(2);
        remark.setDeleted(0);

        assertEquals(1, remark.getId());
        assertEquals(10, remark.getActivityId());
        assertEquals("备注内容", remark.getNoteContent());
        assertNotNull(remark.getCreateTime());
        assertEquals(1, remark.getCreateBy());
        assertEquals(0, remark.getDeleted());
    }

    @Test
    void testTActivityRemarkDefaultConstructor() {
        TActivityRemark remark = new TActivityRemark();
        assertNull(remark.getId());
        assertNull(remark.getActivityId());
        assertNull(remark.getDeleted());
    }

    @Test
    void testTActivityRemarkAssociatedObjects() {
        TActivityRemark remark = new TActivityRemark();
        TUser createByDO = new TUser();
        createByDO.setName("creator");
        TUser editByDO = new TUser();
        editByDO.setName("editor");
        remark.setCreateByDO(createByDO);
        remark.setEditByDO(editByDO);

        assertNotNull(remark.getCreateByDO());
        assertEquals("creator", remark.getCreateByDO().getName());
        assertNotNull(remark.getEditByDO());
    }

    // ==================== TClue ====================

    @Test
    void testTClueGetterSetter() {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setOwnerId(1);
        clue.setActivityId(10);
        clue.setFullName("张三");
        clue.setAppellation(18);
        clue.setPhone("13800138000");
        clue.setWeixin("zhangsan");
        clue.setQq("123456");
        clue.setEmail("zhangsan@example.com");
        clue.setAge(30);
        clue.setJob("工程师");
        clue.setYearIncome(new BigDecimal("200000"));
        clue.setAddress("北京市");
        clue.setNeedLoan(1);
        clue.setIntentionState(48);
        clue.setIntentionProduct(2);
        clue.setState(1);
        clue.setSource(3);
        clue.setDescription("有购车意向");
        Date nextContactTime = new Date();
        clue.setNextContactTime(nextContactTime);
        clue.setCreateTime(new Date());
        clue.setCreateBy(1);
        clue.setEditTime(new Date());
        clue.setEditBy(2);

        assertEquals(1, clue.getId());
        assertEquals(1, clue.getOwnerId());
        assertEquals(10, clue.getActivityId());
        assertEquals("张三", clue.getFullName());
        assertEquals(18, clue.getAppellation());
        assertEquals("13800138000", clue.getPhone());
        assertEquals("zhangsan", clue.getWeixin());
        assertEquals("123456", clue.getQq());
        assertEquals("zhangsan@example.com", clue.getEmail());
        assertEquals(30, clue.getAge());
        assertEquals("工程师", clue.getJob());
        assertEquals(new BigDecimal("200000"), clue.getYearIncome());
        assertEquals("北京市", clue.getAddress());
        assertEquals(1, clue.getNeedLoan());
        assertEquals(48, clue.getIntentionState());
        assertEquals(2, clue.getIntentionProduct());
        assertEquals(1, clue.getState());
        assertEquals(3, clue.getSource());
        assertEquals("有购车意向", clue.getDescription());
        assertEquals(nextContactTime, clue.getNextContactTime());
    }

    @Test
    void testTClueDefaultConstructor() {
        TClue clue = new TClue();
        assertNull(clue.getId());
        assertNotNull(clue.getOwnerDO());
        assertNotNull(clue.getActivityDO());
        assertNotNull(clue.getAppellationDO());
        assertNotNull(clue.getNeedLoanDO());
        assertNotNull(clue.getIntentionStateDO());
        assertNotNull(clue.getIntentionProductDO());
        assertNotNull(clue.getStateDO());
        assertNotNull(clue.getSourceDO());
    }

    @Test
    void testTClueAssociatedObjects() {
        TClue clue = new TClue();
        TUser ownerDO = new TUser();
        ownerDO.setName("owner");
        clue.setOwnerDO(ownerDO);

        assertNotNull(clue.getOwnerDO());
        assertEquals("owner", clue.getOwnerDO().getName());
    }

    // ==================== TClueRemark ====================

    @Test
    void testTClueRemarkGetterSetter() {
        TClueRemark remark = new TClueRemark();
        remark.setId(1);
        remark.setClueId(10);
        remark.setNoteWay(1);
        remark.setNoteContent("电话联系");
        remark.setCreateTime(new Date());
        remark.setCreateBy(1);
        remark.setEditTime(new Date());
        remark.setEditBy(2);
        remark.setDeleted(0);

        assertEquals(1, remark.getId());
        assertEquals(10, remark.getClueId());
        assertEquals(1, remark.getNoteWay());
        assertEquals("电话联系", remark.getNoteContent());
        assertNotNull(remark.getCreateTime());
        assertEquals(1, remark.getCreateBy());
        assertEquals(0, remark.getDeleted());
    }

    @Test
    void testTClueRemarkDefaultConstructor() {
        TClueRemark remark = new TClueRemark();
        assertNull(remark.getId());
        assertNull(remark.getClueId());
        assertNull(remark.getDeleted());
    }

    @Test
    void testTClueRemarkAssociatedObjects() {
        TClueRemark remark = new TClueRemark();
        TUser createByDO = new TUser();
        TUser editByDO = new TUser();
        TDicValue noteWayDO = new TDicValue();
        remark.setCreateByDO(createByDO);
        remark.setEditByDO(editByDO);
        remark.setNoteWayDO(noteWayDO);

        assertNotNull(remark.getCreateByDO());
        assertNotNull(remark.getEditByDO());
        assertNotNull(remark.getNoteWayDO());
    }

    // ==================== TCustomer ====================

    @Test
    void testTCustomerGetterSetter() {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setClueId(10);
        customer.setProduct(5);
        customer.setDescription("测试客户");
        Date nextContactTime = new Date();
        customer.setNextContactTime(nextContactTime);
        customer.setCreateTime(new Date());
        customer.setCreateBy(1);
        customer.setEditTime(new Date());
        customer.setEditBy(2);

        assertEquals(1, customer.getId());
        assertEquals(10, customer.getClueId());
        assertEquals(5, customer.getProduct());
        assertEquals("测试客户", customer.getDescription());
        assertEquals(nextContactTime, customer.getNextContactTime());
        assertNotNull(customer.getCreateTime());
        assertEquals(1, customer.getCreateBy());
        assertNotNull(customer.getEditTime());
        assertEquals(2, customer.getEditBy());
    }

    @Test
    void testTCustomerDefaultConstructor() {
        TCustomer customer = new TCustomer();
        assertNull(customer.getId());
        assertNotNull(customer.getClueDO());
        assertNotNull(customer.getOwnerDO());
        assertNotNull(customer.getActivityDO());
        assertNotNull(customer.getAppellationDO());
        assertNotNull(customer.getNeedLoanDO());
        assertNotNull(customer.getIntentionStateDO());
        assertNotNull(customer.getIntentionProductDO());
        assertNotNull(customer.getStateDO());
        assertNotNull(customer.getSourceDO());
    }

    @Test
    void testTCustomerAssociatedObjects() {
        TCustomer customer = new TCustomer();
        TClue clueDO = new TClue();
        clueDO.setId(100);
        customer.setClueDO(clueDO);

        assertNotNull(customer.getClueDO());
        assertEquals(100, customer.getClueDO().getId());
    }

    // ==================== TCustomerRemark ====================

    @Test
    void testTCustomerRemarkGetterSetter() {
        TCustomerRemark remark = new TCustomerRemark();
        remark.setId(1);
        remark.setCustomerId(10);
        remark.setNoteWay(1);
        remark.setNoteContent("客户跟踪");
        remark.setCreateBy(1);
        remark.setCreateTime(new Date());
        remark.setEditTime(new Date());
        remark.setEditBy(2);
        remark.setDeleted(0);

        assertEquals(1, remark.getId());
        assertEquals(10, remark.getCustomerId());
        assertEquals(1, remark.getNoteWay());
        assertEquals("客户跟踪", remark.getNoteContent());
        assertEquals(1, remark.getCreateBy());
        assertNotNull(remark.getCreateTime());
        assertEquals(0, remark.getDeleted());
    }

    @Test
    void testTCustomerRemarkDefaultConstructor() {
        TCustomerRemark remark = new TCustomerRemark();
        assertNull(remark.getId());
        assertNull(remark.getCustomerId());
        assertNull(remark.getDeleted());
    }

    // ==================== TDicType ====================

    @Test
    void testTDicTypeGetterSetter() {
        TDicType dicType = new TDicType();
        dicType.setId(1);
        dicType.setTypeCode("appellation");
        dicType.setTypeName("称呼");
        dicType.setRemark("字典类型备注");

        assertEquals(1, dicType.getId());
        assertEquals("appellation", dicType.getTypeCode());
        assertEquals("称呼", dicType.getTypeName());
        assertEquals("字典类型备注", dicType.getRemark());
    }

    @Test
    void testTDicTypeDefaultConstructor() {
        TDicType dicType = new TDicType();
        assertNull(dicType.getId());
        assertNull(dicType.getDicValueList());
    }

    @Test
    void testTDicTypeWithDicValueList() {
        TDicType dicType = new TDicType();
        TDicValue value1 = new TDicValue();
        value1.setId(1);
        TDicValue value2 = new TDicValue();
        value2.setId(2);
        dicType.setDicValueList(Arrays.asList(value1, value2));

        assertNotNull(dicType.getDicValueList());
        assertEquals(2, dicType.getDicValueList().size());
    }

    // ==================== TDicValue ====================

    @Test
    void testTDicValueGetterSetter() {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(1);
        dicValue.setTypeCode("appellation");
        dicValue.setTypeValue("先生");
        dicValue.setOrder(1);
        dicValue.setRemark("备注");

        assertEquals(1, dicValue.getId());
        assertEquals("appellation", dicValue.getTypeCode());
        assertEquals("先生", dicValue.getTypeValue());
        assertEquals(1, dicValue.getOrder());
        assertEquals("备注", dicValue.getRemark());
    }

    @Test
    void testTDicValueDefaultConstructor() {
        TDicValue dicValue = new TDicValue();
        assertNull(dicValue.getId());
        assertNull(dicValue.getTypeCode());
    }

    // ==================== TPermission ====================

    @Test
    void testTPermissionGetterSetter() {
        TPermission perm = new TPermission();
        perm.setId(1);
        perm.setName("用户管理");
        perm.setCode("user:list");
        perm.setUrl("/api/users");
        perm.setType("menu");
        perm.setParentId(0);
        perm.setOrderNo(1);
        perm.setIcon("user-icon");

        assertEquals(1, perm.getId());
        assertEquals("用户管理", perm.getName());
        assertEquals("user:list", perm.getCode());
        assertEquals("/api/users", perm.getUrl());
        assertEquals("menu", perm.getType());
        assertEquals(0, perm.getParentId());
        assertEquals(1, perm.getOrderNo());
        assertEquals("user-icon", perm.getIcon());
    }

    @Test
    void testTPermissionDefaultConstructor() {
        TPermission perm = new TPermission();
        assertNull(perm.getId());
        assertNull(perm.getSubPermissionList());
    }

    @Test
    void testTPermissionWithSubList() {
        TPermission perm = new TPermission();
        TPermission sub = new TPermission();
        sub.setId(2);
        perm.setSubPermissionList(Arrays.asList(sub));

        assertNotNull(perm.getSubPermissionList());
        assertEquals(1, perm.getSubPermissionList().size());
    }

    // ==================== TProduct ====================

    @Test
    void testTProductGetterSetter() {
        TProduct product = new TProduct();
        product.setId(1);
        product.setName("比亚迪e2");
        product.setGuidePriceS(new BigDecimal("100000"));
        product.setGuidePriceE(new BigDecimal("150000"));
        product.setQuotation(new BigDecimal("120000"));
        product.setState(0);
        product.setCreateTime(new Date());
        product.setCreateBy(1);
        product.setEditTime(new Date());
        product.setEditBy(2);

        assertEquals(1, product.getId());
        assertEquals("比亚迪e2", product.getName());
        assertEquals(new BigDecimal("100000"), product.getGuidePriceS());
        assertEquals(new BigDecimal("150000"), product.getGuidePriceE());
        assertEquals(new BigDecimal("120000"), product.getQuotation());
        assertEquals(0, product.getState());
        assertNotNull(product.getCreateTime());
        assertEquals(1, product.getCreateBy());
        assertNotNull(product.getEditTime());
        assertEquals(2, product.getEditBy());
    }

    @Test
    void testTProductDefaultConstructor() {
        TProduct product = new TProduct();
        assertNull(product.getId());
        assertNull(product.getName());
    }

    // ==================== TRole ====================

    @Test
    void testTRoleGetterSetter() {
        TRole role = new TRole();
        role.setId(1);
        role.setRole("admin");
        role.setRoleName("管理员");

        assertEquals(1, role.getId());
        assertEquals("admin", role.getRole());
        assertEquals("管理员", role.getRoleName());
    }

    @Test
    void testTRoleDefaultConstructor() {
        TRole role = new TRole();
        assertNull(role.getId());
        assertNull(role.getRole());
    }

    // ==================== TRolePermission ====================

    @Test
    void testTRolePermissionGetterSetter() {
        TRolePermission rp = new TRolePermission();
        rp.setId(1);
        rp.setRoleId(10);
        rp.setPermissionId(20);

        assertEquals(1, rp.getId());
        assertEquals(10, rp.getRoleId());
        assertEquals(20, rp.getPermissionId());
    }

    @Test
    void testTRolePermissionDefaultConstructor() {
        TRolePermission rp = new TRolePermission();
        assertNull(rp.getId());
        assertNull(rp.getRoleId());
    }

    // ==================== TSystem ====================

    @Test
    void testTSystemGetterSetter() {
        TSystem system = new TSystem();
        system.setId(1);
        system.setSystemCode("SYS001");
        system.setName("经销商CRM系统");
        system.setSite("http://localhost");
        system.setLogo("logo.png");
        system.setTitle("CRM系统");
        system.setDescription("经销商客户关系管理系统");
        system.setKeywords("CRM,经销商");
        system.setShortcuticon("favicon.ico");
        system.setTel("400-123-4567");
        system.setWeixin("wechat");
        system.setEmail("support@example.com");
        system.setAddress("北京市");
        system.setVersion("1.0.0");
        system.setCloseMsg("系统维护中");
        system.setIsopen("1");
        LocalDateTime now = LocalDateTime.now();
        system.setCreateTime(now);
        system.setCreateBy(1);
        system.setEditTime(now);
        system.setEditBy(2);

        assertEquals(1, system.getId());
        assertEquals("SYS001", system.getSystemCode());
        assertEquals("经销商CRM系统", system.getName());
        assertEquals("http://localhost", system.getSite());
        assertEquals("logo.png", system.getLogo());
        assertEquals("CRM系统", system.getTitle());
        assertEquals("经销商客户关系管理系统", system.getDescription());
        assertEquals("CRM,经销商", system.getKeywords());
        assertEquals("favicon.ico", system.getShortcuticon());
        assertEquals("400-123-4567", system.getTel());
        assertEquals("wechat", system.getWeixin());
        assertEquals("support@example.com", system.getEmail());
        assertEquals("北京市", system.getAddress());
        assertEquals("1.0.0", system.getVersion());
        assertEquals("系统维护中", system.getCloseMsg());
        assertEquals("1", system.getIsopen());
        assertEquals(now, system.getCreateTime());
        assertEquals(1, system.getCreateBy());
        assertEquals(now, system.getEditTime());
        assertEquals(2, system.getEditBy());
    }

    @Test
    void testTSystemDefaultConstructor() {
        TSystem system = new TSystem();
        assertNull(system.getId());
        assertNull(system.getName());
    }

    // ==================== TSystemInfo ====================

    @Test
    void testTSystemInfoGetterSetter() {
        TSystemInfo info = new TSystemInfo();
        info.setId(1);
        info.setSystemCode("SYS001");
        info.setName("系统");
        info.setSite("http://localhost");
        info.setLogo("logo.png");
        info.setTitle("标题");
        info.setDescription("描述");
        info.setKeywords("关键词");
        info.setShortcuticon("icon.ico");
        info.setTel("123456");
        info.setWeixin("wx");
        info.setEmail("e@e.com");
        info.setAddress("地址");
        info.setVersion("1.0");
        info.setClosemsg("关闭消息");
        info.setIsopen("1");
        info.setCreateTime(new Date());
        info.setCreateBy(1);
        info.setEditTime(new Date());
        info.setEditBy(2);

        assertEquals(1, info.getId());
        assertEquals("SYS001", info.getSystemCode());
        assertEquals("系统", info.getName());
        assertEquals("http://localhost", info.getSite());
        assertEquals("logo.png", info.getLogo());
        assertEquals("标题", info.getTitle());
        assertEquals("描述", info.getDescription());
        assertEquals("关键词", info.getKeywords());
        assertEquals("icon.ico", info.getShortcuticon());
        assertEquals("123456", info.getTel());
        assertEquals("wx", info.getWeixin());
        assertEquals("e@e.com", info.getEmail());
        assertEquals("地址", info.getAddress());
        assertEquals("1.0", info.getVersion());
        assertEquals("关闭消息", info.getClosemsg());
        assertEquals("1", info.getIsopen());
        assertNotNull(info.getCreateTime());
        assertEquals(1, info.getCreateBy());
        assertNotNull(info.getEditTime());
        assertEquals(2, info.getEditBy());
    }

    @Test
    void testTSystemInfoDefaultConstructor() {
        TSystemInfo info = new TSystemInfo();
        assertNull(info.getId());
        assertNull(info.getSystemCode());
    }

    // ==================== TTran ====================

    @Test
    void testTTranGetterSetter() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("20240101000001");
        tran.setCustomerId(1);
        tran.setCustomerName("张三");
        tran.setMoney(new BigDecimal("100000"));
        tran.setExpectedDate(new Date());
        tran.setStage(41);
        tran.setDescription("交易描述");
        tran.setNextContactTime(new Date());
        tran.setCreateTime(new Date());
        tran.setCreateBy(1);
        tran.setEditTime(new Date());
        tran.setEditBy(2);

        assertEquals(1, tran.getId());
        assertEquals("20240101000001", tran.getTranNo());
        assertEquals(1, tran.getCustomerId());
        assertEquals("张三", tran.getCustomerName());
        assertEquals(new BigDecimal("100000"), tran.getMoney());
        assertNotNull(tran.getExpectedDate());
        assertEquals(41, tran.getStage());
        assertEquals("交易描述", tran.getDescription());
        assertNotNull(tran.getNextContactTime());
        assertNotNull(tran.getCreateTime());
        assertEquals(1, tran.getCreateBy());
        assertNotNull(tran.getEditTime());
        assertEquals(2, tran.getEditBy());
    }

    @Test
    void testTTranDefaultConstructor() {
        TTran tran = new TTran();
        assertNull(tran.getId());
        assertNull(tran.getTranNo());
    }

    // ==================== TTranApprove ====================

    @Test
    void testTTranApproveGetterSetter() {
        TTranApprove approve = new TTranApprove();
        approve.setId(1);
        approve.setTranId(10);
        approve.setApproveResult(true);
        approve.setApproveComment("审批通过");
        approve.setApproveTime(new Date());
        approve.setApproveBy(1);
        approve.setCreateTime(new Date());
        approve.setCreateBy(1);

        assertEquals(1, approve.getId());
        assertEquals(10, approve.getTranId());
        assertTrue(approve.getApproveResult());
        assertEquals("审批通过", approve.getApproveComment());
        assertNotNull(approve.getApproveTime());
        assertEquals(1, approve.getApproveBy());
        assertNotNull(approve.getCreateTime());
        assertEquals(1, approve.getCreateBy());
    }

    @Test
    void testTTranApproveDefaultConstructor() {
        TTranApprove approve = new TTranApprove();
        assertNull(approve.getId());
        assertNull(approve.getApproveResult());
    }

    // ==================== TTranInvoice ====================

    @Test
    void testTTranInvoiceGetterSetter() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(1);
        invoice.setTranId(10);
        invoice.setInvoiceNo("INV001");
        invoice.setType("增值税");
        invoice.setTitle("公司抬头");
        invoice.setTaxNumber("91110000");
        invoice.setBankName("工商银行");
        invoice.setBankAccount("622200000000");
        invoice.setAddress("北京市");
        invoice.setPhone("13800138000");
        invoice.setAmount(new BigDecimal("100000"));
        invoice.setStatus("已开票");
        invoice.setRemark("发票备注");
        invoice.setIssueTime(new Date());
        invoice.setCreateTime(new Date());
        invoice.setCreateBy(1);
        invoice.setUpdateTime(new Date());
        invoice.setUpdateBy(2);

        assertEquals(1, invoice.getId());
        assertEquals(10, invoice.getTranId());
        assertEquals("INV001", invoice.getInvoiceNo());
        assertEquals("增值税", invoice.getType());
        assertEquals("公司抬头", invoice.getTitle());
        assertEquals("91110000", invoice.getTaxNumber());
        assertEquals("工商银行", invoice.getBankName());
        assertEquals("622200000000", invoice.getBankAccount());
        assertEquals("北京市", invoice.getAddress());
        assertEquals("13800138000", invoice.getPhone());
        assertEquals(new BigDecimal("100000"), invoice.getAmount());
        assertEquals("已开票", invoice.getStatus());
        assertEquals("发票备注", invoice.getRemark());
        assertNotNull(invoice.getIssueTime());
        assertNotNull(invoice.getCreateTime());
        assertEquals(1, invoice.getCreateBy());
        assertNotNull(invoice.getUpdateTime());
        assertEquals(2, invoice.getUpdateBy());
    }

    @Test
    void testTTranInvoiceDefaultConstructor() {
        TTranInvoice invoice = new TTranInvoice();
        assertNull(invoice.getId());
        assertNull(invoice.getInvoiceNo());
    }

    // ==================== TTranProduct ====================

    @Test
    void testTTranProductGetterSetter() {
        TTranProduct tp = new TTranProduct();
        tp.setId(1);
        tp.setTranId(10);
        tp.setProductId(5);
        tp.setProductName("比亚迪e2");
        tp.setQuantity(2);
        tp.setPrice(new BigDecimal("120000"));
        tp.setCreateTime(new Date());
        tp.setCreateBy(1);

        assertEquals(1, tp.getId());
        assertEquals(10, tp.getTranId());
        assertEquals(5, tp.getProductId());
        assertEquals("比亚迪e2", tp.getProductName());
        assertEquals(2, tp.getQuantity());
        assertEquals(new BigDecimal("120000"), tp.getPrice());
        assertNotNull(tp.getCreateTime());
        assertEquals(1, tp.getCreateBy());
    }

    @Test
    void testTTranProductDefaultConstructor() {
        TTranProduct tp = new TTranProduct();
        assertNull(tp.getId());
        assertNull(tp.getProductId());
    }

    // ==================== TTranRemark ====================

    @Test
    void testTTranRemarkGetterSetter() {
        TTranRemark remark = new TTranRemark();
        remark.setId(1);
        remark.setTranId(10);
        remark.setNoteWay(1);
        remark.setNoteContent("交易跟踪");
        remark.setCreateTime(new Date());
        remark.setCreateBy(1);
        remark.setEditTime(new Date());
        remark.setEditBy(2);
        remark.setDeleted(0);

        assertEquals(1, remark.getId());
        assertEquals(10, remark.getTranId());
        assertEquals(1, remark.getNoteWay());
        assertEquals("交易跟踪", remark.getNoteContent());
        assertNotNull(remark.getCreateTime());
        assertEquals(1, remark.getCreateBy());
        assertEquals(0, remark.getDeleted());
    }

    @Test
    void testTTranRemarkDefaultConstructor() {
        TTranRemark remark = new TTranRemark();
        assertNull(remark.getId());
        assertNull(remark.getDeleted());
    }

    // ==================== TUserRole ====================

    @Test
    void testTUserRoleGetterSetter() {
        TUserRole ur = new TUserRole();
        ur.setId(1);
        ur.setUserId(10);
        ur.setRoleId(5);

        assertEquals(1, ur.getId());
        assertEquals(10, ur.getUserId());
        assertEquals(5, ur.getRoleId());
    }

    @Test
    void testTUserRoleDefaultConstructor() {
        TUserRole ur = new TUserRole();
        assertNull(ur.getId());
        assertNull(ur.getUserId());
    }

    // ==================== CustomerOption ====================

    @Test
    void testCustomerOptionGetterSetter() {
        CustomerOption option = new CustomerOption();
        option.setCustomerId(1);
        option.setCustomerName("张三");
        option.setClueId(10);

        assertEquals(1, option.getCustomerId());
        assertEquals("张三", option.getCustomerName());
        assertEquals(10, option.getClueId());
    }

    @Test
    void testCustomerOptionDefaultConstructor() {
        CustomerOption option = new CustomerOption();
        assertNull(option.getCustomerId());
        assertNull(option.getCustomerName());
    }

    // ==================== Product ====================

    @Test
    void testProductGetterSetter() {
        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU001");
        product.setName("比亚迪e2");
        product.setCategory("新能源");
        product.setSpecification("标准版");
        product.setPrice(new BigDecimal("100000"));
        product.setStock(100);
        product.setMinStock(10);
        product.setStatus("上架");
        LocalDateTime now = LocalDateTime.now();
        product.setCreateTime(now);
        product.setUpdateTime(now);

        assertEquals(1L, product.getId());
        assertEquals("SKU001", product.getSku());
        assertEquals("比亚迪e2", product.getName());
        assertEquals("新能源", product.getCategory());
        assertEquals("标准版", product.getSpecification());
        assertEquals(new BigDecimal("100000"), product.getPrice());
        assertEquals(100, product.getStock());
        assertEquals(10, product.getMinStock());
        assertEquals("上架", product.getStatus());
        assertEquals(now, product.getCreateTime());
        assertEquals(now, product.getUpdateTime());
    }

    @Test
    void testProductDefaultConstructor() {
        Product product = new Product();
        assertNull(product.getId());
        assertNull(product.getName());
    }

    // ==================== ProductCategory ====================

    @Test
    void testProductCategoryGetterSetter() {
        ProductCategory cat = new ProductCategory();
        cat.setId(1L);
        cat.setName("新能源");
        cat.setCode("NE");
        cat.setDescription("新能源汽车");
        cat.setSort(1);
        cat.setStatus("1");
        LocalDateTime now = LocalDateTime.now();
        cat.setCreateTime(now);
        cat.setUpdateTime(now);

        assertEquals(1L, cat.getId());
        assertEquals("新能源", cat.getName());
        assertEquals("NE", cat.getCode());
        assertEquals("新能源汽车", cat.getDescription());
        assertEquals(1, cat.getSort());
        assertEquals("1", cat.getStatus());
        assertEquals(now, cat.getCreateTime());
        assertEquals(now, cat.getUpdateTime());
    }

    @Test
    void testProductCategoryDefaultConstructor() {
        ProductCategory cat = new ProductCategory();
        assertNull(cat.getId());
        assertNull(cat.getName());
    }

    // ==================== ProductPromotion ====================

    @Test
    void testProductPromotionGetterSetter() {
        ProductPromotion promo = new ProductPromotion();
        promo.setId(1L);
        promo.setName("夏季促销");
        promo.setType("discount");
        promo.setDiscount(new BigDecimal("0.9"));
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(30);
        promo.setStartTime(start);
        promo.setEndTime(end);
        promo.setStatus("1");
        promo.setCreateTime(LocalDateTime.now());
        promo.setUpdateTime(LocalDateTime.now());

        assertEquals(1L, promo.getId());
        assertEquals("夏季促销", promo.getName());
        assertEquals("discount", promo.getType());
        assertEquals(new BigDecimal("0.9"), promo.getDiscount());
        assertEquals(start, promo.getStartTime());
        assertEquals(end, promo.getEndTime());
        assertEquals("1", promo.getStatus());
        assertNotNull(promo.getCreateTime());
        assertNotNull(promo.getUpdateTime());
    }

    @Test
    void testProductPromotionDefaultConstructor() {
        ProductPromotion promo = new ProductPromotion();
        assertNull(promo.getId());
        assertNull(promo.getName());
    }

    // ==================== ProductStockRecord ====================

    @Test
    void testProductStockRecordGetterSetter() {
        ProductStockRecord record = new ProductStockRecord();
        record.setId(1L);
        record.setProductId(10L);
        record.setQuantity(50);
        record.setType("入库");
        record.setRemark("补货");
        record.setCreateTime(LocalDateTime.now());

        assertEquals(1L, record.getId());
        assertEquals(10L, record.getProductId());
        assertEquals(50, record.getQuantity());
        assertEquals("入库", record.getType());
        assertEquals("补货", record.getRemark());
        assertNotNull(record.getCreateTime());
    }

    @Test
    void testProductStockRecordDefaultConstructor() {
        ProductStockRecord record = new ProductStockRecord();
        assertNull(record.getId());
        assertNull(record.getProductId());
    }

    // ==================== TranCreateRequest ====================

    @Test
    void testTranCreateRequestGetterSetter() {
        TranCreateRequest request = new TranCreateRequest();
        request.setId(1);
        request.setCustomerId(10);
        request.setCustomerName("张三");
        request.setAmount(new BigDecimal("100000"));
        request.setDescription("交易描述");
        request.setExpectedDeliveryDate("2024-12-31 00:00:00");

        List<TranCreateRequest.ProductDetail> products = new ArrayList<>();
        TranCreateRequest.ProductDetail detail = new TranCreateRequest.ProductDetail();
        detail.setProductId(5);
        detail.setQuantity(2);
        detail.setPrice(new BigDecimal("50000"));
        products.add(detail);
        request.setProducts(products);

        assertEquals(1, request.getId());
        assertEquals(10, request.getCustomerId());
        assertEquals("张三", request.getCustomerName());
        assertEquals(new BigDecimal("100000"), request.getAmount());
        assertEquals("交易描述", request.getDescription());
        assertEquals("2024-12-31 00:00:00", request.getExpectedDeliveryDate());
        assertNotNull(request.getProducts());
        assertEquals(1, request.getProducts().size());
    }

    @Test
    void testTranCreateRequestDefaultConstructor() {
        TranCreateRequest request = new TranCreateRequest();
        assertNull(request.getId());
        assertNull(request.getProducts());
    }

    @Test
    void testTranCreateRequestProductDetail() {
        TranCreateRequest.ProductDetail detail = new TranCreateRequest.ProductDetail();
        detail.setProductId(5);
        detail.setQuantity(3);
        detail.setPrice(new BigDecimal("30000"));

        assertEquals(5, detail.getProductId());
        assertEquals(3, detail.getQuantity());
        assertEquals(new BigDecimal("30000"), detail.getPrice());
    }

    @Test
    void testTranCreateRequestProductDetailDefaultConstructor() {
        TranCreateRequest.ProductDetail detail = new TranCreateRequest.ProductDetail();
        assertNull(detail.getProductId());
        assertNull(detail.getQuantity());
        assertNull(detail.getPrice());
    }
}
