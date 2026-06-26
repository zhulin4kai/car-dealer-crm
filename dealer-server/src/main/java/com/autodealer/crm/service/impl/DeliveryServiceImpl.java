package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CreateDeliveryRequest;
import com.autodealer.crm.dto.DeliveryCheckItemRequest;
import com.autodealer.crm.dto.DeliveryExceptionRequest;
import com.autodealer.crm.dto.SignDeliveryRequest;
import com.autodealer.crm.dto.UpdateDeliveryCheckItemRequest;
import com.autodealer.crm.enums.DeliveryCheckStatus;
import com.autodealer.crm.enums.DeliveryStatus;
import com.autodealer.crm.enums.ProductVehicleStatus;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TDeliveryCheckItemMapper;
import com.autodealer.crm.mapper.TDeliveryMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TProductVehicleMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.model.TDeliveryCheckItem;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.query.DeliveryQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.DeliveryService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final List<DeliveryCheckItemRequest> DEFAULT_CHECK_ITEMS = defaultCheckItems();
    private static final Set<DeliveryStatus> TERMINAL_STATUSES = Set.of(
            DeliveryStatus.COMPLETED,
            DeliveryStatus.CANCELLED
    );

    private final TDeliveryMapper deliveryMapper;
    private final TDeliveryCheckItemMapper checkItemMapper;
    private final TTranMapper tranMapper;
    private final TProductVehicleMapper vehicleMapper;
    private final TProductStockRecordMapper stockRecordMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public DeliveryServiceImpl(TDeliveryMapper deliveryMapper,
                               TDeliveryCheckItemMapper checkItemMapper,
                               TTranMapper tranMapper,
                               TProductVehicleMapper vehicleMapper,
                               TProductStockRecordMapper stockRecordMapper,
                               CurrentUserProvider currentUserProvider,
                               OperationAuditRecorder auditRecorder) {
        this.deliveryMapper = deliveryMapper;
        this.checkItemMapper = checkItemMapper;
        this.tranMapper = tranMapper;
        this.vehicleMapper = vehicleMapper;
        this.stockRecordMapper = stockRecordMapper;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public PageInfo<TDelivery> getDeliveryPage(DeliveryQuery query) {
        DeliveryQuery actualQuery = query == null ? new DeliveryQuery() : query;
        int page = actualQuery.getPage() == null || actualQuery.getPage() < 1 ? 1 : actualQuery.getPage();
        int size = actualQuery.getSize() == null || actualQuery.getSize() < 1 ? 10 : Math.min(actualQuery.getSize(), 100);
        PageHelper.startPage(page, size);
        return new PageInfo<>(deliveryMapper.selectPage(actualQuery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TDelivery createDelivery(CreateDeliveryRequest request) {
        TTran transaction = requireAccessibleTransaction(request.getTranId());
        if (transaction.getStage() != TranStage.DELIVERY) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易未进入待交付阶段，不能创建交付记录");
        }
        TDelivery existing = deliveryMapper.selectActiveByTranId(request.getTranId());
        if (existing != null) {
            return existing;
        }

        TProductVehicle vehicle = vehicleMapper.selectByIdForUpdate(request.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "库存车辆不存在");
        }
        validateVehicleReservedForTransaction(vehicle, transaction.getId());

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        TDelivery delivery = new TDelivery();
        delivery.setTranId(transaction.getId());
        delivery.setCustomerId(transaction.getCustomerId());
        delivery.setVehicleId(vehicle.getId());
        delivery.setStatus(DeliveryStatus.PENDING_PREPARE.name());
        delivery.setPlannedDeliveryTime(request.getPlannedDeliveryTime());
        delivery.setResponsibleUserId(operatorId);
        delivery.setCreateTime(now);
        delivery.setCreateBy(operatorId);
        delivery.setUpdateTime(now);
        delivery.setUpdateBy(operatorId);

        try {
            if (deliveryMapper.insert(delivery) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "交付记录创建失败");
            }
            insertCheckItems(delivery.getId(), request.getCheckItems(), now, operatorId);
        } catch (DuplicateKeyException e) {
            TDelivery active = deliveryMapper.selectActiveByTranId(request.getTranId());
            if (active != null) {
                return active;
            }
            throw new BusinessException(CodeEnum.DUPLICATE, "交付记录已存在", e);
        }
        auditRecorder.record(AuditActionEnum.DELIVERY_CREATE, String.valueOf(delivery.getId()));
        return deliveryMapper.selectById(delivery.getId());
    }

    @Override
    public TDelivery getDelivery(Long id) {
        TDelivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交付记录不存在");
        }
        requireAccessibleTransaction(delivery.getTranId());
        return delivery;
    }

    @Override
    public List<TDeliveryCheckItem> getCheckItems(Long id) {
        TDelivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交付记录不存在");
        }
        requireAccessibleTransaction(delivery.getTranId());
        return checkItemMapper.selectByDeliveryId(id);
    }

    @Override
    public List<TDelivery> getDeliveriesByTranId(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return deliveryMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TDeliveryCheckItem updateCheckItem(Long itemId, UpdateDeliveryCheckItemRequest request) {
        TDeliveryCheckItem item = checkItemMapper.selectByIdForUpdate(itemId);
        if (item == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交付准备项不存在");
        }
        TDelivery delivery = requireDeliveryForUpdate(item.getDeliveryId());
        DeliveryStatus deliveryStatus = parseDeliveryStatus(delivery.getStatus());
        if (TERMINAL_STATUSES.contains(deliveryStatus)) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交付已终止，不能修改准备项");
        }

        DeliveryCheckStatus status = parseCheckStatus(request.getStatus());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime completedTime = status == DeliveryCheckStatus.COMPLETED ? now : null;
        int updated = checkItemMapper.updateStatus(itemId, status.name(), completedTime,
                request.getRemark(), now, operatorId);
        if (updated != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "准备项状态已变更，请刷新后重试");
        }
        if (deliveryStatus == DeliveryStatus.PENDING_PREPARE) {
            deliveryMapper.markPreparing(delivery.getId(), now, operatorId);
        }
        auditRecorder.record(AuditActionEnum.DELIVERY_CHECK, String.valueOf(delivery.getId()));
        return checkItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TDelivery signDelivery(Long id, SignDeliveryRequest request) {
        TDelivery delivery = requireDeliveryForUpdate(id);
        DeliveryStatus deliveryStatus = parseDeliveryStatus(delivery.getStatus());
        if (deliveryStatus == DeliveryStatus.COMPLETED) {
            return deliveryMapper.selectById(id);
        }
        if (deliveryStatus == DeliveryStatus.CANCELLED || deliveryStatus == DeliveryStatus.EXCEPTION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交付状态不能签收");
        }

        TTran transaction = requireAccessibleTransaction(delivery.getTranId());
        if (transaction.getStage() != TranStage.DELIVERY) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易未处于待交付阶段，不能签收");
        }
        if (checkItemMapper.countIncompleteByDeliveryId(id) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交付准备项未完成，不能签收");
        }

        TProductVehicle vehicle = vehicleMapper.selectByIdForUpdate(delivery.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "库存车辆不存在");
        }
        validateVehicleReservedForTransaction(vehicle, transaction.getId());
        TProductStockRecord reserveRecord = stockRecordMapper.selectLatestReserveByVehicle(
                vehicle.getId(), "ORDER", transaction.getId().longValue());
        if (reserveRecord == null) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "缺少原订单库存占用流水，不能出库");
        }
        if (stockRecordMapper.selectReleaseByRelatedRecordId(reserveRecord.getId()) != null) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "原订单库存占用已释放，不能出库");
        }

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        ProductVehicleStatus currentVehicleStatus = ProductVehicleStatus.parse(vehicle.getStatus());
        int vehicleRows = vehicleMapper.outboundIfCurrent(vehicle.getId(),
                currentVehicleStatus.name(), now, operatorId);
        if (vehicleRows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "库存车辆状态已变更，请刷新后重试");
        }
        insertOutboundRecord(vehicle, reserveRecord, delivery.getId(), currentVehicleStatus.name(), now, operatorId);

        int deliveryRows = deliveryMapper.signIfCurrent(id, deliveryStatus.name(), request.getSignedAt(),
                request.getSignerName(), request.getSignedAt(), request.getSignMethod(),
                request.getSignEvidence(), now, operatorId);
        if (deliveryRows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交付状态已变更，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_OUT, String.valueOf(vehicle.getId()));
        auditRecorder.record(AuditActionEnum.DELIVERY_SIGN, String.valueOf(id));
        auditRecorder.record(AuditActionEnum.DELIVERY_COMPLETE, String.valueOf(id));
        return deliveryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TDelivery markException(Long id, DeliveryExceptionRequest request) {
        TDelivery delivery = requireDeliveryForUpdate(id);
        DeliveryStatus status = parseDeliveryStatus(delivery.getStatus());
        if (status == DeliveryStatus.COMPLETED || status == DeliveryStatus.CANCELLED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态交付不能登记异常");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        if (deliveryMapper.markExceptionIfNotTerminal(id, requireNonBlank(request.getExceptionType(), "异常类型不能为空"),
                requireNonBlank(request.getReason(), "异常原因不能为空"), now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交付状态已变更，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.DELIVERY_EXCEPTION, String.valueOf(id));
        return deliveryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TDelivery cancelDelivery(Long id, String reason) {
        TDelivery delivery = requireDeliveryForUpdate(id);
        DeliveryStatus status = parseDeliveryStatus(delivery.getStatus());
        if (status == DeliveryStatus.COMPLETED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "已完成交付不能直接取消");
        }
        if (status == DeliveryStatus.CANCELLED) {
            return deliveryMapper.selectById(id);
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        if (deliveryMapper.cancelIfNotTerminal(id, requireNonBlank(reason, "取消原因不能为空"), now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交付状态已变更，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.DELIVERY_CANCEL, String.valueOf(id));
        return deliveryMapper.selectById(id);
    }

    private TDelivery requireDeliveryForUpdate(Long id) {
        TDelivery delivery = deliveryMapper.selectByIdForUpdate(id);
        if (delivery == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交付记录不存在");
        }
        requireAccessibleTransaction(delivery.getTranId());
        return delivery;
    }

    private TTran requireAccessibleTransaction(Integer tranId) {
        CurrentUserProvider.TransactionDataScope scope = currentUserProvider.getTransactionDataScope();
        TTran transaction = scope.isAll()
                ? tranMapper.selectByPrimaryKey(tranId)
                : tranMapper.selectScopedById(tranId, scope.getSelfUserId(),
                scope.isApprovalScope(), scope.getFinanceStages());
        if (transaction == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权访问该交易");
        }
        return transaction;
    }

    private void validateVehicleReservedForTransaction(TProductVehicle vehicle, Integer tranId) {
        ProductVehicleStatus status = ProductVehicleStatus.parse(vehicle.getStatus());
        if (status != ProductVehicleStatus.ORDER_RESERVED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "库存车辆不是订单占用状态");
        }
        if (!"ORDER".equals(vehicle.getSourceType())
                || vehicle.getSourceId() == null
                || !vehicle.getSourceId().equals(tranId.longValue())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "库存车辆未被当前交易占用");
        }
    }

    private void insertCheckItems(Long deliveryId, List<DeliveryCheckItemRequest> requestedItems,
                                  LocalDateTime now, Integer operatorId) {
        List<DeliveryCheckItemRequest> items = requestedItems == null || requestedItems.isEmpty()
                ? DEFAULT_CHECK_ITEMS
                : requestedItems;
        Set<String> codes = new LinkedHashSet<>();
        for (DeliveryCheckItemRequest request : items) {
            String itemCode = requireNonBlank(request.getItemCode(), "准备项编码不能为空")
                    .trim().toUpperCase(Locale.ROOT);
            if (!codes.add(itemCode)) {
                throw new BusinessException(CodeEnum.DUPLICATE, "准备项编码重复");
            }
            TDeliveryCheckItem item = new TDeliveryCheckItem();
            item.setDeliveryId(deliveryId);
            item.setItemCode(itemCode);
            item.setItemName(requireNonBlank(request.getItemName(), "准备项名称不能为空").trim());
            item.setStatus(DeliveryCheckStatus.PENDING.name());
            item.setResponsibleUserId(request.getResponsibleUserId());
            item.setCreateTime(now);
            item.setCreateBy(operatorId);
            item.setUpdateTime(now);
            item.setUpdateBy(operatorId);
            if (checkItemMapper.insert(item) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "交付准备项创建失败");
            }
        }
    }

    private void insertOutboundRecord(TProductVehicle vehicle, TProductStockRecord reserveRecord,
                                      Long deliveryId, String beforeStatus,
                                      LocalDateTime now, Integer operatorId) {
        TProductStockRecord record = new TProductStockRecord();
        record.setProductId(vehicle.getProductId());
        record.setVehicleId(vehicle.getId());
        record.setQuantity(0);
        record.setType("OUTBOUND");
        record.setSourceType("DELIVERY");
        record.setSourceId(deliveryId);
        record.setBeforeStatus(beforeStatus);
        record.setAfterStatus(ProductVehicleStatus.OUTBOUND.name());
        record.setRelatedRecordId(reserveRecord.getId());
        record.setRemark("交付签收出库");
        record.setCreateTime(now);
        record.setCreateBy(operatorId);
        if (stockRecordMapper.insert(record) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "交付出库流水创建失败");
        }
    }

    private DeliveryStatus parseDeliveryStatus(String value) {
        try {
            return DeliveryStatus.parse(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, e.getMessage(), e);
        }
    }

    private DeliveryCheckStatus parseCheckStatus(String value) {
        try {
            return DeliveryCheckStatus.parse(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, e.getMessage(), e);
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        return value;
    }

    private static List<DeliveryCheckItemRequest> defaultCheckItems() {
        List<DeliveryCheckItemRequest> items = new ArrayList<>();
        items.add(checkItem("VEHICLE_READY", "车辆验收"));
        items.add(checkItem("DOCUMENTS_READY", "资料移交"));
        items.add(checkItem("PAYMENT_READY", "收款条件"));
        items.add(checkItem("INVOICE_READY", "票据条件"));
        items.add(checkItem("CUSTOMER_APPOINTMENT", "客户预约"));
        return List.copyOf(items);
    }

    private static DeliveryCheckItemRequest checkItem(String code, String name) {
        DeliveryCheckItemRequest request = new DeliveryCheckItemRequest();
        request.setItemCode(code);
        request.setItemName(name);
        return request;
    }
}
