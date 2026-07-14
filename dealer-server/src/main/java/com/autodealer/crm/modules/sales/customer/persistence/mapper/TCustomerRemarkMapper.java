package com.autodealer.crm.modules.sales.customer.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomerRemark;


@Mapper
public interface TCustomerRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TCustomerRemark record);

    int insertSelective(TCustomerRemark record);

    TCustomerRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TCustomerRemark record);

    int updateByPrimaryKey(TCustomerRemark record);
}
