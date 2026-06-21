package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.query.CustomerListQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TCustomerMapper {
    int deleteByPrimaryKey(Integer id);
    int insert(TCustomer record);
    int insertSelective(TCustomer record);
    TCustomer selectByPrimaryKey(Integer id);
    int updateByPrimaryKeySelective(TCustomer record);
    int updateByPrimaryKey(TCustomer record);
    List<TCustomer> selectCustomerPage();
    List<TCustomer> selectCustomerByExcel(@Param("idList") List<String> idList,
                                          @Param("dataScopeUserId") Integer dataScopeUserId,
                                          @Param("maxRows") Integer maxRows);
    int countCustomerByExcel(@Param("idList") List<String> idList,
                             @Param("dataScopeUserId") Integer dataScopeUserId);
    Integer selectByCount();
    @DataScope(tableAlias = "tc", tableField = "owner_id")
    List<TCustomer> selectByQuery(CustomerListQuery query);
    List<CustomerOption> selectCustomerOptions(Integer dataScopeUserId);
    TCustomer selectScopedById(@Param("id") Integer id, @Param("dataScopeUserId") Integer dataScopeUserId);
}
