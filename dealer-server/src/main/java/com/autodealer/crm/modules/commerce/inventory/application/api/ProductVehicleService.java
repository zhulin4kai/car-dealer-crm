package com.autodealer.crm.modules.commerce.inventory.application.api;

import com.autodealer.crm.modules.commerce.inventory.application.api.dto.CreateProductVehicleRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.dto.ReleaseProductVehicleRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.dto.ReserveProductVehicleRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductVehicle;
import com.autodealer.crm.modules.commerce.inventory.application.api.query.ProductVehicleQuery;
import com.github.pagehelper.PageInfo;

public interface ProductVehicleService {
    PageInfo<TProductVehicle> getVehiclePage(ProductVehicleQuery query);

    TProductVehicle inboundVehicle(CreateProductVehicleRequest request);

    TProductVehicle reserveVehicle(Long vehicleId, ReserveProductVehicleRequest request);

    TProductVehicle releaseVehicle(Long vehicleId, ReleaseProductVehicleRequest request);
}
