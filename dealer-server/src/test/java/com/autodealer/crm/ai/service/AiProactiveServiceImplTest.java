package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.CreateAiProactiveSubscriptionRequest;
import com.autodealer.crm.ai.enums.AiProactiveEventStatus;
import com.autodealer.crm.ai.enums.AiProactiveSubscriptionStatus;
import com.autodealer.crm.ai.mapper.TAiProactiveEventMapper;
import com.autodealer.crm.ai.mapper.TAiProactiveSubscriptionMapper;
import com.autodealer.crm.ai.model.TAiProactiveEvent;
import com.autodealer.crm.ai.model.TAiProactiveSubscription;
import com.autodealer.crm.ai.service.impl.AiProactiveServiceImpl;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.FollowTaskService;
import com.autodealer.crm.service.ProductService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProactiveServiceImplTest {
    @Mock private TAiProactiveSubscriptionMapper subscriptionMapper;
    @Mock private TAiProactiveEventMapper eventMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private ProductService productService;
    @Mock private FollowTaskService followTaskService;

    private AiProactiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiProactiveServiceImpl(
                subscriptionMapper,
                eventMapper,
                currentUserProvider,
                new AiSensitiveDataSanitizer(),
                productService,
                followTaskService);
    }

    @Test
    void createSubscription_shouldPersistOwnerLimitsAndActiveStatus() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(subscriptionMapper.insert(any(TAiProactiveSubscription.class))).thenReturn(1);
        ArgumentCaptor<TAiProactiveSubscription> captor =
                ArgumentCaptor.forClass(TAiProactiveSubscription.class);
        CreateAiProactiveSubscriptionRequest request = new CreateAiProactiveSubscriptionRequest();
        request.setSubscriptionType("INVENTORY_ALERT");
        request.setFrequency("DAILY");
        request.setDailyLimit(3);
        request.setMaxResults(5);

        service.createSubscription(request);

        verify(subscriptionMapper).insert(captor.capture());
        assertEquals(7, captor.getValue().getUserId());
        assertEquals("INVENTORY_ALERT", captor.getValue().getSubscriptionType());
        assertEquals(AiProactiveSubscriptionStatus.ACTIVE.name(), captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getDailyLimit());
        assertEquals(5, captor.getValue().getMaxResults());
    }

    @Test
    void generateDueEvents_shouldUseProductServiceAndApplyLimits() {
        TUser user = userWithPermissions(PermissionCodes.PRODUCT_STOCK_VIEW);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.PRODUCT_STOCK_VIEW)).thenReturn(true);
        TAiProactiveSubscription subscription = subscription("AIPS1", "INVENTORY_ALERT");
        when(subscriptionMapper.selectActiveDueByUserId(eq(7), any(LocalDateTime.class), eq(20)))
                .thenReturn(List.of(subscription));
        when(eventMapper.countBySubscriptionAfter(eq(20L), any(LocalDateTime.class))).thenReturn(0);
        when(eventMapper.selectDuplicateAfter(eq(20L), eq("INVENTORY_ALERT"), eq("AIPS1"), any(LocalDateTime.class)))
                .thenReturn(null);
        TProduct product = new TProduct();
        product.setId(1L);
        product.setSku("SKU-1");
        product.setName("库存预警车");
        when(productService.getStockAlerts(1, 10)).thenReturn(new PageInfo<>(List.of(product)));
        when(eventMapper.insert(any(TAiProactiveEvent.class))).thenReturn(1);
        when(subscriptionMapper.selectById(20L)).thenReturn(subscription);
        ArgumentCaptor<TAiProactiveEvent> eventCaptor = ArgumentCaptor.forClass(TAiProactiveEvent.class);

        service.generateDueEvents();

        verify(productService).getStockAlerts(1, 10);
        verify(eventMapper).insert(eventCaptor.capture());
        assertEquals(AiProactiveEventStatus.READY.name(), eventCaptor.getValue().getStatus());
        assertEquals("PRODUCT_STOCK_ALERT", eventCaptor.getValue().getObjectType());
        verify(subscriptionMapper).updateTriggerTime(eq(20L), any(), any(), eq(7));
    }

    @Test
    void generateDueEvents_shouldSkipWhenPermissionChanged() {
        TUser user = userWithPermissions();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        TAiProactiveSubscription subscription = subscription("AIPS1", "INVENTORY_ALERT");
        when(subscriptionMapper.selectActiveDueByUserId(eq(7), any(LocalDateTime.class), eq(20)))
                .thenReturn(List.of(subscription));
        when(eventMapper.insert(any(TAiProactiveEvent.class))).thenReturn(1);
        when(subscriptionMapper.selectById(20L)).thenReturn(subscription);
        ArgumentCaptor<TAiProactiveEvent> eventCaptor = ArgumentCaptor.forClass(TAiProactiveEvent.class);

        service.generateDueEvents();

        verify(productService, never()).getStockAlerts(
                org.mockito.ArgumentMatchers.<Integer>any(),
                org.mockito.ArgumentMatchers.<Integer>any());
        verify(eventMapper).insert(eventCaptor.capture());
        assertEquals(AiProactiveEventStatus.SKIPPED.name(), eventCaptor.getValue().getStatus());
        assertEquals("AI_PROACTIVE_FORBIDDEN", eventCaptor.getValue().getErrorCode());
    }

    private TAiProactiveSubscription subscription(String subscriptionNo, String type) {
        TAiProactiveSubscription subscription = new TAiProactiveSubscription();
        subscription.setId(20L);
        subscription.setSubscriptionNo(subscriptionNo);
        subscription.setUserId(7);
        subscription.setSubscriptionType(type);
        subscription.setStatus(AiProactiveSubscriptionStatus.ACTIVE.name());
        subscription.setFrequency("DAILY");
        subscription.setDailyLimit(5);
        subscription.setMaxResults(10);
        subscription.setDuplicateWindowMinutes(60);
        return subscription;
    }

    private TUser userWithPermissions(String... permissions) {
        TUser user = new TUser();
        user.setId(7);
        user.setAccountEnabled(1);
        user.setPermissionList(List.of(permissions));
        return user;
    }
}
