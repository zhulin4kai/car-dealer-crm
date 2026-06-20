package com.autodealer.crm.aspect;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.query.BaseQuery;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class DataScopeAspect {

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Pointcut("@annotation(com.autodealer.crm.commons.DataScope)")
    private void pointCut() {
    }

    @Around("pointCut()")
    public Object process(ProceedingJoinPoint joinPoint) throws Throwable {
        BaseQuery query = Arrays.stream(joinPoint.getArgs())
                .filter(BaseQuery.class::isInstance)
                .map(BaseQuery.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("@DataScope 方法必须包含 BaseQuery 参数"));

        query.setDataScopeUserId(currentUserProvider.getDataScopeUserId());
        return joinPoint.proceed();
    }
}
