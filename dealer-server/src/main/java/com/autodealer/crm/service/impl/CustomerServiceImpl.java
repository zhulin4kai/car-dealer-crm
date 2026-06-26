package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PaginationConstants;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.dto.CustomerDetailResponse;
import com.autodealer.crm.dto.CustomerListResponse;
import com.autodealer.crm.dto.CustomerMergeResponse;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.dto.MergeCustomerRequest;
import com.autodealer.crm.dto.TransferCustomerOwnerRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.CustomerManager;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TCustomerOwnerHistoryMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TCustomerOwnerHistory;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.CustomerListQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.CustomerExcel;
import com.autodealer.crm.service.CustomerService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final int EXPORT_MAX_ROWS = 10000;
    private static final String STATUS_MERGED = "MERGED";
    private static final Map<String, String> CUSTOMER_STATUS_NAMES = Map.ofEntries(
            Map.entry("POTENTIAL", "潜在"),
            Map.entry("INTENTION", "意向"),
            Map.entry("VISITED", "到店"),
            Map.entry("TEST_DRIVE", "试驾"),
            Map.entry("DEAL", "成交"),
            Map.entry("REPURCHASE", "复购"),
            Map.entry("DORMANT", "休眠"),
            Map.entry("LOST", "流失"),
            Map.entry("INVALID", "无效"),
            Map.entry(STATUS_MERGED, "已合并")
    );

    private final CustomerManager customerManager;
    private final TCustomerMapper tCustomerMapper;
    private final TUserMapper tUserMapper;
    private final TCustomerOwnerHistoryMapper tCustomerOwnerHistoryMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public CustomerServiceImpl(CustomerManager customerManager,
                               TCustomerMapper tCustomerMapper,
                               TUserMapper tUserMapper,
                               TCustomerOwnerHistoryMapper tCustomerOwnerHistoryMapper,
                               CurrentUserProvider currentUserProvider,
                               OperationAuditRecorder auditRecorder) {
        this.customerManager = customerManager;
        this.tCustomerMapper = tCustomerMapper;
        this.tUserMapper = tUserMapper;
        this.tCustomerOwnerHistoryMapper = tCustomerOwnerHistoryMapper;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public void convertCustomer(ConvertCustomerRequest request) {
        customerManager.convertCustomer(request);
    }

    @Override
    public PageInfo<CustomerListResponse> getCustomerByPage(Integer current) {
        PageHelper.startPage(current, PaginationConstants.DEFAULT_PAGE_SIZE);
        List<TCustomer> list = tCustomerMapper.selectByQuery(new CustomerListQuery());
        return toPageInfo(list);
    }

    @Override
    public List<CustomerExcel> getCustomerByExcel(List<String> idList) {
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        int count = tCustomerMapper.countCustomerByExcel(idList, dataScopeUserId);
        if (count > EXPORT_MAX_ROWS) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED,
                    "导出数据量超出限制，最多导出 " + EXPORT_MAX_ROWS + " 条，当前为 " + count + " 条");
        }
        if (count == 0) {
            return Collections.emptyList();
        }
        Integer maxRows = (idList == null || idList.isEmpty()) ? EXPORT_MAX_ROWS : null;
        List<TCustomer> tCustomerList = tCustomerMapper.selectCustomerByExcel(
                idList, dataScopeUserId, maxRows);
        boolean canViewSensitive = canViewSensitive();
        return tCustomerList.stream().map(customer -> toExcel(customer, canViewSensitive)).collect(Collectors.toList());
    }

    @Override
    public PageInfo<CustomerListResponse> getCustomerList(CustomerListQuery query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TCustomer> customerList = tCustomerMapper.selectByQuery(query);
        return toPageInfo(customerList);
    }

    @Override
    public List<CustomerOption> getCustomerOptions() {
        return tCustomerMapper.selectCustomerOptions(currentUserProvider.getDataScopeUserId());
    }

    @Override
    public CustomerDetailResponse getCustomerById(Integer id) {
        TCustomer customer = tCustomerMapper.selectScopedById(id, currentUserProvider.getDataScopeUserId());
        if (customer == null) {
            return null;
        }
        return toDetailResponse(customer, canViewSensitive());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCustomer(Integer id) {
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        TCustomer customer = tCustomerMapper.selectScopedById(id, dataScopeUserId);
        if (customer == null) {
            return false;
        }
        int referenceCount = tCustomerMapper.countBusinessReferences(id);
        if (referenceCount > 0) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "该客户存在业务关系，无法删除");
        }
        int result = tCustomerMapper.deleteScopedByPrimaryKey(id, dataScopeUserId);
        if (result > 0) {
            auditRecorder.recordQuietly(AuditActionEnum.CUSTOMER_DELETE, String.valueOf(id),
                    "SUCCESS", "{\"customerId\":" + id + "}");
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Integer id, TransferCustomerOwnerRequest request) {
        String reason = normalizeReason(request.getReason(), "转移原因不能为空");
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        Integer operatorId = currentUserProvider.getCurrentUserId();
        TCustomer customer = tCustomerMapper.selectScopedById(id, dataScopeUserId);
        if (customer == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "客户不存在或无权限操作");
        }
        if (STATUS_MERGED.equals(customer.getCustomerStatus())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "已合并客户不能转移归属");
        }
        Integer fromOwnerId = customer.getOwnerId();
        Integer toOwnerId = request.getNewOwnerId();
        if (toOwnerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "目标负责人不能为空");
        }
        if (toOwnerId.equals(fromOwnerId)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "目标负责人不能与原负责人相同");
        }
        requireEnabledOwner(toOwnerId);
        int updated = tCustomerMapper.updateOwnerAtomic(id, fromOwnerId, toOwnerId, operatorId, dataScopeUserId);
        if (updated != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "客户归属已变化，请刷新后重试");
        }
        TCustomerOwnerHistory history = new TCustomerOwnerHistory();
        history.setCustomerId(id);
        history.setFromOwnerId(fromOwnerId);
        history.setToOwnerId(toOwnerId);
        history.setReason(reason);
        history.setOperatorId(operatorId);
        history.setTransferTime(new Date());
        tCustomerOwnerHistoryMapper.insert(history);
        auditRecorder.recordQuietly(AuditActionEnum.CUSTOMER_OWNER_CHANGE, String.valueOf(id),
                "SUCCESS", "{\"customerId\":" + id + ",\"fromOwnerId\":" + fromOwnerId
                        + ",\"toOwnerId\":" + toOwnerId + "}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerMergeResponse mergeCustomer(Integer targetCustomerId, MergeCustomerRequest request) {
        String reason = normalizeReason(request.getReason(), "合并原因不能为空");
        Integer sourceCustomerId = request.getSourceCustomerId();
        if (sourceCustomerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "被合并客户不能为空");
        }
        if (sourceCustomerId.equals(targetCustomerId)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "主客户和被合并客户不能相同");
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        Integer operatorId = currentUserProvider.getCurrentUserId();
        TCustomer target = tCustomerMapper.selectScopedById(targetCustomerId, dataScopeUserId);
        TCustomer source = tCustomerMapper.selectScopedById(sourceCustomerId, dataScopeUserId);
        if (target == null || source == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "客户不存在或无权限操作");
        }
        if (STATUS_MERGED.equals(target.getCustomerStatus())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "主客户已被合并，不能作为合并目标");
        }
        if (STATUS_MERGED.equals(source.getCustomerStatus())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "被合并客户已经合并");
        }

        int remarkCount = tCustomerMapper.reassignCustomerRemarks(sourceCustomerId, targetCustomerId);
        int tranCount = tCustomerMapper.reassignTransactions(sourceCustomerId, targetCustomerId);
        int quoteCount = tCustomerMapper.reassignQuotes(sourceCustomerId, targetCustomerId);
        int merged = tCustomerMapper.markMerged(sourceCustomerId, targetCustomerId, reason, operatorId, dataScopeUserId);
        if (merged != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "客户合并状态已变化，请刷新后重试");
        }
        auditRecorder.recordQuietly(AuditActionEnum.CUSTOMER_MERGE, String.valueOf(targetCustomerId),
                "SUCCESS", "{\"targetCustomerId\":" + targetCustomerId
                        + ",\"sourceCustomerId\":" + sourceCustomerId + "}");
        CustomerMergeResponse response = new CustomerMergeResponse();
        response.setTargetCustomerId(targetCustomerId);
        response.setSourceCustomerId(sourceCustomerId);
        response.setMigratedRemarkCount(remarkCount);
        response.setMigratedTranCount(tranCount);
        response.setMigratedQuoteCount(quoteCount);
        return response;
    }

    private PageInfo<CustomerListResponse> toPageInfo(List<TCustomer> customers) {
        PageInfo<TCustomer> rawPage = new PageInfo<>(customers);
        List<CustomerListResponse> responses = toListResponse(customers);
        PageInfo<CustomerListResponse> pageInfo = new PageInfo<>();
        pageInfo.setList(responses);
        pageInfo.setTotal(rawPage.getTotal());
        pageInfo.setPageNum(rawPage.getPageNum());
        pageInfo.setPageSize(rawPage.getPageSize());
        pageInfo.setPages(rawPage.getPages());
        return pageInfo;
    }

    private List<CustomerListResponse> toListResponse(List<TCustomer> customers) {
        boolean canViewSensitive = canViewSensitive();
        List<CustomerListResponse> result = new ArrayList<>();
        for (TCustomer c : customers) {
            CustomerListResponse dto = new CustomerListResponse();
            dto.setId(c.getId());
            dto.setClueId(c.getClueId());
            dto.setProduct(c.getProduct());
            dto.setDescription(c.getDescription());
            dto.setNextContactTime(c.getNextContactTime());
            dto.setCreateTime(c.getCreateTime());
            dto.setCustomerName(c.getCustomerName());
            dto.setPhone(maskPhone(c.getPhone(), canViewSensitive));
            dto.setWeixin(maskGeneric(c.getWeixin(), canViewSensitive));
            dto.setOwnerName(c.getOwnerDO() != null ? c.getOwnerDO().getName() : null);
            dto.setActivityName(c.getActivityDO() != null ? c.getActivityDO().getName() : null);
            dto.setAppellationName(c.getAppellationDO() != null ? c.getAppellationDO().getTypeValue() : null);
            dto.setNeedLoanName(c.getNeedLoanDO() != null ? c.getNeedLoanDO().getTypeValue() : null);
            dto.setIntentionStateName(c.getIntentionStateDO() != null ? c.getIntentionStateDO().getTypeValue() : null);
            dto.setSourceName(c.getSourceDO() != null ? c.getSourceDO().getTypeValue() : null);
            dto.setOriginalSourceName(c.getOriginalClueSourceDO() != null ? c.getOriginalClueSourceDO().getTypeValue() : null);
            dto.setIntentionProductName(c.getProductDO() != null ? c.getProductDO().getName() : null);
            dto.setCustomerStatus(c.getCustomerStatus());
            dto.setCustomerStatusName(customerStatusName(c.getCustomerStatus()));
            dto.setStateName(dto.getCustomerStatusName());
            result.add(dto);
        }
        return result;
    }

    private CustomerDetailResponse toDetailResponse(TCustomer c, boolean canViewSensitive) {
        CustomerDetailResponse dto = new CustomerDetailResponse();
        dto.setId(c.getId());
        dto.setClueId(c.getClueId());
        dto.setProduct(c.getProduct());
        dto.setDescription(c.getDescription());
        dto.setNextContactTime(c.getNextContactTime());
        dto.setCreateTime(c.getCreateTime());
        dto.setCustomerName(c.getCustomerName());
        dto.setPhone(maskPhone(c.getPhone(), canViewSensitive));
        dto.setWeixin(maskGeneric(c.getWeixin(), canViewSensitive));
        dto.setQq(maskGeneric(c.getQq(), canViewSensitive));
        dto.setEmail(maskEmail(c.getEmail(), canViewSensitive));
        dto.setAge(c.getAge());
        dto.setJob(c.getJob());
        dto.setYearIncome(c.getYearIncome() != null ? c.getYearIncome().toString() : null);
        dto.setAddress(maskAddress(c.getAddress(), canViewSensitive));
        dto.setOwnerName(c.getOwnerDO() != null ? c.getOwnerDO().getName() : null);
        dto.setActivityName(c.getActivityDO() != null ? c.getActivityDO().getName() : null);
        dto.setAppellationName(c.getAppellationDO() != null ? c.getAppellationDO().getTypeValue() : null);
        dto.setNeedLoanName(c.getNeedLoanDO() != null ? c.getNeedLoanDO().getTypeValue() : null);
        dto.setIntentionStateName(c.getIntentionStateDO() != null ? c.getIntentionStateDO().getTypeValue() : null);
        dto.setSourceName(c.getSourceDO() != null ? c.getSourceDO().getTypeValue() : null);
        dto.setOriginalSourceName(c.getOriginalClueSourceDO() != null ? c.getOriginalClueSourceDO().getTypeValue() : null);
        dto.setProductName(c.getProductDO() != null ? c.getProductDO().getName() : null);
        dto.setCustomerStatus(c.getCustomerStatus());
        dto.setCustomerStatusName(customerStatusName(c.getCustomerStatus()));
        dto.setStateName(dto.getCustomerStatusName());
        return dto;
    }

    private CustomerExcel toExcel(TCustomer tCustomer, boolean canViewSensitive) {
        CustomerExcel customerExcel = new CustomerExcel();
        customerExcel.setOwnerName(tCustomer.getOwnerDO() != null ? tCustomer.getOwnerDO().getName() : "");
        customerExcel.setActivityName(tCustomer.getActivityDO() != null ? tCustomer.getActivityDO().getName() : "");
        customerExcel.setFullName(tCustomer.getCustomerName());
        customerExcel.setPhone(maskPhone(tCustomer.getPhone(), canViewSensitive));
        customerExcel.setWeixin(maskGeneric(tCustomer.getWeixin(), canViewSensitive));
        customerExcel.setQq(maskGeneric(tCustomer.getQq(), canViewSensitive));
        customerExcel.setEmail(maskEmail(tCustomer.getEmail(), canViewSensitive));
        customerExcel.setAge(tCustomer.getAge() != null ? tCustomer.getAge() : 0);
        customerExcel.setJob(tCustomer.getJob());
        customerExcel.setYearIncome(tCustomer.getYearIncome());
        customerExcel.setAddress(maskAddress(tCustomer.getAddress(), canViewSensitive));
        customerExcel.setAppellationName(tCustomer.getAppellationDO() != null ? tCustomer.getAppellationDO().getTypeValue() : "");
        customerExcel.setNeedLoanName(tCustomer.getNeedLoanDO() != null ? tCustomer.getNeedLoanDO().getTypeValue() : "");
        customerExcel.setProductName(tCustomer.getProductDO() != null ? tCustomer.getProductDO().getName() : "");
        customerExcel.setSourceName(tCustomer.getSourceDO() != null ? tCustomer.getSourceDO().getTypeValue() : "");
        customerExcel.setDescription(tCustomer.getDescription());
        customerExcel.setNextContactTime(tCustomer.getNextContactTime());
        return customerExcel;
    }

    private boolean canViewSensitive() {
        return currentUserProvider.hasAuthority(PermissionCodes.CUSTOMER_SENSITIVE_VIEW);
    }

    private void requireEnabledOwner(Integer ownerId) {
        TUser owner = tUserMapper.selectByPrimaryKey(ownerId);
        if (owner == null || !enabled(owner)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "目标负责人无效或已停用");
        }
    }

    private boolean enabled(TUser owner) {
        return Integer.valueOf(1).equals(owner.getAccountEnabled())
                && Integer.valueOf(1).equals(owner.getAccountNoLocked())
                && Integer.valueOf(1).equals(owner.getAccountNoExpired())
                && Integer.valueOf(1).equals(owner.getCredentialsNoExpired());
    }

    private String normalizeReason(String reason, String message) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 255) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "原因长度不能超过255个字符");
        }
        return trimmed;
    }

    private String customerStatusName(String status) {
        if (!StringUtils.hasText(status)) {
            return CUSTOMER_STATUS_NAMES.get("INTENTION");
        }
        return CUSTOMER_STATUS_NAMES.getOrDefault(status, status);
    }

    private String maskPhone(String phone, boolean canViewSensitive) {
        if (canViewSensitive || !StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskGeneric(String value, boolean canViewSensitive) {
        if (canViewSensitive || !StringUtils.hasText(value) || value.length() <= 5) {
            return value;
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 3);
    }

    private String maskEmail(String email, boolean canViewSensitive) {
        if (canViewSensitive || !StringUtils.hasText(email)) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskGeneric(email, false);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskAddress(String address, boolean canViewSensitive) {
        if (canViewSensitive || !StringUtils.hasText(address) || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "***";
    }
}
