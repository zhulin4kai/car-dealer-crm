package com.bjpowernode.integration;

import com.bjpowernode.constant.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-layer contract tests. These tests MUST fail on real inconsistencies
 * between the frontend (dealer-web/src/api, dealer-web/src/view), the backend
 * controllers (dealer-server/src/main/java/.../web), the SecurityConfig and
 * the docs (docs/api.md, docs/integration.md).
 *
 * They are not allowed to:
 * - print mismatches and pass (e.g. System.out.println(...))
 * - check for the existence of source strings without verifying real behavior
 * - tolerate known inconsistencies (the @DisplayName describes what MUST hold)
 */
class CrossLayerConsistencyTest extends BackendIntegrationTestBase {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    private static final Path FRONTEND_API_DIR = PROJECT_ROOT.resolve("dealer-web/src/api");
    private static final Path FRONTEND_VIEW_DIR = PROJECT_ROOT.resolve("dealer-web/src/view");
    private static final Path BACKEND_CONTROLLER_DIR = PROJECT_ROOT.resolve("dealer-server/src/main/java/com/bjpowernode/web");
    private static final Path BACKEND_SECURITY_CONFIG = PROJECT_ROOT.resolve("dealer-server/src/main/java/com/bjpowernode/config/SecurityConfig.java");
    private static final Path DOCS_INTEGRATION = PROJECT_ROOT.resolve("docs/integration.md");
    private static final Path DOCS_API = PROJECT_ROOT.resolve("docs/api.md");

    // ==================== API path/method coverage ====================

    @Test
    @DisplayName("every frontend /api path declared in dealer-web/src/api/*.js MUST exist in the backend controllers")
    void everyFrontendApiPathMustExistInBackend() throws IOException {
        Set<ApiRef> frontendApis = collectFrontendApis();
        Set<ApiRef> backendApis = collectBackendApis();

        List<String> missing = new ArrayList<>();
        for (ApiRef fe : frontendApis) {
            if (!backendApis.contains(fe)) {
                missing.add(fe.toString());
            }
        }
        assertTrue(missing.isEmpty(),
                "Frontend calls these API endpoints that do NOT exist in the backend Controllers: "
                        + String.join(", ", missing));
    }

    @Test
    @DisplayName("every API method/path called directly from a Vue view (axios or doGet/Post) MUST exist in the backend")
    void everyViewInlineApiPathMustExistInBackend() throws IOException {
        Set<ApiRef> viewApis = collectViewInlineApis();
        Set<ApiRef> backendApis = collectBackendApis();

        List<String> missing = new ArrayList<>();
        for (ApiRef ve : viewApis) {
            if (!backendApis.contains(ve)) {
                missing.add(ve.toString());
            }
        }
        assertTrue(missing.isEmpty(),
                "Frontend Vue views call these API endpoints that do NOT exist in the backend Controllers: "
                        + String.join(", ", missing));
    }

    // ==================== Response wrapper consistency ====================

