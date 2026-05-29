package com.bjpowernode.mapper;

import com.bjpowernode.model.CustomerOption;
import com.bjpowernode.model.TCustomer;
import com.bjpowernode.query.CustomerQuery;

import java.util.List;

public interface TCustomerMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TCustomer record);

    int insertSelective(TCustomer record);

    TCustomer selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TCustomer record);

    int updateByPrimaryKey(TCustomer record);

    List<TCustomer> selectCustomerPage();

    List<TCustomer> selectCustomerByExcel(List<String> idList);

    Integer selectByCount();
    
    /**
     * 查询客户列表（包含线索名称）
     */
    List<TCustomer> selectByQuery(CustomerQuery query);
    
    /**
     * 获取所有客户选项（用于下拉选择，去重）
     */
    List<CustomerOption> selectCustomerOptions();
}