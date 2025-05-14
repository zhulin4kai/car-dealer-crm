package com.bjpowernode.mapper;

import com.bjpowernode.config.TestConfig;
import com.bjpowernode.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = {"classpath:schema-test.sql"})
public class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private DataSource dataSource;
    
    @BeforeEach
    public void setUp() {
        // 使用JdbcTemplate插入测试数据
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // 先清空表
        jdbcTemplate.execute("DELETE FROM product");
        
        // 插入测试数据 - 低库存商品用于测试
        String insertSql = "INSERT INTO product (sku, name, category, specification, price, stock, min_stock, status, create_time, update_time) VALUES " +
                "('LOW001', '低库存手机', '电子产品', '64GB 黑色', 2999.00, 5, 20, '上架', NOW(), NOW()), " +
                "('LOW002', '低库存笔记本', '电子产品', 'i5 8GB 256GB', 4999.00, 2, 10, '上架', NOW(), NOW()), " +
                "('LOW003', '低库存平板', '电子产品', '10.2英寸 32GB', 2599.00, 3, 15, '上架', NOW(), NOW()), " +
                "('LOW004', '低库存耳机', '电子产品', '无线蓝牙', 199.00, 8, 30, '上架', NOW(), NOW()), " +
                "('LOW005', '低库存键盘', '电子产品', '有线USB', 99.00, 5, 20, '上架', NOW(), NOW()), " +
                "('NORM001', '正常库存商品', '服装', 'XL', 199.00, 100, 20, '上架', NOW(), NOW())";
        
        jdbcTemplate.execute(insertSql);
    }

    @Test
    @DisplayName("测试查询库存预警商品不带筛选条件")
    public void testSelectStockAlertsWithFilterNoParams() {
        List<Product> products = productMapper.selectStockAlertsWithFilter(null, null, null, null, null);
        assertFalse(products.isEmpty(), "应该返回至少一个库存预警商品");
        assertEquals(5, products.size(), "应该返回5个库存预警商品");
    }

    @Test
    @DisplayName("测试查询库存预警商品按SKU筛选")
    public void testSelectStockAlertsWithFilterBySku() {
        List<Product> products = productMapper.selectStockAlertsWithFilter(null, null, "LOW00", null, null);
        assertFalse(products.isEmpty(), "应该返回至少一个符合SKU条件的库存预警商品");
        assertTrue(products.size() >= 5, "应该返回至少5个库存预警商品");
        
        // 验证筛选条件生效
        for (Product product : products) {
            assertTrue(product.getSku().contains("LOW00"), "返回的商品SKU应该包含LOW00");
        }
    }

    @Test
    @DisplayName("测试查询库存预警商品按名称筛选")
    public void testSelectStockAlertsWithFilterByName() {
        List<Product> products = productMapper.selectStockAlertsWithFilter(null, null, null, "低库存", null);
        assertFalse(products.isEmpty(), "应该返回至少一个符合名称条件的库存预警商品");
        assertTrue(products.size() >= 5, "应该返回至少5个库存预警商品");
        
        // 验证筛选条件生效
        for (Product product : products) {
            assertTrue(product.getName().contains("低库存"), "返回的商品名称应该包含'低库存'");
        }
    }

    @Test
    @DisplayName("测试查询库存预警商品按分类筛选")
    public void testSelectStockAlertsWithFilterByCategory() {
        List<Product> products = productMapper.selectStockAlertsWithFilter(null, null, null, null, "电子产品");
        assertFalse(products.isEmpty(), "应该返回至少一个符合分类条件的库存预警商品");
        assertEquals(5, products.size(), "应该返回5个电子产品类的库存预警商品");
        
        // 验证筛选条件生效
        for (Product product : products) {
            assertEquals("电子产品", product.getCategory(), "返回的商品分类应该是'电子产品'");
        }
    }

    @Test
    @DisplayName("测试查询库存预警商品组合筛选条件")
    public void testSelectStockAlertsWithFilterMultiParams() {
        List<Product> products = productMapper.selectStockAlertsWithFilter(null, null, "LOW00", "手机", "电子产品");
        assertFalse(products.isEmpty(), "应该返回至少一个符合多条件的库存预警商品");
        assertEquals(1, products.size(), "应该返回1个符合组合条件的库存预警商品");
        
        // 验证筛选条件生效
        Product product = products.get(0);
        assertTrue(product.getSku().contains("LOW00"), "返回的商品SKU应该包含LOW00");
        assertTrue(product.getName().contains("手机"), "返回的商品名称应该包含'手机'");
        assertEquals("电子产品", product.getCategory(), "返回的商品分类应该是'电子产品'");
    }
} 