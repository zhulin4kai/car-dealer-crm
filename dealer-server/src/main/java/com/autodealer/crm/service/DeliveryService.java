package com.autodealer.crm.service;

import com.autodealer.crm.dto.CreateDeliveryRequest;
import com.autodealer.crm.dto.DeliveryExceptionRequest;
import com.autodealer.crm.dto.SignDeliveryRequest;
import com.autodealer.crm.dto.UpdateDeliveryCheckItemRequest;
import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.model.TDeliveryCheckItem;
import com.autodealer.crm.query.DeliveryQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface DeliveryService {
    PageInfo<TDelivery> getDeliveryPage(DeliveryQuery query);

    TDelivery createDelivery(CreateDeliveryRequest request);

    TDelivery getDelivery(Long id);

    List<TDeliveryCheckItem> getCheckItems(Long id);

    List<TDelivery> getDeliveriesByTranId(Integer tranId);

    TDeliveryCheckItem updateCheckItem(Long itemId, UpdateDeliveryCheckItemRequest request);

    TDelivery signDelivery(Long id, SignDeliveryRequest request);

    TDelivery markException(Long id, DeliveryExceptionRequest request);

    TDelivery cancelDelivery(Long id, String reason);
}
