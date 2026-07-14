package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.security.SecurityConfig;
import com.autodealer.crm.modules.identity.web.UserController;
import com.autodealer.crm.shared.security.SecurityPaths;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-layer contract tests. These tests MUST fail on real inconsistencies
 * between the frontend API/page layers, the backend
 * controllers (dealer-server/src/main/java/com/autodealer/crm 下递归扫描), the SecurityConfig and
 * the docs (docs/api.md, docs/integration.md).
 *
 * They are not allowed to:
 * - print mismatches and pass (e.g. System.out.println(...))
 * - check for the existence of source strings without verifying real behavior
 * - tolerate known inconsistencies (the @DisplayName describes what MUST hold)
 */
class CrossLayerConsistencyTest extends BackendIntegrationTestBase {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    private static final List<Path> FRONTEND_API_ROOTS = List.of(
            PROJECT_ROOT.resolve("dealer-web/src/modules"),
            PROJECT_ROOT.resolve("dealer-web/src/api")
    );
    private static final List<Path> FRONTEND_VIEW_ROOTS = List.of(
            PROJECT_ROOT.resolve("dealer-web/src/pages"),
            PROJECT_ROOT.resolve("dealer-web/src/layouts"),
            PROJECT_ROOT.resolve("dealer-web/src/view")
    );
    private static final Path FRONTEND_USER_API = PROJECT_ROOT.resolve("dealer-web/src/modules/user/api/user-api.ts");
    private static final Path FRONTEND_DASHBOARD_LAYOUT = PROJECT_ROOT.resolve("dealer-web/src/layouts/DashboardLayout.vue");
    private static final Path BACKEND_JAVA_ROOT = PROJECT_ROOT.resolve("dealer-server/src/main/java/com/autodealer/crm");
    private static final Path BACKEND_SECURITY_CONFIG = PROJECT_ROOT.resolve(
            "dealer-server/src/main/java/com/autodealer/crm/bootstrap/security/SecurityConfig.java");
    private static final Path DOCS_INTEGRATION = PROJECT_ROOT.resolve("docs/integration.md");
    private static final Path DOCS_API = PROJECT_ROOT.resolve("docs/api.md");

    // ==================== API path/method coverage ====================

