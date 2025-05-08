package com.bjpowernode.service;

import com.bjpowernode.model.TCustomer;
import com.bjpowernode.query.CustomerQuery;
import com.bjpowernode.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CustomerService {

    Boolean convertCustomer(CustomerQuery customerQuery);

    PageInfo<TCustomer> getCustomerByPage(Integer current);

    List<CustomerExcel> getCustomerByExcel(List<String> idList);
}
