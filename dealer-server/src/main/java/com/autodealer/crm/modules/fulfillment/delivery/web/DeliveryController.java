package com.autodealer.crm.modules.fulfillment.delivery.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.CreateDeliveryRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.DeliveryCancelRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.DeliveryExceptionRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.SignDeliveryRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.dto.UpdateDeliveryCheckItemRequest;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDelivery;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDeliveryCheckItem;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.query.DeliveryQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.DeliveryService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_LIST + "')")
    public Result<PageInfo<TDelivery>> list(DeliveryQuery query) {
        return Result.OK(deliveryService.getDeliveryPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_CREATE + "')")
    public Result<TDelivery> create(@Valid @RequestBody CreateDeliveryRequest request) {
        return Result.OK(deliveryService.createDelivery(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_VIEW + "')")
    public Result<TDelivery> detail(@PathVariable Long id) {
        return Result.OK(deliveryService.getDelivery(id));
    }

    @GetMapping("/{id}/check-items")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_VIEW + "')")
    public Result<List<TDeliveryCheckItem>> checkItems(@PathVariable Long id) {
        return Result.OK(deliveryService.getCheckItems(id));
    }

    @GetMapping("/tran/{tranId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_VIEW + "')")
    public Result<List<TDelivery>> byTran(@PathVariable Integer tranId) {
        return Result.OK(deliveryService.getDeliveriesByTranId(tranId));
    }

    @PutMapping("/check-items/{itemId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_CHECK + "')")
    public Result<TDeliveryCheckItem> updateCheckItem(@PathVariable Long itemId,
                                                 @Valid @RequestBody UpdateDeliveryCheckItemRequest request) {
        return Result.OK(deliveryService.updateCheckItem(itemId, request));
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_SIGN + "')")
    public Result<TDelivery> sign(@PathVariable Long id, @Valid @RequestBody SignDeliveryRequest request) {
        return Result.OK(deliveryService.signDelivery(id, request));
    }

    @PostMapping("/{id}/exception")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_EXCEPTION + "')")
    public Result<TDelivery> exception(@PathVariable Long id,
                                  @Valid @RequestBody DeliveryExceptionRequest request) {
        return Result.OK(deliveryService.markException(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_CANCEL + "')")
    public Result<TDelivery> cancel(@PathVariable Long id, @Valid @RequestBody DeliveryCancelRequest request) {
        return Result.OK(deliveryService.cancelDelivery(id, request.getReason()));
    }
}
