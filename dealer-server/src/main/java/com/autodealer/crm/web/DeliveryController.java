package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.CreateDeliveryRequest;
import com.autodealer.crm.dto.DeliveryCancelRequest;
import com.autodealer.crm.dto.DeliveryExceptionRequest;
import com.autodealer.crm.dto.SignDeliveryRequest;
import com.autodealer.crm.dto.UpdateDeliveryCheckItemRequest;
import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.model.TDeliveryCheckItem;
import com.autodealer.crm.query.DeliveryQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.DeliveryService;
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
    public R<PageInfo<TDelivery>> list(DeliveryQuery query) {
        return R.OK(deliveryService.getDeliveryPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_CREATE + "')")
    public R<TDelivery> create(@Valid @RequestBody CreateDeliveryRequest request) {
        return R.OK(deliveryService.createDelivery(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_VIEW + "')")
    public R<TDelivery> detail(@PathVariable Long id) {
        return R.OK(deliveryService.getDelivery(id));
    }

    @GetMapping("/{id}/check-items")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_VIEW + "')")
    public R<List<TDeliveryCheckItem>> checkItems(@PathVariable Long id) {
        return R.OK(deliveryService.getCheckItems(id));
    }

    @GetMapping("/tran/{tranId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_VIEW + "')")
    public R<List<TDelivery>> byTran(@PathVariable Integer tranId) {
        return R.OK(deliveryService.getDeliveriesByTranId(tranId));
    }

    @PutMapping("/check-items/{itemId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_CHECK + "')")
    public R<TDeliveryCheckItem> updateCheckItem(@PathVariable Long itemId,
                                                 @Valid @RequestBody UpdateDeliveryCheckItemRequest request) {
        return R.OK(deliveryService.updateCheckItem(itemId, request));
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_SIGN + "')")
    public R<TDelivery> sign(@PathVariable Long id, @Valid @RequestBody SignDeliveryRequest request) {
        return R.OK(deliveryService.signDelivery(id, request));
    }

    @PostMapping("/{id}/exception")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_EXCEPTION + "')")
    public R<TDelivery> exception(@PathVariable Long id,
                                  @Valid @RequestBody DeliveryExceptionRequest request) {
        return R.OK(deliveryService.markException(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DELIVERY_CANCEL + "')")
    public R<TDelivery> cancel(@PathVariable Long id, @Valid @RequestBody DeliveryCancelRequest request) {
        return R.OK(deliveryService.cancelDelivery(id, request.getReason()));
    }
}
