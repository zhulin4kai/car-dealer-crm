package com.autodealer.crm.aspect;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.BaseQuery;
import com.autodealer.crm.util.JWTUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataScopeAspectTest {

    @InjectMocks
    private DataScopeAspect dataScopeAspect;

    @Test
    void testAdminUserShouldNotSetFilterSQL() throws Throwable {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class);
             MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            MethodSignature methodSignature = mock(MethodSignature.class);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(Constants.TOKEN_NAME, "valid.token");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);

            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            TUser adminUser = new TUser();
            adminUser.setId(1);
            adminUser.setRoleList(Arrays.asList("admin"));

            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.token")).thenReturn(adminUser);

            Method method = TestService.class.getMethod("testMethod", BaseQuery.class);
            DataScope dataScope = method.getAnnotation(DataScope.class);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.proceed()).thenReturn(null);

            BaseQuery query = new BaseQuery();
            Object[] args = {query};
            when(joinPoint.getArgs()).thenReturn(args);

            dataScopeAspect.process(joinPoint);

            assertNull(query.getFilterSQL());
        }
    }

    @Test
    void testNormalUserShouldSetFilterSQL() throws Throwable {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class);
             MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            MethodSignature methodSignature = mock(MethodSignature.class);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(Constants.TOKEN_NAME, "valid.token");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);

            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            TUser normalUser = new TUser();
            normalUser.setId(2);
            normalUser.setRoleList(Collections.singletonList("user"));

            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.token")).thenReturn(normalUser);

            Method method = TestService.class.getMethod("testMethod", BaseQuery.class);
            DataScope dataScope = method.getAnnotation(DataScope.class);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.proceed()).thenReturn(null);

            BaseQuery query = new BaseQuery();
            Object[] args = {query};
            when(joinPoint.getArgs()).thenReturn(args);

            dataScopeAspect.process(joinPoint);

            assertNotNull(query.getFilterSQL());
            assertTrue(query.getFilterSQL().contains("2"));
        }
    }

    @Test
    void testNonBaseQueryParamShouldNotSetFilterSQL() throws Throwable {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class);
             MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            MethodSignature methodSignature = mock(MethodSignature.class);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(Constants.TOKEN_NAME, "valid.token");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);

            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            TUser normalUser = new TUser();
            normalUser.setId(2);
            normalUser.setRoleList(Collections.singletonList("user"));

            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.token")).thenReturn(normalUser);

            Method method = TestService.class.getMethod("testMethod", BaseQuery.class);
            DataScope dataScope = method.getAnnotation(DataScope.class);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.proceed()).thenReturn(null);

            Object[] args = {"not a BaseQuery"};
            when(joinPoint.getArgs()).thenReturn(args);

            dataScopeAspect.process(joinPoint);

            verify(joinPoint).proceed();
        }
    }

    static class TestService {
        @DataScope(tableAlias = "ta", tableField = "owner_id")
        public void testMethod(BaseQuery query) {
        }
    }
}
