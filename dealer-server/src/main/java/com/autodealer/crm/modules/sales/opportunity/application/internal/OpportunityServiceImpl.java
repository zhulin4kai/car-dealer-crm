package com.autodealer.crm.modules.sales.opportunity.application.internal;

import com.autodealer.crm.modules.fulfillment.transaction.application.api.port.TransactionDataPort;
import com.autodealer.crm.modules.commerce.quote.application.api.port.QuoteDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.AdvanceOpportunityStageRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.CreateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.OpportunityResultRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.UpdateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.enums.OpportunityStage;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.opportunity.persistence.mapper.TOpportunityMapper;
import com.autodealer.crm.modules.sales.opportunity.persistence.mapper.TOpportunityStageHistoryMapper;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunityStageHistory;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.sales.opportunity.application.api.query.OpportunityQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.opportunity.application.api.OpportunityService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OpportunityServiceImpl implements OpportunityService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<TranStage> ORDER_ESTABLISHED_STAGES = Set.of(
            TranStage.APPROVED,
            TranStage.PAYMENT,
            TranStage.DELIVERY,
            TranStage.COMPLETED
    );
    private static final Map<OpportunityStage, Set<OpportunityStage>> TRANSITIONS = buildTransitions();

    private final TOpportunityMapper opportunityMapper;
    private final TOpportunityStageHistoryMapper stageHistoryMapper;
    private final CustomerDataPort customerMapper;
    private final QuoteDataPort quoteMapper;
    private final TransactionDataPort tranMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final EmploymentResponsibilityGuard responsibilityGuard;

    public OpportunityServiceImpl(TOpportunityMapper opportunityMapper,
                                  TOpportunityStageHistoryMapper stageHistoryMapper,
                                  CustomerDataPort customerMapper,
                                  QuoteDataPort quoteMapper,
                                  TransactionDataPort tranMapper,
                                  CurrentUserProvider currentUserProvider,
                                  OperationAuditRecorder auditRecorder,
                                  EmploymentResponsibilityGuard responsibilityGuard) {
        this.opportunityMapper = opportunityMapper;
        this.stageHistoryMapper = stageHistoryMapper;
        this.customerMapper = customerMapper;
        this.quoteMapper = quoteMapper;
        this.tranMapper = tranMapper;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
        this.responsibilityGuard = responsibilityGuard;
    }

    @Override
    public PageInfo<TOpportunity> getOpportunityPage(OpportunityQuery query) {
        OpportunityQuery safeQuery = query == null ? new OpportunityQuery() : query;
        if (StringUtils.hasText(safeQuery.getStage())) {
            parseStage(safeQuery.getStage());
        }
        int page = safeQuery.getPage() == null || safeQuery.getPage() < 1 ? 1 : safeQuery.getPage();
        int size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 10 : safeQuery.getSize();
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        safeQuery.setDataScopeUserId(currentUserProvider.getDataScopeUserId());
        PageHelper.startPage(page, size);
        return new PageInfo<>(opportunityMapper.selectByQuery(safeQuery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity createOpportunity(CreateOpportunityRequest request) {
        TCustomer customer = requireAccessibleCustomer(request.getCustomerId());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        TOpportunity opportunity = new TOpportunity();
        opportunity.setOpportunityNo(generateOpportunityNo(now));
        opportunity.setCustomerId(customer.getId());
        opportunity.setClueId(resolveClueId(request, customer));
        opportunity.setOwnerId(customer.getOwnerId() == null ? operatorId : customer.getOwnerId());
        responsibilityGuard.requireActiveOwner(opportunity.getOwnerId());
        opportunity.setProductId(request.getProductId());
        opportunity.setSourceType(normalizeNullable(request.getSourceType()));
        opportunity.setStage(OpportunityStage.INITIAL_CONTACT.name());
        opportunity.setRequirement(normalizeRequired(request.getRequirement(), "购车需求不能为空"));
        opportunity.setExpectedAmount(request.getExpectedAmount());
        opportunity.setExpectedCloseDate(request.getExpectedCloseDate());
        opportunity.setNextActionTime(request.getNextActionTime());
        opportunity.setVersion(0);
        opportunity.setCreateTime(now);
        opportunity.setCreateBy(operatorId);
        opportunity.setUpdateTime(now);
        opportunity.setUpdateBy(operatorId);

        int rows = opportunityMapper.insert(opportunity);
        if (rows != 1 || opportunity.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "创建商机失败");
        }
        insertHistory(opportunity.getId(), null, OpportunityStage.INITIAL_CONTACT.name(), "创建商机", operatorId, now);
        auditRecorder.record(AuditActionEnum.OPPORTUNITY_CREATE, opportunity.getId().toString());
        return requireAccessibleOpportunity(opportunity.getId());
    }

    @Override
    public TOpportunity getOpportunity(Long id) {
        return requireAccessibleOpportunity(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity updateOpportunity(Long id, UpdateOpportunityRequest request) {
        if (!id.equals(request.getId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "路径商机ID与请求体不一致");
        }
        TOpportunity current = requireAccessibleOpportunity(id);
        OpportunityStage currentStage = parseStage(current.getStage());
        if (currentStage.terminal()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态商机不允许普通编辑");
        }

        TOpportunity update = new TOpportunity();
        update.setId(id);
        update.setProductId(request.getProductId());
        update.setRequirement(normalizeRequired(request.getRequirement(), "购车需求不能为空"));
        update.setExpectedAmount(request.getExpectedAmount());
        update.setExpectedCloseDate(request.getExpectedCloseDate());
        update.setNextActionTime(request.getNextActionTime());
        update.setUpdateTime(LocalDateTime.now());
        update.setUpdateBy(currentUserProvider.getCurrentUserId());
        int rows = opportunityMapper.updateBasic(update, currentUserProvider.getDataScopeUserId());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "商机已变更，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.OPPORTUNITY_UPDATE, id.toString());
        return requireAccessibleOpportunity(id);
    }

    @Override
    public List<TOpportunityStageHistory> getStageHistory(Long id) {
        requireAccessibleOpportunity(id);
        return stageHistoryMapper.selectByOpportunityId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity advanceStage(Long id, AdvanceOpportunityStageRequest request) {
        TOpportunity opportunity = requireAccessibleOpportunityForUpdate(id);
        OpportunityStage expected = parseStage(request.getExpectedStage());
        OpportunityStage target = parseStage(request.getTargetStage());
        if (!expected.name().equals(opportunity.getStage())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "商机阶段已变更，请刷新后重试");
        }
        if (target.resultStage()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "输赢单、搁置和关闭必须使用独立命令");
        }
        validateTransition(expected, target);
        validateAdvanceEvidence(opportunity, target);
        return changeStage(opportunity, expected, target, request.getReason(), null, null,
                request.getReason(), request.getNextActionTime(), null, AuditActionEnum.OPPORTUNITY_STAGE_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity markWon(Long id, OpportunityResultRequest request) {
        TOpportunity opportunity = requireAccessibleOpportunityForUpdate(id);
        OpportunityStage current = parseStage(opportunity.getStage());
        validateNotTerminal(current);
        Integer orderTranId = request.getOrderTranId();
        if (orderTranId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "赢单必须关联已成立订单");
        }
        TTran transaction = tranMapper.selectByPrimaryKey(orderTranId);
        if (transaction == null || !opportunity.getCustomerId().equals(transaction.getCustomerId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "订单与商机客户不匹配");
        }
        if (!ORDER_ESTABLISHED_STAGES.contains(transaction.getStage())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "订单尚未成立，不能标记赢单");
        }
        return changeStage(opportunity, current, OpportunityStage.WON, request.getReason(), null,
                request.getRemark(), request.getReason(), null, orderTranId, AuditActionEnum.OPPORTUNITY_WIN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity markLost(Long id, OpportunityResultRequest request) {
        TOpportunity opportunity = requireAccessibleOpportunityForUpdate(id);
        OpportunityStage current = parseStage(opportunity.getStage());
        validateNotTerminal(current);
        String reason = normalizeRequired(request.getReason(), "输单原因不能为空");
        return changeStage(opportunity, current, OpportunityStage.LOST, reason,
                normalizeNullable(request.getCompetitor()), normalizeNullable(request.getRemark()), reason,
                null, null, AuditActionEnum.OPPORTUNITY_LOSE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity shelve(Long id, OpportunityResultRequest request) {
        TOpportunity opportunity = requireAccessibleOpportunityForUpdate(id);
        OpportunityStage current = parseStage(opportunity.getStage());
        validateNotTerminal(current);
        String reason = normalizeRequired(request.getReason(), "搁置原因不能为空");
        if (request.getNextActionTime() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "搁置必须填写下次跟进时间");
        }
        return changeStage(opportunity, current, OpportunityStage.SHELVED, reason, null,
                normalizeNullable(request.getRemark()), reason, request.getNextActionTime(), null,
                AuditActionEnum.OPPORTUNITY_SHELVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOpportunity restore(Long id, OpportunityResultRequest request) {
        TOpportunity opportunity = requireAccessibleOpportunityForUpdate(id);
        OpportunityStage current = parseStage(opportunity.getStage());
        if (current != OpportunityStage.SHELVED && current != OpportunityStage.LOST) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有搁置或输单商机可以恢复");
        }
        String reason = normalizeRequired(request.getReason(), "恢复原因不能为空");
        return changeStage(opportunity, current, OpportunityStage.NEEDS_ANALYSIS, reason, null,
                normalizeNullable(request.getRemark()), reason, request.getNextActionTime(), null,
                AuditActionEnum.OPPORTUNITY_RESTORE);
    }

    private TOpportunity changeStage(TOpportunity opportunity,
                                     OpportunityStage expected,
                                     OpportunityStage target,
                                     String historyReason,
                                     String competitor,
                                     String remark,
                                     String resultReason,
                                     LocalDate nextActionTime,
                                     Integer orderTranId,
                                     AuditActionEnum auditAction) {
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int rows = opportunityMapper.updateStageIfCurrent(opportunity.getId(), expected.name(), target.name(),
                resultReason, competitor, remark, nextActionTime, orderTranId, now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "商机阶段已变更，请刷新后重试");
        }
        insertHistory(opportunity.getId(), expected.name(), target.name(), historyReason, operatorId, now);
        auditRecorder.record(auditAction, opportunity.getId().toString());
        return requireAccessibleOpportunity(opportunity.getId());
    }

    private void validateTransition(OpportunityStage expected, OpportunityStage target) {
        Set<OpportunityStage> allowed = TRANSITIONS.getOrDefault(expected, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "商机阶段不允许这样推进");
        }
    }

    private void validateAdvanceEvidence(TOpportunity opportunity, OpportunityStage target) {
        if (target == OpportunityStage.QUOTING
                && quoteMapper.countActiveByOpportunityId(opportunity.getId()) <= 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "进入报价中必须已有有效报价或报价草稿");
        }
        if (target == OpportunityStage.PENDING_APPROVAL
                && opportunity.getExpectedAmount() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "进入待审批必须具备预计成交金额");
        }
    }

    private void validateNotTerminal(OpportunityStage stage) {
        if (stage.terminal()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态商机不能重复处理");
        }
    }

    private TCustomer requireAccessibleCustomer(Integer customerId) {
        if (customerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户不能为空");
        }
        TCustomer customer = customerMapper.selectScopedById(customerId, currentUserProvider.getDataScopeUserId());
        if (customer == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权访问该客户");
        }
        return customer;
    }

    private TOpportunity requireAccessibleOpportunity(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "商机ID不能为空");
        }
        TOpportunity opportunity = opportunityMapper.selectById(id);
        if (!isAccessible(opportunity)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权访问该商机");
        }
        return opportunity;
    }

    private TOpportunity requireAccessibleOpportunityForUpdate(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "商机ID不能为空");
        }
        TOpportunity opportunity = opportunityMapper.selectByIdForUpdate(id);
        if (!isAccessible(opportunity)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权访问该商机");
        }
        return opportunity;
    }

    private boolean isAccessible(TOpportunity opportunity) {
        if (opportunity == null) {
            return false;
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        return dataScopeUserId == null || dataScopeUserId.equals(opportunity.getOwnerId());
    }

    private void insertHistory(Long opportunityId, String fromStage, String toStage,
                               String reason, Integer operatorId, LocalDateTime operateTime) {
        TOpportunityStageHistory history = new TOpportunityStageHistory();
        history.setOpportunityId(opportunityId);
        history.setFromStage(fromStage);
        history.setToStage(toStage);
        history.setReason(reason);
        history.setOperateBy(operatorId);
        history.setOperateTime(operateTime);
        int rows = stageHistoryMapper.insert(history);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "商机阶段历史写入失败");
        }
    }

    private OpportunityStage parseStage(String value) {
        try {
            return OpportunityStage.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long resolveClueId(CreateOpportunityRequest request, TCustomer customer) {
        if (request.getClueId() != null) {
            return request.getClueId();
        }
        return customer.getClueId() == null ? null : customer.getClueId().longValue();
    }

    private String generateOpportunityNo(LocalDateTime now) {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "OPP" + now.format(NO_DATE_FORMATTER) + suffix;
    }

    private static Map<OpportunityStage, Set<OpportunityStage>> buildTransitions() {
        Map<OpportunityStage, Set<OpportunityStage>> map = new EnumMap<>(OpportunityStage.class);
        map.put(OpportunityStage.INITIAL_CONTACT, Set.of(OpportunityStage.NEEDS_ANALYSIS, OpportunityStage.SHELVED));
        map.put(OpportunityStage.NEEDS_ANALYSIS, Set.of(OpportunityStage.VEHICLE_MATCHING, OpportunityStage.SHELVED));
        map.put(OpportunityStage.VEHICLE_MATCHING, Set.of(OpportunityStage.TEST_DRIVE_INVITED,
                OpportunityStage.QUOTING, OpportunityStage.SHELVED));
        map.put(OpportunityStage.TEST_DRIVE_INVITED, Set.of(OpportunityStage.QUOTING,
                OpportunityStage.NEGOTIATION, OpportunityStage.SHELVED));
        map.put(OpportunityStage.QUOTING, Set.of(OpportunityStage.NEGOTIATION,
                OpportunityStage.PENDING_APPROVAL, OpportunityStage.SHELVED));
        map.put(OpportunityStage.NEGOTIATION, Set.of(OpportunityStage.PENDING_APPROVAL,
                OpportunityStage.SHELVED));
        map.put(OpportunityStage.PENDING_APPROVAL, Set.of(OpportunityStage.SHELVED));
        map.put(OpportunityStage.SHELVED, Set.of(OpportunityStage.NEEDS_ANALYSIS));
        return map;
    }
}
