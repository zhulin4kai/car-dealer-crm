package com.autodealer.crm.aspect;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.query.BaseQuery;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataScopeAspectTest {

    @InjectMocks
    private DataScopeAspect aspect;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private ProceedingJoinPoint joinPoint;

    @Test
    void normalUserShouldInjectScopeIntoQueryAtAnyArgumentPosition() throws Throwable {
        BaseQuery query = new BaseQuery();
        when(joinPoint.getArgs()).thenReturn(new Object[]{"prefix", query});
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(joinPoint.proceed()).thenReturn("result");

        assertEquals("result", aspect.process(joinPoint));
        assertEquals(7, query.getDataScopeUserId());
    }

    @Test
    void adminShouldExplicitlyClearAnyClientSuppliedScope() throws Throwable {
        BaseQuery query = new BaseQuery();
        query.setDataScopeUserId(999);
        when(joinPoint.getArgs()).thenReturn(new Object[]{query});
        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);

        aspect.process(joinPoint);

        assertNull(query.getDataScopeUserId());
        verify(joinPoint).proceed();
    }

    @Test
    void missingBaseQueryShouldFailClosed() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"not-a-query"});

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> aspect.process(joinPoint));

        assertTrue(error.getMessage().contains("BaseQuery"));
        verify(joinPoint, never()).proceed();
    }
}
