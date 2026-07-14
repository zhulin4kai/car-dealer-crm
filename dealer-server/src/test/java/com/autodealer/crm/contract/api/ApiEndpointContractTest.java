package com.autodealer.crm.contract.api;

import com.autodealer.crm.modules.fulfillment.transaction.web.TranController;
import com.autodealer.crm.modules.sales.activity.web.ActivityController;
import com.autodealer.crm.modules.sales.customer.web.CustomerController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiEndpointContractTest {

    @Test
    void activityList_shouldExposeOnlyCurrentEndpoint() {
        assertTrue(mappedPaths(ActivityController.class, GetMapping.class).contains("/api/activities"));
        assertFalse(mappedPaths(ActivityController.class, GetMapping.class).contains("/api/activitys"));
    }

    @Test
    void customerList_shouldExposeOnlyCurrentEndpoint() {
        assertTrue(mappedPaths(CustomerController.class, GetMapping.class).contains("/api/customers"));
        assertFalse(mappedPaths(CustomerController.class, GetMapping.class).contains("/api/customer/list"));
    }

    @Test
    void transactionCreate_shouldExposeOnlyCurrentEndpoint() {
        assertTrue(mappedPaths(TranController.class, PostMapping.class).contains("/api/transactions"));
        assertFalse(mappedPaths(TranController.class, PostMapping.class).contains("/api/tran/create"));
    }

    @Test
    void transactionLegacyMutationEndpoints_shouldNotBeExposed() {
        assertFalse(mappedPaths(TranController.class, DeleteMapping.class).contains("/api/tran/{id}"));
        assertFalse(mappedPaths(TranController.class, PostMapping.class).contains("/api/tran/batch-delete"));
        assertFalse(mappedPaths(TranController.class, PostMapping.class).contains("/api/tran/payment/{id}/refund"));
    }

    private Set<String> mappedPaths(Class<?> controllerClass, Class<? extends Annotation> mappingAnnotation) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotation))
                .flatMap(method -> pathsFor(controllerClass, method, mappingAnnotation).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Method methodMappedTo(Class<?> controllerClass,
                                  Class<? extends Annotation> mappingAnnotation,
                                  String expectedPath) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotation))
                .filter(method -> pathsFor(controllerClass, method, mappingAnnotation).contains(expectedPath))
                .findFirst()
                .orElseThrow();
    }

    private Set<String> pathsFor(Class<?> controllerClass,
                                 Method method,
                                 Class<? extends Annotation> mappingAnnotation) {
        Set<String> classPaths = classPaths(controllerClass);
        Set<String> methodPaths = methodPaths(method, mappingAnnotation);
        Set<String> paths = new LinkedHashSet<>();
        for (String classPath : classPaths) {
            for (String methodPath : methodPaths) {
                paths.add(normalizePath(classPath, methodPath));
            }
        }
        return paths;
    }

    private Set<String> classPaths(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Set.of("");
        }
        String[] paths = requestMapping.value().length > 0 ? requestMapping.value() : requestMapping.path();
        return paths.length == 0 ? Set.of("") : Set.of(paths);
    }

    private Set<String> methodPaths(Method method, Class<? extends Annotation> mappingAnnotation) {
        Annotation annotation = method.getAnnotation(mappingAnnotation);
        if (annotation instanceof GetMapping getMapping) {
            String[] paths = getMapping.value().length > 0 ? getMapping.value() : getMapping.path();
            return paths.length == 0 ? Set.of("") : Set.of(paths);
        }
        if (annotation instanceof PostMapping postMapping) {
            String[] paths = postMapping.value().length > 0 ? postMapping.value() : postMapping.path();
            return paths.length == 0 ? Set.of("") : Set.of(paths);
        }
        if (annotation instanceof DeleteMapping deleteMapping) {
            String[] paths = deleteMapping.value().length > 0 ? deleteMapping.value() : deleteMapping.path();
            return paths.length == 0 ? Set.of("") : Set.of(paths);
        }
        return Set.of("");
    }

    private String normalizePath(String classPath, String methodPath) {
        String joined = (classPath == null ? "" : classPath) + "/" + (methodPath == null ? "" : methodPath);
        return joined.replaceAll("/+", "/").replaceAll("/$", "");
    }
}
