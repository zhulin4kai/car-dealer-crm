package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PaginationConstants;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.dto.CustomerDetailResponse;
import com.autodealer.crm.dto.CustomerListResponse;
import com.autodealer.crm.manager.CustomerManager;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.query.CustomerListQuery;
import com.autodealer.crm.result.CustomerExcel;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.service.CustomerService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final int EXPORT_MAX_ROWS = 10000;

    private final CustomerManager customerManager;
    private final TCustomerMapper tCustomerMapper;
    private final TTranMapper tTranMapper;
    private final CurrentUserProvider currentUserProvider;

    public CustomerServiceImpl(CustomerManager customerManager,
                               TCustomerMapper tCustomerMapper,
                               TTranMapper tTranMapper,
                               CurrentUserProvider currentUserProvider) {
        this.customerManager = customerManager;
        this.tCustomerMapper = tCustomerMapper;
        this.tTranMapper = tTranMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void convertCustomer(ConvertCustomerRequest request) {
        customerManager.convertCustomer(request);
    }

    @Override
    public PageInfo<CustomerListResponse> getCustomerByPage(Integer current) {
        PageHelper.startPage(current, PaginationConstants.DEFAULT_PAGE_SIZE);
        List<TCustomer> list = tCustomerMapper.selectByQuery(new CustomerListQuery());
        PageInfo<TCustomer> rawPage = new PageInfo<>(list);
        List<CustomerListResponse> responses = toListResponse(list);
        PageInfo<CustomerListResponse> pageInfo = new PageInfo<>();
        pageInfo.setList(responses);
        pageInfo.setTotal(rawPage.getTotal());
        pageInfo.setPageNum(rawPage.getPageNum());
        pageInfo.setPageSize(rawPage.getPageSize());
        pageInfo.setPages(rawPage.getPages());
        return pageInfo;
    }

    @Override
    public List<CustomerExcel> getCustomerByExcel(List<String> idList) {
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        int count = tCustomerMapper.countCustomerByExcel(idList, dataScopeUserId);
        if (count > EXPORT_MAX_ROWS) {
            throw new BusinessException(CodeEnum.FAIL,
                    "导出数据量超出限制，最多导出 " + EXPORT_MAX_ROWS + " 条，当前为 " + count + " 条");
        }
        if (count == 0) {
            return Collections.emptyList();
        }
        Integer maxRows = (idList == null || idList.isEmpty()) ? EXPORT_MAX_ROWS : null;
        List<TCustomer> tCustomerList = tCustomerMapper.selectCustomerByExcel(
                idList, dataScopeUserId, maxRows);
        return tCustomerList.stream().map(this::toExcel).collect(Collectors.toList());
    }

    @Override
    public PageInfo<CustomerListResponse> getCustomerList(CustomerListQuery query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TCustomer> customerList = tCustomerMapper.selectByQuery(query);
        PageInfo<TCustomer> rawPage = new PageInfo<>(customerList);
        List<CustomerListResponse> responses = toListResponse(customerList);
        PageInfo<CustomerListResponse> pageInfo = new PageInfo<>();
        pageInfo.setList(responses);
        pageInfo.setTotal(rawPage.getTotal());
        pageInfo.setPageNum(rawPage.getPageNum());
        pageInfo.setPageSize(rawPage.getPageSize());
        pageInfo.setPages(rawPage.getPages());
        return pageInfo;
    }

    @Override
    public List<CustomerOption> getCustomerOptions() {
        return tCustomerMapper.selectCustomerOptions(currentUserProvider.getDataScopeUserId());
    }

    @Override
    public CustomerDetailResponse getCustomerById(Integer id) {
        TCustomer customer = tCustomerMapper.selectScopedById(id, currentUserProvider.getDataScopeUserId());
        if (customer == null) { return null; }
        return toDetailResponse(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCustomer(Integer id) {
        TCustomer customer = tCustomerMapper.selectByPrimaryKey(id);
        if (customer == null) { return false; }
        int tranCount = tTranMapper.selectCountByCustomerId(id);
        if (tranCount > 0) {
            int activeCount = tTranMapper.selectActiveCountByCustomerId(id);
            if (activeCount > 0) {
                throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "该客户有未完成的交易，无法删除");
            }
        }
        int result = tCustomerMapper.deleteByPrimaryKey(id);
        return result > 0;
    }

    private List<CustomerListResponse> toListResponse(List<TCustomer> customers) {
        List<CustomerListResponse> result = new ArrayList<>();
        for (TCustomer c : customers) {
            CustomerListResponse dto = new CustomerListResponse();
            dto.setId(c.getId()); dto.setClueId(c.getClueId()); dto.setProduct(c.getProduct());
            dto.setDescription(c.getDescription()); dto.setNextContactTime(c.getNextContactTime());
            dto.setCreateTime(c.getCreateTime());
            if (c.getClueDO() != null) {
                TClue clue = c.getClueDO();
                dto.setCustomerName(clue.getFullName()); dto.setPhone(clue.getPhone()); dto.setWeixin(clue.getWeixin());
                if (clue.getOwnerDO() != null) dto.setOwnerName(clue.getOwnerDO().getName());
                if (clue.getActivityDO() != null) dto.setActivityName(clue.getActivityDO().getName());
                if (clue.getAppellationDO() != null) dto.setAppellationName(clue.getAppellationDO().getTypeValue());
                if (clue.getNeedLoanDO() != null) dto.setNeedLoanName(clue.getNeedLoanDO().getTypeValue());
                if (clue.getIntentionStateDO() != null) dto.setIntentionStateName(clue.getIntentionStateDO().getTypeValue());
                if (clue.getStateDO() != null) dto.setStateName(clue.getStateDO().getTypeValue());
                if (clue.getSourceDO() != null) dto.setSourceName(clue.getSourceDO().getTypeValue());
                if (clue.getIntentionProductDO() != null) dto.setIntentionProductName(clue.getIntentionProductDO().getName());
            }
            result.add(dto);
        }
        return result;
    }

    private CustomerDetailResponse toDetailResponse(TCustomer c) {
        CustomerDetailResponse dto = new CustomerDetailResponse();
        dto.setId(c.getId()); dto.setClueId(c.getClueId()); dto.setProduct(c.getProduct());
        dto.setDescription(c.getDescription()); dto.setNextContactTime(c.getNextContactTime());
        dto.setCreateTime(c.getCreateTime());
        if (c.getClueDO() != null) {
            TClue clue = c.getClueDO();
            dto.setCustomerName(clue.getFullName()); dto.setPhone(clue.getPhone()); dto.setWeixin(clue.getWeixin());
            dto.setQq(clue.getQq()); dto.setEmail(clue.getEmail()); dto.setAge(clue.getAge());
            dto.setJob(clue.getJob());
            dto.setYearIncome(clue.getYearIncome() != null ? clue.getYearIncome().toString() : null);
            dto.setAddress(clue.getAddress());
            if (clue.getOwnerDO() != null) dto.setOwnerName(clue.getOwnerDO().getName());
            if (clue.getActivityDO() != null) dto.setActivityName(clue.getActivityDO().getName());
            if (clue.getAppellationDO() != null) dto.setAppellationName(clue.getAppellationDO().getTypeValue());
            if (clue.getNeedLoanDO() != null) dto.setNeedLoanName(clue.getNeedLoanDO().getTypeValue());
            if (clue.getSourceDO() != null) dto.setSourceName(clue.getSourceDO().getTypeValue());
            if (clue.getIntentionProductDO() != null) dto.setProductName(clue.getIntentionProductDO().getName());
        }
        return dto;
    }

    private CustomerExcel toExcel(TCustomer tCustomer) {
        CustomerExcel customerExcel = new CustomerExcel();
        if (tCustomer.getClueDO() != null) {
            TClue clue = tCustomer.getClueDO();
            customerExcel.setOwnerName(clue.getOwnerDO() != null ? clue.getOwnerDO().getName() : "");
            customerExcel.setActivityName(clue.getActivityDO() != null ? clue.getActivityDO().getName() : "");
            customerExcel.setFullName(clue.getFullName()); customerExcel.setPhone(clue.getPhone());
            customerExcel.setWeixin(clue.getWeixin()); customerExcel.setQq(clue.getQq());
            customerExcel.setEmail(clue.getEmail());
            customerExcel.setAge(clue.getAge() != null ? clue.getAge() : 0);
            customerExcel.setJob(clue.getJob()); customerExcel.setYearIncome(clue.getYearIncome());
            customerExcel.setAddress(clue.getAddress());
            customerExcel.setAppellationName(clue.getAppellationDO() != null ? clue.getAppellationDO().getTypeValue() : "");
            customerExcel.setNeedLoanName(clue.getNeedLoanDO() != null ? clue.getNeedLoanDO().getTypeValue() : "");
            customerExcel.setProductName(clue.getIntentionProductDO() != null ? clue.getIntentionProductDO().getName() : "");
            customerExcel.setSourceName(clue.getSourceDO() != null ? clue.getSourceDO().getTypeValue() : "");
        }
        customerExcel.setDescription(tCustomer.getDescription());
        customerExcel.setNextContactTime(tCustomer.getNextContactTime());
        return customerExcel;
    }
}
