package com.autodealer.crm.modules.sales.customer.application.api;

import com.autodealer.crm.modules.sales.customer.application.api.dto.ConvertCustomerRequest;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerDetailResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerListResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerMergeResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerOption;
import com.autodealer.crm.modules.sales.customer.application.api.dto.MergeCustomerRequest;
import com.autodealer.crm.modules.sales.customer.application.api.dto.TransferCustomerOwnerRequest;
import com.autodealer.crm.modules.sales.customer.application.api.query.CustomerListQuery;
import com.autodealer.crm.modules.sales.customer.application.api.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CustomerService {

    PageInfo<CustomerListResponse> getCustomerList(CustomerListQuery query, Integer pageNum, Integer pageSize);

    List<CustomerOption> getCustomerOptions();

    CustomerDetailResponse getCustomerById(Integer id);

    void convertCustomer(ConvertCustomerRequest request);

    List<CustomerExcel> getCustomerByExcel(List<String> idList);

    boolean deleteCustomer(Integer id);

    void transferOwner(Integer id, TransferCustomerOwnerRequest request);

    CustomerMergeResponse mergeCustomer(Integer targetCustomerId, MergeCustomerRequest request);
}
