package com.autodealer.crm.audit;

import com.autodealer.crm.mapper.TOperationLogMapper;
import com.autodealer.crm.model.TOperationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TOperationLogMapper 集成测试，使用 H2 数据库验证 SQL 和 resultMap。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TOperationLogMapper 集成测试")
class TOperationLogMapperIntegrationTest {

    @Autowired
    private TOperationLogMapper mapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 清除其他非 @Transactional 集成测试（如 UserControllerH2IntegrationTest、
        // UserFlowIntegrationTest）提交的审计记录，避免共享 H2 数据库的数据污染。
        jdbcTemplate.update("DELETE FROM t_operation_log");

        Date now = new Date();
        for (int i = 1; i <= 5; i++) {
            TOperationLog log = new TOperationLog();
            log.setUserId(i);
            log.setUserName("用户" + i);
            log.setActionCode("USER_CREATE");
            log.setModuleName("用户管理");
            log.setResourceId(String.valueOf(100 + i));
            log.setDetail("{\"result\":\"SUCCESS\",\"summary\":{\"loginAct\":\"user" + i + "\"}}");
            log.setIp("10.0.0." + i);
            log.setCreateTime(new Date(now.getTime() - i * 10000));
            assertEquals(1, mapper.insert(log));
        }
        for (int i = 1; i <= 3; i++) {
            TOperationLog log = new TOperationLog();
            log.setUserId(i + 10);
            log.setUserName("操作员" + i);
            log.setActionCode("TRAN_CREATE");
            log.setModuleName("交易管理");
            log.setResourceId(String.valueOf(200 + i));
            log.setDetail("{\"result\":\"SUCCESS\"}");
            log.setIp("10.0.1." + i);
            log.setCreateTime(new Date(now.getTime() + i * 10000));
            assertEquals(1, mapper.insert(log));
        }
    }

    @Test
    @DisplayName("按模块名分页查询应按 create_time DESC, id DESC 排序")
    void selectByModule_shouldOrderByCreateTimeDescThenIdDesc() {
        List<TOperationLog> list = mapper.selectByModule("用户管理", 0, 10);

        assertEquals(5, list.size());
        for (int i = 0; i < list.size() - 1; i++) {
            TOperationLog current = list.get(i);
            TOperationLog next = list.get(i + 1);
            assertTrue(current.getCreateTime().compareTo(next.getCreateTime()) >= 0);
            if (current.getCreateTime().equals(next.getCreateTime())) {
                assertTrue(current.getId() >= next.getId());
            }
        }
    }

    @Test
    @DisplayName("按模块名统计数量应准确")
    void selectCountByModule_shouldReturnCorrectCount() {
        Integer count = mapper.selectCountByModule("交易管理");
        assertEquals(3, count);
    }

    @Test
    @DisplayName("按用户ID查询应按 create_time DESC 稳定排序")
    void selectByUserId_shouldOrderStably() {
        List<TOperationLog> list = mapper.selectByUserId(11, 0, 10);

        assertFalse(list.isEmpty());
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i).getCreateTime().compareTo(list.get(i + 1).getCreateTime()) >= 0);
        }
    }

    @Test
    @DisplayName("insert 应返回影响行数 1 并生成主键")
    void insert_shouldReturnOneAndGenerateId() {
        TOperationLog log = new TOperationLog();
        log.setUserId(99);
        log.setUserName("测试用户");
        log.setActionCode("DICT_TYPE_SAVE");
        log.setModuleName("字典管理");
        log.setResourceId("999");
        log.setDetail("{\"result\":\"SUCCESS\"}");
        log.setIp("10.0.0.99");
        log.setCreateTime(new Date());

        int rows = mapper.insert(log);
        assertEquals(1, rows);
        assertNotNull(log.getId());
        assertTrue(log.getId() > 0);
    }

    @Test
    @DisplayName("分页查询 limit 参数应生效")
    void pagingQuery_shouldRespectLimit() {
        List<TOperationLog> list = mapper.selectByModule("用户管理", 0, 2);

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("分页查询 offset 参数应生效")
    void pagingQuery_shouldRespectOffset() {
        List<TOperationLog> fullList = mapper.selectByModule("用户管理", 0, 10);

        List<TOperationLog> pagedList = mapper.selectByModule("用户管理", 2, 10);

        assertEquals(fullList.size() - 2, pagedList.size());
        assertEquals(fullList.get(2).getId(), pagedList.get(0).getId());
    }

    @Test
    @DisplayName("resultMap 应正确映射 DB 列到 Java 属性")
    void resultMap_shouldMapColumnsCorrectly() {
        TOperationLog log = new TOperationLog();
        log.setUserId(1);
        log.setUserName("验证明用户");
        log.setActionCode("DICT_TYPE_SAVE");
        log.setModuleName("字典管理");
        log.setResourceId("888");
        log.setDetail("{\"result\":\"SUCCESS\"}");
        log.setIp("172.16.0.1");
        log.setCreateTime(new Date());
        mapper.insert(log);

        List<TOperationLog> list = mapper.selectByUserId(1, 0, 10);

        assertFalse(list.isEmpty());
        TOperationLog retrieved = list.stream()
                .filter(l -> "888".equals(l.getResourceId()))
                .findFirst()
                .orElse(null);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.getUserId());
        assertEquals("验证明用户", retrieved.getUserName());
        assertEquals("DICT_TYPE_SAVE", retrieved.getActionCode());
        assertEquals("字典管理", retrieved.getModuleName());
        assertEquals("888", retrieved.getResourceId());
        assertNotNull(retrieved.getDetail());
        assertEquals("172.16.0.1", retrieved.getIp());
        assertNotNull(retrieved.getCreateTime());
    }
}
