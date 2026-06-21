package com.autodealer.crm.manager;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.model.TTranProduct;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.TranService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class CustomerManager {
    private static final Logger log = LoggerFactory.getLogger(CustomerManager.class);

    private final TCustomerMapper tCustomerMapper;
    private final TClueMapper tClueMapper;
    private final TProductMapper productMapper;
    private final TranService tranService;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public CustomerManager(TCustomerMapper tCustomerMapper, TClueMapper tClueMapper,
                           TProductMapper productMapper, TranService tranService,
                           CurrentUserProvider currentUserProvider, OperationAuditRecorder auditRecorder) {
        this.tCustomerMapper = tCustomerMapper; this.tClueMapper = tClueMapper;
        this.productMapper = productMapper; this.tranService = tranService;
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
        TTran tTran = new TTran();
        tTran.setCustomerId(tCustomer.getId()); tTran.setStage(TranStage.QUOTATION);
        tTran.setDescription(request.getDescription()); tTran.setNextContactTime(request.getNextContactTime());
        tTran.setCreateBy(operatorId);
        List<TTranProduct> products = new ArrayList<>();
        if (request.getProduct() != null) {
            TProduct product = productMapper.selectById(request.getProduct());
            if (product != null) {
                TTranProduct tp = new TTranProduct(); tp.setProductId(request.getProduct());
                int qty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
                tp.setQuantity(qty); tp.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
                tp.setCreateBy(operatorId); products.add(tp);
            }
        }
        tranService.createTransaction(tTran, products);
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