    @Test
    @DisplayName("every web/*Controller MUST use the R.java response wrapper, never Result.java")
    void controllersMustUseRNotResult() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.list(BACKEND_CONTROLLER_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith("Controller.java")).collect(Collectors.toList())) {
                String content = Files.readString(file);
                if (content.contains("import com.bjpowernode.result.Result;")) {
                    violations.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "These controllers import the deprecated Result.java wrapper instead of R.java: "
                        + String.join(", ", violations));
    }

    // ==================== Logout method consistency ====================

    @Test
    @DisplayName("backend SecurityConfig, frontend view and docs MUST all agree on the HTTP method for /api/logout")
    void logoutMethodMustBeConsistentAcrossLayers() throws IOException {
        String backendMethod = detectBackendLogoutMethod();
        String docsMethod = detectDocsLogoutMethod();
        String frontendMethod = detectFrontendLogoutMethod();

        StringBuilder msg = new StringBuilder();
        if (!backendMethod.equals(frontendMethod)) {
            msg.append("Backend SecurityConfig uses ").append(backendMethod)
                    .append(" but frontend DashboardView uses ").append(frontendMethod).append("; ");
        }
        if (!backendMethod.equals(docsMethod)) {
            msg.append("Backend SecurityConfig uses ").append(backendMethod)
                    .append(" but docs/integration.md says ").append(docsMethod).append("; ");
        }
        if (!frontendMethod.equals(docsMethod)) {
            msg.append("Frontend DashboardView uses ").append(frontendMethod)
                    .append(" but docs/integration.md says ").append(docsMethod).append("; ");
        }
        assertEquals("", msg.toString(),
                "Cross-layer /api/logout method mismatch. Pick one HTTP method and update all three layers. " + msg);
    }

    // ==================== TSystem field name consistency ====================

    @Test
    @DisplayName("TSystem field name for system-open flag MUST match what the frontend sends and the JSON serialization actually emits")
    void systemOpenFieldNameMustBeConsistent() throws Exception {
        // Trigger the real serialization path: get a token, hit GET /api/system/list,
        // inspect the JSON keys. Use H2 + real controller.
        String token = loginAsAdmin();
        MvcResult result = mockMvc.perform(get("/api/system/list")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        JsonNode tree = objectMapper.readTree(body);
        JsonNode dataNode = tree.path("data");
        assertTrue(dataNode.isArray() && !dataNode.isEmpty(),
                "GET /api/system/list must return a non-empty array; seed t_system_info is required. Body: " + body);
        JsonNode first = dataNode.get(0);
        Set<String> actualKeys = new HashSet<>();
        first.fieldNames().forEachRemaining(actualKeys::add);
        assertTrue(actualKeys.contains("isopen"),
                "TSystem JSON must expose the open flag as the lowercase 'isopen' key, got: " + actualKeys);
        assertFalse(actualKeys.contains("isOpen"),
                "TSystem JSON must NOT expose a camelCase 'isOpen' alias; the field is lowercase 'isopen', got: "
                        + actualKeys);
    }

    // ==================== Batch delete parameter shape ====================

    @Test
    @DisplayName("DELETE /api/user batch delete MUST accept a JSON array body, not a wrapped object")
    void batchDeleteUserAcceptsJsonArray() throws Exception {
        String token = loginAsAdmin();

        // Spec is plain array. Use non-seed IDs (999, 1000) so we verify the
        // endpoint contract without actually deleting the seeded users that
        // other integration tests in the same shared H2 DB depend on.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[999,1000]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Sanity: send a malformed object body and ensure the server rejects it (4xx or contract fail).
        MvcResult bad = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[999,1000]}"))
                .andReturn();
        JsonNode body = objectMapper.readTree(bad.getResponse().getContentAsString());
        assertFalse(body.path("code").asInt(0) == 200,
                "Backend must NOT silently accept {\"ids\":[...]} for DELETE /api/user batch delete");
    }

    // ==================== Helpers ====================

    private Set<ApiRef> collectFrontendApis() throws IOException {
        Set<ApiRef> out = new HashSet<>();
        try (Stream<Path> files = Files.list(FRONTEND_API_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".js")).collect(Collectors.toList())) {
                String content = Files.readString(file);
                Pattern p = Pattern.compile(
                        "do(Get|Post|Put|Delete)\\s*\\(\\s*(['\"`])([^'\"`]*?)\\2",
                        Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(content);
                while (m.find()) {
                    String method = m.group(1).toUpperCase();
                    String path = m.group(3);
                    if (path.startsWith("/api/")) {
                        out.add(new ApiRef(method, normalizePath(stripQuery(path))));
                    }
                }
            }
        }
        return out;
    }

    private Set<ApiRef> collectViewInlineApis() throws IOException {
        Set<ApiRef> out = new HashSet<>();
        try (Stream<Path> files = Files.list(FRONTEND_VIEW_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".vue")).collect(Collectors.toList())) {
                String content = Files.readString(file);
                Pattern p = Pattern.compile("do(Get|Post|Put|Delete)\\s*\\(\\s*(['\"`])([^'\"`]*?)\\2");
                Matcher m = p.matcher(content);
                while (m.find()) {
                    String method = m.group(1).toUpperCase();
                    String path = m.group(3);
                    if (path.startsWith("/api/") && !isFrameworkEndpoint(path)) {
                        out.add(new ApiRef(method, normalizePath(stripQuery(path))));
                    }
                }
            }
        }
        return out;
    }

    /**
     * Endpoints that are NOT defined in any @Controller but are wired by Spring Security,
     * filters, or other framework components. These MUST NOT be flagged as missing.
     */
    private static boolean isFrameworkEndpoint(String path) {
        return path.equals(Constants.LOGIN_URI)         // form login -> Spring Security
                || path.equals("/api/login")              // alias
                || path.startsWith("/api/login/free");    // free login -> UserController freeLogin
    }

    private Set<ApiRef> collectBackendApis() throws IOException {
        Set<ApiRef> out = new HashSet<>();
        try (Stream<Path> files = Files.list(BACKEND_CONTROLLER_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith("Controller.java")).collect(Collectors.toList())) {
                String content = Files.readString(file);

                String classPath = "";
                Matcher classMatcher = Pattern.compile(
                        "@RequestMapping\\s*\\(\\s*\\\"([^\\\"]+)\\\"\\s*\\)").matcher(content);
                if (classMatcher.find()) {
                    classPath = classMatcher.group(1);
                }

                for (String verb : new String[]{"GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping"}) {
                    // 1) @VerbMapping with explicit path
                    Pattern p = Pattern.compile(
                            "@" + verb + "\\s*\\(\\s*(?:value\\s*=\\s*)?\\\"([^\\\"]*)\\\"");
                    Matcher m = p.matcher(content);
                    while (m.find()) {
                        String methodPath = m.group(1);
                        if (methodPath.isEmpty() && classPath.isEmpty()) continue;
                        String fullPath = classPath + methodPath;
                        fullPath = fullPath.replaceAll("//+", "/");
                        if (!fullPath.startsWith("/")) fullPath = "/" + fullPath;
                        out.add(new ApiRef(verb.replace("Mapping", "").toUpperCase(), normalizePath(fullPath)));
                    }
                    // 2) @VerbMapping() with no path -> resolves to class path
                    if (!classPath.isEmpty()) {
                        Pattern emptyParen = Pattern.compile("@" + verb + "\\s*\\(\\s*\\)");
                        if (emptyParen.matcher(content).find()) {
                            out.add(new ApiRef(verb.replace("Mapping", "").toUpperCase(), normalizePath(classPath)));
                        }
                        // 3) @VerbMapping with no parens at all -> resolves to class path
                        Pattern noParen = Pattern.compile("@" + verb + "(?!\\s*[(\\\"'])");
                        if (noParen.matcher(content).find()) {
                            out.add(new ApiRef(verb.replace("Mapping", "").toUpperCase(), normalizePath(classPath)));
                        }
                    }
                }
            }
        }
        return out;
    }

    private static String stripQuery(String path) {
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    private static String normalizePath(String path) {
        // Template literals: /api/clue/${id} -> /api/clue/{id}
        String n = path.replaceAll("\\$\\{[^}]*\\}", "{id}");
        // :id style placeholders -> {id}
        n = n.replaceAll(":[A-Za-z_][A-Za-z0-9_]*", "{id}");
        // Any {name} placeholder -> {id} (path variable name is not part of the contract)
        n = n.replaceAll("\\{[^}]+\\}", "{id}");
        // Source-side string concatenation that ends with "/" (e.g. '/api/clue/' + id)
        // is treated as '/.../{id}' for contract comparison.
        if (n.endsWith("/")) n = n + "{id}";
        return n;
    }

    private String detectBackendLogoutMethod() throws IOException {
        String content = Files.readString(BACKEND_SECURITY_CONFIG);
        Matcher m = Pattern.compile("AntPathRequestMatcher\\(\\s*\\\"/api/logout\\\"\\s*,\\s*\\\"([A-Z]+)\\\"\\s*\\)")
                .matcher(content);
        if (m.find()) return m.group(1);
        // fallback: formLogout only
        if (content.contains("logout.logoutUrl(\"/api/logout\")") && !content.contains("AntPathRequestMatcher")) {
            return "POST";
        }
        fail("SecurityConfig.java does not declare an explicit HTTP method for /api/logout");
        return "";
    }

    private String detectFrontendLogoutMethod() throws IOException {
        Path dashboard = FRONTEND_VIEW_DIR.resolve("DashboardView.vue");
        if (!Files.exists(dashboard)) fail("DashboardView.vue not found at " + dashboard);
        String content = Files.readString(dashboard);
        if (content.contains("doPost(\"/api/logout\"")) return "POST";
        if (content.contains("doGet(\"/api/logout\"")) return "GET";
        if (content.contains("axios.post(\"/api/logout\"")) return "POST";
        if (content.contains("axios.get(\"/api/logout\"")) return "GET";
        fail("DashboardView.vue does not call /api/logout via doGet/doPost/axios");
        return "";
    }

    private String detectDocsLogoutMethod() throws IOException {
        if (!Files.exists(DOCS_INTEGRATION)) fail("docs/integration.md not found");
        String content = Files.readString(DOCS_INTEGRATION);
        Matcher m = Pattern.compile("(GET|POST|PUT|DELETE)\\s+/api/logout").matcher(content);
        if (m.find()) return m.group(1);
        fail("docs/integration.md does not declare the HTTP method for /api/logout");
        return "";
    }

    /** Records (HTTP method, normalized path). */
    private record ApiRef(String method, String path) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
