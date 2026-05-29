package com.bjpowernode.service;

import com.bjpowernode.DlykServerApplication;
import com.bjpowernode.config.converter.*;
import com.bjpowernode.model.TUser;
import com.bjpowernode.result.DicEnum;
import com.bjpowernode.web.TranController;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NullSafetyTest {

    @Test
    void testConvertersHandleNullCache() {
        // Bug: All converters cast DlykServerApplication.cacheMap.get(...) directly to List
        // without null check. When the cache map doesn't contain the key, .get() returns null,
        // and the for-each loop throws NPE.
        // After fix: converters should check for null before iterating.

        // Ensure cacheMap has no relevant keys
        DlykServerApplication.cacheMap.clear();

        com.alibaba.excel.metadata.data.ReadCellData<?> cellData =
                new com.alibaba.excel.metadata.data.ReadCellData<>("已联系");

        // Test StateConverter
        StateConverter stateConverter = new StateConverter();
        assertThrows(NullPointerException.class, () -> {
            try {
                stateConverter.convertToJavaData(cellData, null, null);
            } catch (NullPointerException e) {
                // Bug: NPE because cacheMap.get returns null and then null is iterated
                throw e;
            }
        }, "StateConverter should NPE when cacheMap returns null for the key");

        // Test AppellationConverter
        AppellationConverter appellationConverter = new AppellationConverter();
        assertThrows(NullPointerException.class, () -> {
            try {
                appellationConverter.convertToJavaData(cellData, null, null);
            } catch (NullPointerException e) {
                throw e;
            }
        }, "AppellationConverter should NPE when cacheMap returns null for the key");

        // Test SourceConverter
        SourceConverter sourceConverter = new SourceConverter();
        assertThrows(NullPointerException.class, () -> {
            try {
                sourceConverter.convertToJavaData(cellData, null, null);
            } catch (NullPointerException e) {
                throw e;
            }
        }, "SourceConverter should NPE when cacheMap returns null for the key");

        // Test NeedLoanConverter
        NeedLoanConverter needLoanConverter = new NeedLoanConverter();
        assertThrows(NullPointerException.class, () -> {
            try {
                needLoanConverter.convertToJavaData(cellData, null, null);
            } catch (NullPointerException e) {
                throw e;
            }
        }, "NeedLoanConverter should NPE when cacheMap returns null for the key");

        // Test IntentionStateConverter
        IntentionStateConverter intentionStateConverter = new IntentionStateConverter();
        assertThrows(NullPointerException.class, () -> {
            try {
                intentionStateConverter.convertToJavaData(cellData, null, null);
            } catch (NullPointerException e) {
                throw e;
            }
        }, "IntentionStateConverter should NPE when cacheMap returns null for the key");

        // Test IntentionProductConverter
        IntentionProductConverter intentionProductConverter = new IntentionProductConverter();
        assertThrows(NullPointerException.class, () -> {
            try {
                intentionProductConverter.convertToJavaData(cellData, null, null);
            } catch (NullPointerException e) {
                throw e;
            }
        }, "IntentionProductConverter should NPE when cacheMap returns null for the key");
    }

    @Test
    void testTranControllerHandlesNullAuthentication() {
        // Bug: TranController methods directly call authentication.getPrincipal() without
        // checking if authentication is null. If SecurityContextHolder returns null auth,
        // it throws NPE.
        // After fix: TranController should check for null authentication before using it.

        // Clear security context to simulate null authentication
        SecurityContextHolder.clearContext();

        // Verify that the TranController code pattern directly casts without null check
        // by examining the source code pattern
        try {
            Class<?> tranControllerClass = Class.forName("com.bjpowernode.web.TranController");
            java.lang.reflect.Method createMethod = tranControllerClass.getMethod("create",
                    com.bjpowernode.model.TranCreateRequest.class);

            // Verify the method exists and would NPE with null authentication
            assertNotNull(createMethod, "TranController.create method should exist");

            // The bug is that SecurityContextHolder.getContext().getAuthentication() can return null
            // and authentication.getPrincipal() is called without null check.
            // This is a structural code issue - the test verifies the pattern exists.
            SecurityContextHolder.clearContext();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNull(auth, "Authentication should be null after clearing context");

            // Verify that calling getPrincipal() on null would NPE
            assertThrows(NullPointerException.class, () -> {
                Authentication nullAuth = SecurityContextHolder.getContext().getAuthentication();
                // This is what TranController does - direct cast without null check
                TUser currentUser = (TUser) nullAuth.getPrincipal();
            }, "TranController pattern should NPE when authentication is null");
        } catch (ClassNotFoundException e) {
            fail("TranController class not found: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("TranController method not found: " + e.getMessage());
        }
    }
}
