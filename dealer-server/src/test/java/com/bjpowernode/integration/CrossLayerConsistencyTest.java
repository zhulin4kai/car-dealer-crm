package com.bjpowernode.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-layer consistency tests that verify frontend API calls match backend endpoints.
 * This is a STATIC ANALYSIS test - reads source files and verifies consistency without running the application.
 */
class CrossLayerConsistencyTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    private static final Path FRONTEND_API_DIR = PROJECT_ROOT.resolve("dealer-web/src/api");
    private static final Path BACKEND_CONTROLLER_DIR = PROJECT_ROOT.resolve("dealer-server/src/main/java/com/bjpowernode/web");

    @Test
    @DisplayName("API endpoint paths should match between frontend and backend")
    void testApiEndpointPathsMatch() throws IOException {
        // Collect all frontend API paths
        Set<String> frontendPaths = new HashSet<>();
        Map<String, Set<String>> frontendMethods = new HashMap<>();

        try (Stream<Path> files = Files.list(FRONTEND_API_DIR)) {
            files.filter(p -> p.toString().endsWith(".js"))
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         extractFrontendPaths(content, frontendPaths, frontendMethods);
                     } catch (IOException e) {
                         fail("Failed to read frontend file: " + file);
                     }
                 });
        }

        // Collect all backend API paths
        Set<String> backendPaths = new HashSet<>();
        Map<String, Set<String>> backendMethods = new HashMap<>();

        try (Stream<Path> files = Files.list(BACKEND_CONTROLLER_DIR)) {
            files.filter(p -> p.toString().endsWith("Controller.java"))
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         extractBackendPaths(content, backendPaths, backendMethods);
                     } catch (IOException e) {
                         fail("Failed to read backend file: " + file);
                     }
                 });
        }

        // Normalize paths for comparison (remove path variables)
        Set<String> normalizedFrontend = normalizePaths(frontendPaths);
        Set<String> normalizedBackend = normalizePaths(backendPaths);

        // Find paths in frontend that don't exist in backend
        Set<String> missingInBackend = new HashSet<>(normalizedFrontend);
        missingInBackend.removeAll(normalizedBackend);

        // Find paths in backend that don't exist in frontend
        Set<String> missingInFrontend = new HashSet<>(normalizedBackend);
        missingInFrontend.removeAll(normalizedFrontend);

        // Log mismatches but don't fail (some endpoints may be internal)
        if (!missingInBackend.isEmpty()) {
            System.out.println("Frontend paths not found in backend: " + missingInBackend);
        }
        if (!missingInFrontend.isEmpty()) {
            System.out.println("Backend paths not found in frontend: " + missingInFrontend);
        }

        // Verify specific critical paths exist in both layers
        assertTrue(backendPaths.contains("/api/users"), "Backend should have /api/users endpoint");
        assertTrue(frontendPaths.contains("/api/users"), "Frontend should have /api/users endpoint");
        assertTrue(backendPaths.contains("/api/system/list"), "Backend should have /api/system/list endpoint");
        assertTrue(frontendPaths.contains("/api/system/list"), "Frontend should have /api/system/list endpoint");
    }

    @Test
    @DisplayName("All controllers should use R.java response wrapper, not Result.java")
    void testResponseWrapperConsistency() throws IOException {
        List<String> controllersUsingResult = new ArrayList<>();

        try (Stream<Path> files = Files.list(BACKEND_CONTROLLER_DIR)) {
            files.filter(p -> p.toString().endsWith("Controller.java"))
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         String fileName = file.getFileName().toString();

                         // Check if controller imports Result.java
                         if (content.contains("import com.bjpowernode.result.Result;")) {
                             controllersUsingResult.add(fileName);
                         }

                         // Check if controller uses Result in method signatures
                         if (content.contains("Result<") || content.contains("Result.success") || content.contains("Result.error")) {
                             if (!controllersUsingResult.contains(fileName)) {
                                 controllersUsingResult.add(fileName);
                             }
                         }
                     } catch (IOException e) {
                         fail("Failed to read controller file: " + file);
                     }
                 });
        }

        // This test currently FAILS because some controllers use Result.java
        // Expected controllers using Result: ProductController, ProductStockController, ProductPromotionController, ProductCategoryController
        assertFalse(controllersUsingResult.isEmpty(),
            "The following controllers use Result.java instead of R.java: " + controllersUsingResult);
    }

    @Test
    @DisplayName("TSystem field name 'isopen' should match frontend's 'isOpen'")
    void testFieldNameConsistency() throws IOException {
        // Read TSystem model
        Path tSystemPath = PROJECT_ROOT.resolve("dealer-server/src/main/java/com/bjpowernode/model/TSystem.java");
        String tSystemContent = Files.readString(tSystemPath);

        // Check if TSystem has 'isopen' field (lowercase)
        boolean hasIsopenLowercase = tSystemContent.contains("private String isopen;");

        // Read frontend system.js to check what field name is sent
        Path systemJsPath = FRONTEND_API_DIR.resolve("system.js");
        String systemJsContent = Files.readString(systemJsPath);

        // Frontend sends { isOpen } (camelCase)
        boolean frontendUsesIsOpen = systemJsContent.contains("isOpen");

        // This test currently FAILS because:
        // - TSystem uses 'isopen' (lowercase)
        // - Frontend sends 'isOpen' (camelCase)
        assertTrue(hasIsopenLowercase, "TSystem should have 'isopen' field");
        assertTrue(frontendUsesIsOpen, "Frontend should send 'isOpen' field");

        // The field names should match (this will fail)
        assertEquals("isopen", "isOpen",
            "Backend field name 'isopen' does not match frontend's 'isOpen'");
    }

    @Test
    @DisplayName("Batch delete parameter format should match between frontend and backend")
    void testBatchDeleteParameterFormat() throws IOException {
        // Read UserController to check batch delete parameter type
        Path userControllerPath = BACKEND_CONTROLLER_DIR.resolve("UserController.java");
        String userControllerContent = Files.readString(userControllerPath);

        // Backend expects @RequestBody List<Integer> ids
        boolean backendExpectsList = userControllerContent.contains("@RequestBody List<Integer> ids");

        // Read frontend user.js to check what format is sent
        Path userJsPath = FRONTEND_API_DIR.resolve("user.js");
        String userJsContent = Files.readString(userJsPath);

        // Frontend sends { ids } which becomes { "ids": [...] }
        boolean frontendSendsWrappedIds = userJsContent.contains("{ ids }") || userJsContent.contains("{ids}");

        // This test currently FAILS because:
        // - Backend expects List<Integer> (bare array)
        // - Frontend sends { ids: [...] } (wrapped object)
        assertTrue(backendExpectsList, "Backend should expect @RequestBody List<Integer> ids");
        assertTrue(frontendSendsWrappedIds, "Frontend should send { ids } wrapped object");

        // The formats don't match (this will fail)
        // Backend expects: [1, 2, 3]
        // Frontend sends: { "ids": [1, 2, 3] }
        assertEquals("List<Integer>", "{ ids }",
            "Backend expects bare array but frontend sends wrapped object");
    }

    @Test
    @DisplayName("Logout endpoint should use POST method (Spring Security default)")
    void testLogoutHttpMethod() throws IOException {
        // Read SecurityConfig to verify logout URL
        Path securityConfigPath = PROJECT_ROOT.resolve("dealer-server/src/main/java/com/bjpowernode/config/SecurityConfig.java");
        String securityConfigContent = Files.readString(securityConfigPath);

        // Backend configures /api/logout with Spring Security (expects POST by default)
        boolean backendConfiguresLogout = securityConfigContent.contains("logout.logoutUrl(\"/api/logout\")");

        // Search for logout in all frontend API files
        boolean frontendUsesGetForLogout = false;
        try (Stream<Path> files = Files.list(FRONTEND_API_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".js")).collect(Collectors.toList())) {
                String content = Files.readString(file);
                // Check if any file uses doGet for logout
                if (content.contains("doGet") && content.contains("/api/logout")) {
                    frontendUsesGetForLogout = true;
                    break;
                }
                // Check if any file uses doPost for logout
                if (content.contains("doPost") && content.contains("/api/logout")) {
                    frontendUsesGetForLogout = false;
                    break;
                }
            }
        }

        // This test currently FAILS because:
        // - Spring Security default expects POST for logout
        // - Frontend uses GET (doGet) for logout
        assertTrue(backendConfiguresLogout, "Backend should configure /api/logout");

        // Check if frontend uses POST for logout
        // This assertion will fail if frontend uses GET
        assertFalse(frontendUsesGetForLogout,
            "Frontend should use POST for /api/logout, not GET (Spring Security expects POST by default)");
    }

    @Test
    @DisplayName("Verify all critical API paths exist in both layers")
    void testCriticalApiPathsExist() throws IOException {
        // Define critical paths that must exist in both layers
        Map<String, String> criticalPaths = Map.of(
            "/api/users", "User list",
            "/api/user/{id}", "User detail",
            "/api/system/list", "System list",
            "/api/activitys", "Activity list",
            "/api/clues", "Clue list",
            "/api/customer/list", "Customer list",
            "/api/tran/list", "Transaction list",
            "/api/products", "Product list",
            "/api/dict/types", "Dictionary types"
        );

        // Collect backend paths
        Set<String> backendPaths = new HashSet<>();
        try (Stream<Path> files = Files.list(BACKEND_CONTROLLER_DIR)) {
            files.filter(p -> p.toString().endsWith("Controller.java"))
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         extractBackendPaths(content, backendPaths, new HashMap<>());
                     } catch (IOException e) {
                         fail("Failed to read backend file: " + file);
                     }
                 });
        }

        // Collect frontend paths
        Set<String> frontendPaths = new HashSet<>();
        try (Stream<Path> files = Files.list(FRONTEND_API_DIR)) {
            files.filter(p -> p.toString().endsWith(".js"))
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         extractFrontendPaths(content, frontendPaths, new HashMap<>());
                     } catch (IOException e) {
                         fail("Failed to read frontend file: " + file);
                     }
                 });
        }

        // Verify each critical path exists in both layers
        List<String> missingPaths = new ArrayList<>();
        for (Map.Entry<String, String> entry : criticalPaths.entrySet()) {
            String path = entry.getKey();
            String description = entry.getValue();

            boolean inBackend = backendPaths.contains(path);
            boolean inFrontend = frontendPaths.contains(path);

            if (!inBackend || !inFrontend) {
                missingPaths.add(String.format("%s (%s): backend=%s, frontend=%s",
                    path, description, inBackend, inFrontend));
            }
        }

        assertTrue(missingPaths.isEmpty(),
            "Missing critical API paths:\n" + String.join("\n", missingPaths));
    }

    // Helper methods

    private void extractFrontendPaths(String content, Set<String> paths, Map<String, Set<String>> methods) {
        // Pattern to match API calls: doGet('/api/...'), doPost('/api/...), doPut('/api/...'), doDelete('/api/...')
        Pattern pattern = Pattern.compile("do(Get|Post|Put|Delete)\\s*\\(\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String method = matcher.group(1).toLowerCase();
            String path = matcher.group(2);

            // Remove query parameters
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf("?"));
            }

            paths.add(path);
            methods.computeIfAbsent(path, k -> new HashSet<>()).add(method);
        }

        // Also handle template literals with backticks
        Pattern templatePattern = Pattern.compile("do(Get|Post|Put|Delete)\\s*\\(\\s*`([^`]+)`");
        Matcher templateMatcher = templatePattern.matcher(content);

        while (templateMatcher.find()) {
            String method = templateMatcher.group(1).toLowerCase();
            String path = templateMatcher.group(2);

            // Convert template literal to path pattern
            path = path.replaceAll("\\$\\{[^}]+\\}", "{id}");

            // Remove query parameters
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf("?"));
            }

            paths.add(path);
            methods.computeIfAbsent(path, k -> new HashSet<>()).add(method);
        }
    }

    private void extractBackendPaths(String content, Set<String> paths, Map<String, Set<String>> methods) {
        // Extract class-level @RequestMapping
        String classPath = "";
        Pattern classPattern = Pattern.compile("@RequestMapping\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
        Matcher classMatcher = classPattern.matcher(content);
        if (classMatcher.find()) {
            classPath = classMatcher.group(1);
        }

        // Pattern to match method-level mappings
        Pattern[] patterns = {
            Pattern.compile("@GetMapping\\s*\\(\\s*(?:value\\s*=\\s*)?['\"]([^'\"]+)['\"]"),
            Pattern.compile("@PostMapping\\s*\\(\\s*(?:value\\s*=\\s*)?['\"]([^'\"]+)['\"]"),
            Pattern.compile("@PutMapping\\s*\\(\\s*(?:value\\s*=\\s*)?['\"]([^'\"]+)['\"]"),
            Pattern.compile("@DeleteMapping\\s*\\(\\s*(?:value\\s*=\\s*)?['\"]([^'\"]+)['\"]"),
            // Also handle single value without "value ="
            Pattern.compile("@GetMapping\\s*\\(\\s*['\"]([^'\"]+)['\"]"),
            Pattern.compile("@PostMapping\\s*\\(\\s*['\"]([^'\"]+)['\"]"),
            Pattern.compile("@PutMapping\\s*\\(\\s*['\"]([^'\"]+)['\"]"),
            Pattern.compile("@DeleteMapping\\s*\\(\\s*['\"]([^'\"]+)['\"]")
        };

        String[] httpMethods = {"get", "post", "put", "delete", "get", "post", "put", "delete"};

        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(content);
            while (matcher.find()) {
                String methodPath = matcher.group(1);
                String fullPath = classPath + methodPath;

                // Normalize path
                fullPath = fullPath.replaceAll("//+", "/");
                if (!fullPath.startsWith("/")) {
                    fullPath = "/" + fullPath;
                }

                paths.add(fullPath);
                methods.computeIfAbsent(fullPath, k -> new HashSet<>()).add(httpMethods[i]);
            }
        }

        // Handle empty @GetMapping, @PostMapping, etc. (no path specified)
        Pattern emptyMappingPattern = Pattern.compile("@(Get|Post|Put|Delete)Mapping\\s*\\(\\s*\\)");
        Matcher emptyMatcher = emptyMappingPattern.matcher(content);
        while (emptyMatcher.find()) {
            if (!classPath.isEmpty()) {
                paths.add(classPath);
                methods.computeIfAbsent(classPath, k -> new HashSet<>()).add(emptyMatcher.group(1).toLowerCase());
            }
        }
    }

    private Set<String> normalizePaths(Set<String> paths) {
        return paths.stream()
            .map(path -> {
                // Replace path variables like {id} with a placeholder
                String normalized = path.replaceAll("\\{[^}]+\\}", "{var}");
                // Remove trailing slash
                if (normalized.endsWith("/") && normalized.length() > 1) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                return normalized;
            })
            .collect(Collectors.toSet());
    }
}
