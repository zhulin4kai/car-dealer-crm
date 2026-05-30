package com.bjpowernode.manager;

import com.bjpowernode.mapper.TClueMapper;
import com.bjpowernode.mapper.TCustomerMapper;
import com.bjpowernode.mapper.ProductMapper;
import com.bjpowernode.model.TClue;
import com.bjpowernode.model.TCustomer;
import com.bjpowernode.model.TTran;
import com.bjpowernode.model.Product;
import com.bjpowernode.model.TTranProduct;
import com.bjpowernode.query.CustomerQuery;
import com.bjpowernode.service.TranService;
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
    private ProductMapper productMapper;

    @Resource
    private TranService tranService;

    @Transactional(rollbackFor = Exception.class)
    public Boolean convertCustomer(CustomerQuery customerQuery) {
        //1、原子性更新线索状态为已转客户，防止并发重复转换
        int updateCount = tClueMapper.updateStateToConverted(customerQuery.getClueId(), customerQuery.getCreateBy());
        if (updateCount == 0) {
            throw new RuntimeException("该线索已经转过客户，不能再转了.");
        }

        //2、向客户表插入一条数据
        TCustomer tCustomer = new TCustomer();
        //把CustomerQuery对象里面的属性数据复制到TCustomer对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(customerQuery, tCustomer);
        tCustomer.setCreateTime(new Date()); //创建时间
        //登录人的id
        tCustomer.setCreateBy(customerQuery.getCreateBy()); //创建人
        int insert = tCustomerMapper.insertSelective(tCustomer);

        //3、客户转换成功后，创建交易记录
        if (insert >= 1) {
            // 构造TTran对象
            TTran tTran = new TTran();
            tTran.setCustomerId(tCustomer.getId());
            tTran.setStage(41); // 默认交易阶段为41
            tTran.setDescription(customerQuery.getDescription());
            tTran.setNextContactTime(customerQuery.getNextContactTime());
            tTran.setCreateBy(customerQuery.getCreateBy());

            // 根据用户所选的产品，构造TTranProduct列表
            List<TTranProduct> products = new ArrayList<>();
            if (customerQuery.getProduct() != null) {
                // 使用ProductMapper获取产品信息
                Product product = productMapper.selectById(customerQuery.getProduct().longValue());
                
                if (product != null) {
                    TTranProduct tranProduct = new TTranProduct();
                    tranProduct.setProductId(customerQuery.getProduct());
                    tranProduct.setQuantity(1); // 默认数量为1
                    tranProduct.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
                    tranProduct.setCreateBy(customerQuery.getCreateBy());
                    products.add(tranProduct);
                }
            }

            // 调用createTransaction方法插入交易数据
            tranService.createTransaction(tTran, products);
        }

        return insert >= 1;
    }
}
