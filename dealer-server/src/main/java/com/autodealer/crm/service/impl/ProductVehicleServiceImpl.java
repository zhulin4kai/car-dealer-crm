package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CreateProductVehicleRequest;
import com.autodealer.crm.dto.ReleaseProductVehicleRequest;
import com.autodealer.crm.dto.ReserveProductVehicleRequest;
import com.autodealer.crm.enums.ProductVehicleStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TProductVehicleMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.query.ProductVehicleQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.ProductVehicleService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProductVehicleServiceImpl implements ProductVehicleService {

    private static final Set<String> VALID_SOURCE_TYPES = Set.of("ORDER", "TEST_DRIVE", "SALES_LOCK");

    private final TProductVehicleMapper vehicleMapper;
    private final TProductMapper productMapper;
    private final TProductStockRecordMapper stockRecordMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public ProductVehicleServiceImpl(TProductVehicleMapper vehicleMapper,
                                     TProductMapper productMapper,
                                     TProductStockRecordMapper stockRecordMapper,
                                     CurrentUserProvider currentUserProvider,
                                     OperationAuditRecorder auditRecorder) {
        this.vehicleMapper = vehicleMapper;
        this.productMapper = productMapper;
        this.stockRecordMapper = stockRecordMapper;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public PageInfo<TProductVehicle> getVehiclePage(ProductVehicleQuery query) {
        ProductVehicleQuery actualQuery = query == null ? new ProductVehicleQuery() : query;
        int page = actualQuery.getPage() == null || actualQuery.getPage() < 1 ? 1 : actualQuery.getPage();
        int size = actualQuery.getSize() == null || actualQuery.getSize() < 1 ? 10 : actualQuery.getSize();
        if (size > 100) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        PageHelper.startPage(page, size);
        List<TProductVehicle> vehicles = vehicleMapper.selectPage(actualQuery);
        return new PageInfo<>(vehicles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductVehicle inboundVehicle(CreateProductVehicleRequest request) {
        TProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商品不存在，无法入库车辆");
        }
        if (vehicleMapper.selectByVin(request.getVin()) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "VIN 已存在，不能重复入库");
        }

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        TProductVehicle vehicle = new TProductVehicle();
        vehicle.setProductId(request.getProductId());
        vehicle.setVin(request.getVin());
        vehicle.setColor(request.getColor());
        vehicle.setConfiguration(request.getConfiguration());
        vehicle.setLocation(request.getLocation());
        vehicle.setStatus(ProductVehicleStatus.AVAILABLE.name());
        vehicle.setCreateTime(now);
        vehicle.setCreateBy(operatorId);
        vehicle.setUpdateTime(now);
        vehicle.setUpdateBy(operatorId);

        try {
            if (vehicleMapper.insert(vehicle) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "库存车辆入库失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.DUPLICATE, "VIN 已存在，不能重复入库", e);
        }

        if (productMapper.updateStock(request.getProductId(), 1) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "商品库存汇总更新失败");
        }
        insertStockRecord(request.getProductId(), vehicle.getId(), 1, "INBOUND",
                "INBOUND", vehicle.getId(), null, ProductVehicleStatus.AVAILABLE.name(),
                null, request.getRemark(), now, operatorId);
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_IN, String.valueOf(vehicle.getId()));
        return vehicleMapper.selectById(vehicle.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductVehicle reserveVehicle(Long vehicleId, ReserveProductVehicleRequest request) {
        String sourceType = normalizeSourceType(request.getSourceType());
        ProductVehicleStatus targetStatus = statusForHoldType(request.getHoldType(), sourceType);
        TProductVehicle existingReservation = vehicleMapper.selectActiveBySource(sourceType, request.getSourceId());
        if (existingReservation != null) {
            return existingReservation;
        }

        TProductVehicle vehicle = vehicleMapper.selectByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "库存车辆不存在");
        }
        if (ProductVehicleStatus.parse(vehicle.getStatus()) != ProductVehicleStatus.AVAILABLE) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前车辆不可占用");
        }

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int updated = vehicleMapper.reserveIfAvailable(vehicleId, targetStatus.name(), request.getHoldType(),
                sourceType, request.getSourceId(), request.getHoldUntil(), now, operatorId);
        if (updated != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "库存车辆已被占用，请刷新后重试");
        }
        if (productMapper.updateStock(vehicle.getProductId(), -1) != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "商品可售库存不足，无法占用");
        }

        insertStockRecord(vehicle.getProductId(), vehicleId, -1, "RESERVE",
                sourceType, request.getSourceId(), vehicle.getStatus(), targetStatus.name(),
                null, request.getRemark(), now, operatorId);
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_RESERVE, String.valueOf(vehicleId));
        return vehicleMapper.selectById(vehicleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductVehicle releaseVehicle(Long vehicleId, ReleaseProductVehicleRequest request) {
        TProductStockRecord reserveRecord = stockRecordMapper.selectById(request.getReserveRecordId());
        if (reserveRecord == null || !"RESERVE".equals(reserveRecord.getType())
                || !vehicleId.equals(reserveRecord.getVehicleId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "原占用流水无效");
        }
        if (stockRecordMapper.selectReleaseByRelatedRecordId(request.getReserveRecordId()) != null) {
            return vehicleMapper.selectById(vehicleId);
        }

        TProductVehicle vehicle = vehicleMapper.selectByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "库存车辆不存在");
        }
        ProductVehicleStatus currentStatus = ProductVehicleStatus.parse(vehicle.getStatus());
        if (!currentStatus.isOccupied()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前车辆状态不允许释放");
        }

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        if (vehicleMapper.releaseIfCurrent(vehicleId, currentStatus.name(), now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "库存车辆状态已变更，请刷新后重试");
        }
        if (productMapper.updateStock(vehicle.getProductId(), 1) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "商品库存汇总恢复失败");
        }

        insertStockRecord(vehicle.getProductId(), vehicleId, 1, "RELEASE",
                reserveRecord.getSourceType(), reserveRecord.getSourceId(), currentStatus.name(),
                ProductVehicleStatus.AVAILABLE.name(), request.getReserveRecordId(),
                request.getReason(), now, operatorId);
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_RELEASE, String.valueOf(vehicleId));
        return vehicleMapper.selectById(vehicleId);
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = sourceType == null ? "" : sourceType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_SOURCE_TYPES.contains(normalized)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "不支持的库存占用来源");
        }
        return normalized;
    }

    private ProductVehicleStatus statusForHoldType(String holdType, String sourceType) {
        String normalizedHoldType = holdType == null ? "" : holdType.trim().toUpperCase(Locale.ROOT);
        if ("TEST_DRIVE".equals(sourceType) || "TEST_DRIVE".equals(normalizedHoldType)) {
            return ProductVehicleStatus.TEST_DRIVE_RESERVED;
        }
        if ("ORDER".equals(sourceType) || "ORDER".equals(normalizedHoldType)) {
            return ProductVehicleStatus.ORDER_RESERVED;
        }
        if ("SALES_LOCK".equals(sourceType) || "SALES_LOCK".equals(normalizedHoldType)) {
            return ProductVehicleStatus.SALES_LOCKED;
        }
        throw new BusinessException(CodeEnum.PARAM_ERROR, "不支持的库存占用类型");
    }

    private void insertStockRecord(Long productId, Long vehicleId, Integer quantity, String type,
                                   String sourceType, Long sourceId, String beforeStatus, String afterStatus,
                                   Long relatedRecordId, String remark, LocalDateTime now, Integer operatorId) {
        TProductStockRecord record = new TProductStockRecord();
        record.setProductId(productId);
        record.setVehicleId(vehicleId);
        record.setQuantity(quantity);
        record.setType(type);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setBeforeStatus(beforeStatus);
        record.setAfterStatus(afterStatus);
        record.setRelatedRecordId(relatedRecordId);
        record.setRemark(remark);
        record.setCreateTime(now);
        record.setCreateBy(operatorId);
        if (stockRecordMapper.insert(record) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "库存流水创建失败");
        }
    }
}
