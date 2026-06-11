package com.autodealer.crm.service.impl;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.manager.CustomerManager;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.query.CustomerQuery;
import com.autodealer.crm.result.CustomerExcel;
import com.autodealer.crm.service.CustomerService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private CustomerManager customerManager;

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private TTranMapper tTranMapper;

    @Override
    public Boolean convertCustomer(CustomerQuery customerQuery) {
        return customerManager.convertCustomer(customerQuery);
    }

    @Override
    public PageInfo<TCustomer> getCustomerByPage(Integer current) {
        //1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        //2.查询
        List<TCustomer> list = tCustomerMapper.selectCustomerPage();
        //3.封装分页数据到PageInfo
        PageInfo<TCustomer> info = new PageInfo<>(list);

        return info;
    }

    @Override
    public List<CustomerExcel> getCustomerByExcel(List<String> idList) {
        List<CustomerExcel> customerExcelList = new ArrayList<>();

        List<TCustomer> tCustomerList = tCustomerMapper.selectCustomerByExcel(idList);

        //把从数据库查询出来的List<TCustomer>数据，转换为 List<CustomerExcel>数据
        tCustomerList.forEach(tCustomer -> {
            CustomerExcel customerExcel = new CustomerExcel();

            //需要一个一个设置，没有办法，因为没法使用BeanUtils复制
            if (tCustomer.getClueDO() != null) {
                TClue clue = tCustomer.getClueDO();
                customerExcel.setOwnerName(ObjectUtils.isEmpty(clue.getOwnerDO()) ? Constants.EMPTY : clue.getOwnerDO().getName());
                customerExcel.setActivityName(ObjectUtils.isEmpty(clue.getActivityDO()) ? Constants.EMPTY : clue.getActivityDO().getName());
                customerExcel.setFullName(clue.getFullName());
                customerExcel.setPhone(clue.getPhone());
                customerExcel.setWeixin(clue.getWeixin());
                customerExcel.setQq(clue.getQq());
                customerExcel.setEmail(clue.getEmail());
                customerExcel.setAge(clue.getAge() != null ? clue.getAge() : 0);
                customerExcel.setJob(clue.getJob());
                customerExcel.setYearIncome(clue.getYearIncome());
                customerExcel.setAddress(clue.getAddress());
                customerExcel.setAppellationName(ObjectUtils.isEmpty(clue.getAppellationDO()) ? Constants.EMPTY : clue.getAppellationDO().getTypeValue());
                customerExcel.setNeedLoanName(ObjectUtils.isEmpty(clue.getNeedLoanDO()) ? Constants.EMPTY : clue.getNeedLoanDO().getTypeValue());
                customerExcel.setProductName(ObjectUtils.isEmpty(clue.getIntentionProductDO()) ? Constants.EMPTY : clue.getIntentionProductDO().getName());
                customerExcel.setSourceName(ObjectUtils.isEmpty(clue.getSourceDO()) ? Constants.EMPTY : clue.getSourceDO().getTypeValue());
            } else {
                customerExcel.setOwnerName(Constants.EMPTY);
                customerExcel.setActivityName(Constants.EMPTY);
                customerExcel.setFullName(Constants.EMPTY);
                customerExcel.setPhone(Constants.EMPTY);
                customerExcel.setWeixin(Constants.EMPTY);
                customerExcel.setQq(Constants.EMPTY);
                customerExcel.setEmail(Constants.EMPTY);
                customerExcel.setAge(0);
                customerExcel.setJob(Constants.EMPTY);
                customerExcel.setYearIncome(null);
                customerExcel.setAddress(Constants.EMPTY);
                customerExcel.setAppellationName(Constants.EMPTY);
                customerExcel.setNeedLoanName(Constants.EMPTY);
                customerExcel.setProductName(Constants.EMPTY);
                customerExcel.setSourceName(Constants.EMPTY);
            }
            customerExcel.setDescription(tCustomer.getDescription());
            customerExcel.setNextContactTime(tCustomer.getNextContactTime());

            customerExcelList.add(customerExcel);
        });
        return customerExcelList;
    }

    @Override
    public PageInfo<TCustomer> getCustomerList(CustomerQuery query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TCustomer> customerList = tCustomerMapper.selectByQuery(query);
        return new PageInfo<>(customerList);
    }

    @Override
    public List<CustomerOption> getCustomerOptions() {
        return tCustomerMapper.selectCustomerOptions();
    }

    @Override
    public TCustomer getCustomerById(Integer id) {
        return tCustomerMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCustomer(Integer id) {
        // 检查客户是否存在
        TCustomer customer = tCustomerMapper.selectByPrimaryKey(id);
        if (customer == null) {
            return false;
        }

        // 检查是否有未完成的交易
        int tranCount = tTranMapper.selectCountByCustomerId(id);
        if (tranCount > 0) {
            // 检查是否有非终态的交易
            int activeCount = tTranMapper.selectActiveCountByCustomerId(id);
            if (activeCount > 0) {
                throw new RuntimeException("该客户有未完成的交易，无法删除");
            }
        }

        // 删除客户
        int result = tCustomerMapper.deleteByPrimaryKey(id);
        return result > 0;
    }
}
