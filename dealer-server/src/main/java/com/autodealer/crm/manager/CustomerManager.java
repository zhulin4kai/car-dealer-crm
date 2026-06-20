package com.autodealer.crm.manager;

import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TTranProduct;
import com.autodealer.crm.query.CustomerQuery;
import com.autodealer.crm.service.TranService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class CustomerManager {

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TProductMapper productMapper;

    @Resource
    private TranService tranService;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Transactional(rollbackFor = Exception.class)
    public Boolean convertCustomer(CustomerQuery customerQuery, Integer operatorId) {
        // 1、原子性更新线索状态为已转客户，防止并发重复转换
        int updateCount = tClueMapper.updateStateToConverted(
                customerQuery.getClueId(), operatorId, currentUserProvider.getDataScopeUserId());
        if (updateCount == 0) {
            throw new RuntimeException("该线索已经转过客户，不能再转了.");
        }

        // 2、向客户表插入一条数据
        TCustomer tCustomer = new TCustomer();

        // 把 CustomerQuery 对象里面的属性数据复制到 TCustomer 对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(customerQuery, tCustomer);
        tCustomer.setCreateTime(new Date());
        tCustomer.setCreateBy(operatorId); //创建人

        int insert = tCustomerMapper.insertSelective(tCustomer);

        // 3、客户转换成功后，创建交易记录
        if (insert >= 1) {
            // 构造 TTran 对象
            TTran tTran = new TTran();
            tTran.setCustomerId(tCustomer.getId());
            tTran.setStage(TranStage.QUOTATION);
            tTran.setDescription(customerQuery.getDescription());
            tTran.setNextContactTime(customerQuery.getNextContactTime());
            tTran.setCreateBy(operatorId);

            // 根据用户所选的产品，构造 TTranProduct 列表
            List<TTranProduct> products = new ArrayList<>();
            if (customerQuery.getProduct() != null) {
                // 使用 ProductMapper 获取产品信息
                TProduct product = productMapper.selectById(customerQuery.getProduct().longValue());
                
                if (product != null) {
                    TTranProduct tranProduct = new TTranProduct();
                    tranProduct.setProductId(customerQuery.getProduct());
                    // 使用用户指定的数量，默认为 1
                    int quantity = customerQuery.getQuantity() != null && customerQuery.getQuantity() > 0 
                        ? customerQuery.getQuantity() : 1;
                    tranProduct.setQuantity(quantity);
                    tranProduct.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
                    tranProduct.setCreateBy(operatorId);
                    products.add(tranProduct);
                }
            }

            // 调用 createTransaction 方法插入交易数据
            tranService.createTransaction(tTran, products);
        }

        return insert >= 1;
    }
}
