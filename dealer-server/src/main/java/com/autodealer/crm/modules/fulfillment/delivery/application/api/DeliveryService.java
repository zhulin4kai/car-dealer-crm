package com.autodealer.crm.modules.fulfillment.delivery.application.api;

import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.CreateDeliveryRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.DeliveryExceptionRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.SignDeliveryRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.UpdateDeliveryCheckItemRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDelivery;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDeliveryCheckItem;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.query.DeliveryQuery;
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
