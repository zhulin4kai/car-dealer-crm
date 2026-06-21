package com.autodealer.crm.integration;

import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranProductMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.model.TTranProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class DataLayerMapperIntegrationTest extends BackendIntegrationTestBase {

    @Autowired
    private DicMapper dicMapper;

    @Autowired
    private TTranProductMapper tranProductMapper;

    @Autowired
    private TCustomerMapper customerMapper;

    @Test
    @DisplayName("字典值业务编码必须完整写入和读出")
    void dictionaryValueCodeMustRoundTrip() {
        TDicValue value = new TDicValue();
        value.setTypeCode("source");
        value.setTypeValue("数据层审计来源");
        value.setValueCode("data_layer_audit");
        value.setOrder(99);

        assertEquals(1, dicMapper.insertDicValue(value));
        assertEquals("data_layer_audit", dicMapper.selectDicValueById(value.getId()).getValueCode());
    }

    @Test
    @DisplayName("交易商品和客户必须支持BIGINT产品ID")
    void productReferencesMustSupportBigint() {
        long productId = 3_000_000_000L;
        jdbcTemplate.update(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (?, 'BIGINT-DATA-LAYER', 'BIGINT数据层商品', 100, 10, 'on_sale')", productId);

        TTranProduct tranProduct = new TTranProduct();
        tranProduct.setId(900);
        tranProduct.setTranId(1);
        tranProduct.setProductId(productId);
        tranProduct.setQuantity(1);
        tranProduct.setPrice(BigDecimal.valueOf(100));
        assertEquals(1, tranProductMapper.insertSelective(tranProduct));
        assertEquals(productId, tranProductMapper.selectByPrimaryKey(900).getProductId());

        TCustomer customer = new TCustomer();
        customer.setProduct(productId);
        customer.setDescription("BIGINT产品关联测试");
        assertEquals(1, customerMapper.insertSelective(customer));
        assertEquals(productId, customerMapper.selectByPrimaryKey(customer.getId()).getProduct());
    }
}
