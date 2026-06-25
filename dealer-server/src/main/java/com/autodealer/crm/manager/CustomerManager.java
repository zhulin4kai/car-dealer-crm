package com.autodealer.crm.manager;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.result.CodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
public class CustomerManager {
    private static final Logger log = LoggerFactory.getLogger(CustomerManager.class);

    private final TCustomerMapper tCustomerMapper;
    private final TClueMapper tClueMapper;
    private final TProductMapper productMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public CustomerManager(TCustomerMapper tCustomerMapper, TClueMapper tClueMapper,
                           TProductMapper productMapper,
                           CurrentUserProvider currentUserProvider, OperationAuditRecorder auditRecorder) {
        this.tCustomerMapper = tCustomerMapper; this.tClueMapper = tClueMapper;
        this.productMapper = productMapper;
        this.currentUserProvider = currentUserProvider; this.auditRecorder = auditRecorder;
    }

    @Transactional(rollbackFor = Exception.class)
    public void convertCustomer(ConvertCustomerRequest request) {
        Integer operatorId = currentUserProvider.getCurrentUserId();
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        requireClueExists(request.getClueId()); requireQuantityPositive(request.getQuantity());
        if (request.getProduct() != null) { requireProductExists(request.getProduct()); }
        int updateCount = tClueMapper.updateStateToConverted(request.getClueId(), operatorId, dataScopeUserId);
        if (updateCount == 0) { throw new BusinessException(CodeEnum.FAIL, "该线索已经转过客户或您无权限操作"); }
        TCustomer tCustomer = new TCustomer();
        tCustomer.setClueId(request.getClueId()); tCustomer.setProduct(request.getProduct());
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
}
