package com.autodealer.crm.service;

import com.autodealer.crm.dto.CreateProductVehicleRequest;
import com.autodealer.crm.dto.ReleaseProductVehicleRequest;
import com.autodealer.crm.dto.ReserveProductVehicleRequest;
import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.query.ProductVehicleQuery;
import com.github.pagehelper.PageInfo;

public interface ProductVehicleService {
    PageInfo<TProductVehicle> getVehiclePage(ProductVehicleQuery query);

    TProductVehicle inboundVehicle(CreateProductVehicleRequest request);

    TProductVehicle reserveVehicle(Long vehicleId, ReserveProductVehicleRequest request);

    TProductVehicle releaseVehicle(Long vehicleId, ReleaseProductVehicleRequest request);
}
