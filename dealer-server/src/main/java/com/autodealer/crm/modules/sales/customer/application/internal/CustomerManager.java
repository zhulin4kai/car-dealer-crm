package com.autodealer.crm.modules.sales.customer.application.internal;

import com.autodealer.crm.modules.commerce.catalog.application.api.port.ProductCatalogDataPort;
import com.autodealer.crm.modules.sales.lead.application.api.port.LeadDataPort;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.customer.application.api.dto.ConvertCustomerRequest;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerDuplicateResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerDuplicateSummary;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.customer.persistence.mapper.TCustomerMapper;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.PhoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;

import java.util.Date;
import java.util.List;

@Component
public class CustomerManager {
    private static final Logger log = LoggerFactory.getLogger(CustomerManager.class);

    private final TCustomerMapper tCustomerMapper;
    private final LeadDataPort tClueMapper;
    private final ProductCatalogDataPort productMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final EmploymentResponsibilityGuard responsibilityGuard;

    public CustomerManager(TCustomerMapper tCustomerMapper, LeadDataPort tClueMapper,
                           ProductCatalogDataPort productMapper,
                           CurrentUserProvider currentUserProvider, OperationAuditRecorder auditRecorder,
                           EmploymentResponsibilityGuard responsibilityGuard) {
        this.tCustomerMapper = tCustomerMapper; this.tClueMapper = tClueMapper;
        this.productMapper = productMapper;
        this.currentUserProvider = currentUserProvider; this.auditRecorder = auditRecorder;
        this.responsibilityGuard = responsibilityGuard;
    }

    @Transactional(rollbackFor = Exception.class)
    public void convertCustomer(ConvertCustomerRequest request) {
        Integer operatorId = currentUserProvider.getCurrentUserId();
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        requireClueExists(request.getClueId()); requireQuantityPositive(request.getQuantity());
        TClue clue = tClueMapper.selectScopedByPrimaryKey(request.getClueId(), dataScopeUserId);
        if (clue == null) {
            throw new BusinessException(CodeEnum.FAIL, "线索不存在或您无权限操作");
        }
        responsibilityGuard.requireActiveOwner(clue.getOwnerId());
        String normalizedPhone = normalizePhone(clue.getPhone());
        checkDuplicateCustomer(clue, normalizedPhone, dataScopeUserId);
        if (request.getProduct() != null) { requireProductExists(request.getProduct()); }
        int updateCount = tClueMapper.updateStateToConverted(request.getClueId(), operatorId, dataScopeUserId);
        if (updateCount == 0) { throw new BusinessException(CodeEnum.FAIL, "该线索已经转过客户或您无权限操作"); }
        TCustomer tCustomer = new TCustomer();
        tCustomer.setClueId(request.getClueId());
        tCustomer.setOwnerId(clue.getOwnerId());
        tCustomer.setActivityId(clue.getActivityId());
        tCustomer.setActivityNameSnapshot(clue.getActivityNameSnapshot());
        tCustomer.setCustomerName(clue.getFullName());
        tCustomer.setAppellation(clue.getAppellation());
        tCustomer.setPhone(normalizedPhone);
        tCustomer.setWeixin(clue.getWeixin());
        tCustomer.setQq(clue.getQq());
        tCustomer.setEmail(clue.getEmail());
        tCustomer.setAge(clue.getAge());
        tCustomer.setJob(clue.getJob());
        tCustomer.setYearIncome(clue.getYearIncome());
        tCustomer.setAddress(clue.getAddress());
        tCustomer.setNeedLoan(clue.getNeedLoan());
        tCustomer.setIntentionState(clue.getIntentionState());
        tCustomer.setSource(clue.getSource());
        tCustomer.setOriginalClueSource(clue.getSource());
        tCustomer.setProduct(request.getProduct() != null ? request.getProduct() : toLong(clue.getIntentionProduct()));
        tCustomer.setCustomerStatus("INTENTION");
        tCustomer.setDescription(request.getDescription()); tCustomer.setNextContactTime(request.getNextContactTime());
        tCustomer.setCreateTime(new Date()); tCustomer.setCreateBy(operatorId);
        int insert = tCustomerMapper.insertSelective(tCustomer);
        if (insert < 1) { throw new BusinessException(CodeEnum.FAIL, "客户记录插入失败"); }
        auditRecorder.record(AuditActionEnum.CUSTOMER_CONVERT, String.valueOf(tCustomer.getId()),
                "SUCCESS", "{\"clueId\":" + request.getClueId() + ",\"operatorId\":" + operatorId + "}");
        log.info("event=customer_convert result=success clueId={} customerId={} operatorId={}",
                request.getClueId(), tCustomer.getId(), operatorId);
    }

    private void requireClueExists(Integer clueId) {
        if (clueId == null) { throw new BusinessException(CodeEnum.PARAM_ERROR, "线索ID不能为空"); }
    }
    private void requireQuantityPositive(Integer quantity) {
        if (quantity != null && quantity <= 0) { throw new BusinessException(CodeEnum.PARAM_ERROR, "购买数量必须大于0"); }
    }
    private void requireProductExists(Long productId) {
        TProduct product = productMapper.selectById(productId);
        if (product == null) { throw new BusinessException(CodeEnum.FAIL, "选购的产品不存在"); }
    }

    private String normalizePhone(String phone) {
        String normalized = PhoneNormalizer.normalizeMainlandMobile(phone);
        if (normalized != null && !PhoneNormalizer.isMainlandMobile(normalized)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户手机号格式有误");
        }
        return normalized;
    }

    private void checkDuplicateCustomer(TClue clue, String normalizedPhone, Integer dataScopeUserId) {
        int duplicates = tCustomerMapper.countActiveDuplicateContacts(
                normalizedPhone, clue.getWeixin(), clue.getFullName(), null);
        if (duplicates <= 0) {
            return;
        }
        List<CustomerDuplicateSummary> visible = tCustomerMapper.selectVisibleDuplicateSummaries(
                normalizedPhone, clue.getWeixin(), clue.getFullName(), null, dataScopeUserId, 5);
        if (visible == null) {
            visible = List.of();
        }
        for (CustomerDuplicateSummary summary : visible) {
            summary.setMaskedPhone(maskPhone(summary.getMaskedPhone()));
        }
        CustomerDuplicateResponse response = new CustomerDuplicateResponse();
        response.setDuplicated(true);
        response.setHiddenConflict(visible.size() < duplicates);
        response.setVisibleCustomers(visible);
        throw new BusinessException(CodeEnum.DUPLICATE, "存在疑似重复客户", response);
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
