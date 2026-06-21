package com.autodealer.crm.service;

import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.dto.CustomerDetailResponse;
import com.autodealer.crm.dto.CustomerListResponse;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.query.CustomerListQuery;
import com.autodealer.crm.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CustomerService {

    PageInfo<CustomerListResponse> getCustomerList(CustomerListQuery query, Integer pageNum, Integer pageSize);

    List<CustomerOption> getCustomerOptions();

    CustomerDetailResponse getCustomerById(Integer id);

    void convertCustomer(ConvertCustomerRequest request);

    @Deprecated
    PageInfo<CustomerListResponse> getCustomerByPage(Integer current);

    List<CustomerExcel> getCustomerByExcel(List<String> idList);

    boolean deleteCustomer(Integer id);
}
