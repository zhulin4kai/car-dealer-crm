package com.bjpowernode.service;

import com.bjpowernode.model.CustomerOption;
import com.bjpowernode.model.TCustomer;
import com.bjpowernode.query.CustomerQuery;
import com.bjpowernode.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 客户服务接口
 */
public interface CustomerService {
    
    /**
     * 分页查询客户列表
     * @param query 查询条件
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageInfo<TCustomer> getCustomerList(CustomerQuery query, Integer pageNum, Integer pageSize);
    
    /**
     * 获取所有客户选项（用于下拉选择）
     * @return 客户选项列表
     */
    List<CustomerOption> getCustomerOptions();
    
    /**
     * 根据ID获取客户详情
     * @param id 客户ID
     * @return 客户详情
     */
    TCustomer getCustomerById(Integer id);

    Boolean convertCustomer(CustomerQuery customerQuery);

    PageInfo<TCustomer> getCustomerByPage(Integer current);

    List<CustomerExcel> getCustomerByExcel(List<String> idList);

    /**
     * 删除客户
     * @param id 客户ID
     * @return 是否删除成功
     */
    boolean deleteCustomer(Integer id);
}