    @Test
    @DisplayName("every frontend /api path declared in frontend API modules MUST exist in the backend controllers")
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
    @DisplayName("every API method/path called directly from a Vue page/layout MUST exist in the backend")
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
    @DisplayName("every backend *Controller MUST use the shared.web.Result response wrapper")
    void controllersMustUseSharedWebResult() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : collectBackendControllerFiles()) {
            String content = Files.readString(file);
            if (!content.contains("import com.autodealer.crm.shared.web.Result;")) {
                violations.add(BACKEND_JAVA_ROOT.relativize(file).toString());
            }
        }
        assertTrue(violations.isEmpty(),
                "These controllers do not import the shared.web.Result response wrapper: "
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
                    .append(" but frontend logout API uses ").append(frontendMethod).append("; ");
        }
        if (!backendMethod.equals(docsMethod)) {
            msg.append("Backend SecurityConfig uses ").append(backendMethod)
                    .append(" but docs/integration.md says ").append(docsMethod).append("; ");
        }
        if (!frontendMethod.equals(docsMethod)) {
            msg.append("Frontend logout API uses ").append(frontendMethod)
                    .append(" but docs/integration.md says ").append(docsMethod).append("; ");
        }
        assertEquals("", msg.toString(),
                "Cross-layer /api/logout method mismatch. Pick one HTTP method and update all three layers. " + msg);
    }

    // ==================== Legacy batch disable fail-close ====================

    @Test
    @DisplayName("旧批量禁用入口对对象和数组请求都 fail-close 且不产生部分写入")
    void legacyBatchDisableAlwaysFailsClosed() throws Exception {
        String token = loginAsAdmin();

        // Insert temporary test users with non-seed IDs so we can verify the
        // endpoint contract without affecting seeded users other tests depend on.
        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, "
                        + "credentials_no_expired, account_no_locked, account_enabled, create_time, create_by) "
                        + "VALUES (?, ?, ?, ?, 1, 1, 1, 1, CURRENT_TIMESTAMP, 1)",
                999, "_test_cross_999",
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", "_test_cross_999");
        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, "
                        + "credentials_no_expired, account_no_locked, account_enabled, create_time, create_by) "
                        + "VALUES (?, ?, ?, ?, 1, 1, 1, 1, CURRENT_TIMESTAMP, 1)",
                1000, "_test_cross_1000",
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", "_test_cross_1000");

        try {
            // 旧入口即使收到历史对象格式也只能固定拒绝，不能绕过单用户版本命令。
            mockMvc.perform(put("/api/users/batch-disable")
                            .header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\":[999,1000]}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(520));

            // Sanity: send a raw JSON array (old format) and ensure the server rejects it.
            MvcResult bad = mockMvc.perform(put("/api/users/batch-disable")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[999,1000]"))
                    .andReturn();
            JsonNode body = objectMapper.readTree(bad.getResponse().getContentAsString());
            assertFalse(body.path("code").asInt(0) == 200,
                    "Backend must NOT silently accept a raw JSON array [999,1000] for PUT /api/users/batch-disable");
            assertEquals(2, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_user WHERE id IN (999,1000) AND account_enabled=1", Integer.class));
        } finally {
            jdbcTemplate.update("DELETE FROM t_user WHERE id IN (999, 1000)");
        }
    }

    // ==================== Helpers ====================

    private Set<ApiRef> collectFrontendApis() throws IOException {
        Set<ApiRef> out = new HashSet<>();
        for (Path file : collectFrontendFiles(FRONTEND_API_ROOTS, Set.of(".ts", ".js"))) {
            String content = Files.readString(file);
            collectHttpClientRefs(content, out);
            collectLegacyDoRefs(content, out);
        }
        out.removeIf(api -> isFrameworkEndpoint(api.path()));
        return out;
    }

    private Set<ApiRef> collectViewInlineApis() throws IOException {
        Set<ApiRef> out = new HashSet<>();
        for (Path file : collectFrontendFiles(FRONTEND_VIEW_ROOTS, Set.of(".vue"))) {
            String content = Files.readString(file);
            collectLegacyDoRefs(content, out);
            collectHttpClientRefs(content, out);
        }
        out.removeIf(api -> isFrameworkEndpoint(api.path()));
        return out;
    }

    private static List<Path> collectFrontendFiles(List<Path> roots, Set<String> suffixes) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                files.addAll(stream
                        .filter(Files::isRegularFile)
                        .filter(path -> suffixes.stream().anyMatch(suffix -> path.toString().endsWith(suffix)))
                        .collect(Collectors.toList()));
            }
        }
        return files;
    }

    private static void collectLegacyDoRefs(String content, Set<ApiRef> out) {
        Pattern p = Pattern.compile("do(Get|Post|Put|Delete)\\s*\\(\\s*(['\"`])([^'\"`]*?)\\2",
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

    private static void collectHttpClientRefs(String content, Set<ApiRef> out) {
        Pattern p = Pattern.compile(
                "httpClient\\s*\\.\\s*(get|post|put|delete)\\s*(?:<[^\\n(]*>)?\\s*\\(\\s*(['\"`])([^'\"`]*?)\\2",
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

    /**
     * Endpoints that are NOT defined in any @Controller but are wired by Spring Security,
     * filters, or other framework components. These MUST NOT be flagged as missing.
     */
    private static boolean isFrameworkEndpoint(String path) {
        return path.equals(Constants.LOGIN_URI)         // form login -> Spring Security
                || path.equals("/api/login")              // alias
                || path.startsWith("/api/login/free")    // free login -> UserController freeLogin
                || path.equals("/api/logout");            // Spring Security logout filter (GET)
    }

    private Set<ApiRef> collectBackendApis() throws IOException {
        Set<ApiRef> out = new HashSet<>();
        for (Path file : collectBackendControllerFiles()) {
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
        return out;
    }

    private static List<Path> collectBackendControllerFiles() throws IOException {
        if (!Files.exists(BACKEND_JAVA_ROOT)) {
            fail("Backend Java source root not found: " + BACKEND_JAVA_ROOT);
        }
        try (Stream<Path> files = Files.walk(BACKEND_JAVA_ROOT)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .collect(Collectors.toList());
        }
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
        Matcher m = Pattern.compile("AntPathRequestMatcher\\(\\s*(?:\\\"/api/logout\\\"|SecurityPaths\\.LOGOUT)\\s*,\\s*\\\"([A-Z]+)\\\"\\s*\\)")
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
        if (Files.exists(FRONTEND_USER_API)) {
            String method = detectLogoutMethodInContent(Files.readString(FRONTEND_USER_API));
            if (!method.isEmpty()) {
                return method;
            }
        }

        if (Files.exists(FRONTEND_DASHBOARD_LAYOUT)) {
            String method = detectLogoutMethodInContent(Files.readString(FRONTEND_DASHBOARD_LAYOUT));
            if (!method.isEmpty()) {
                return method;
            }
        }

        for (Path file : collectFrontendFiles(FRONTEND_VIEW_ROOTS, Set.of(".vue"))) {
            String method = detectLogoutMethodInContent(Files.readString(file));
            if (!method.isEmpty()) {
                return method;
            }
        }

        fail("Frontend does not declare /api/logout in user-api.ts, DashboardLayout.vue, or Vue pages");
        return "";
    }

    private static String detectLogoutMethodInContent(String content) {
        if (content.contains("httpClient.post('/api/logout'") || content.contains("httpClient.post(\"/api/logout\"")) return "POST";
        if (content.contains("httpClient.get('/api/logout'") || content.contains("httpClient.get(\"/api/logout\"")) return "GET";
        if (content.contains("axios.post(\"/api/logout\"") || content.contains("axios.post('/api/logout'")) return "POST";
        if (content.contains("axios.get(\"/api/logout\"") || content.contains("axios.get('/api/logout'")) return "GET";
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
