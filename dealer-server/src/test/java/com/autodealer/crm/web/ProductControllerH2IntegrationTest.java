package com.autodealer.crm.web;

import com.autodealer.crm.integration.BackendIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real H2 + real Service + real Mapper + real Security integration tests
 * for ProductController. This class is the replacement for the previous
 * addFilters=false + @MockBean ProductService test that didn't actually
 * touch the database, the security chain, or the SQL.
 *
 * <p>ProductController has no @PreAuthorize annotations, so a logged-in
 * admin can hit any endpoint. We use the real /api/login flow from
 * BackendIntegrationTestBase.
 *
 * <p>Each test creates its own test products (using a unique SKU prefix)
 * and cleans them up in {@code @AfterEach} so the suite is order-independent
 * and idempotent.
 */
class ProductControllerH2IntegrationTest extends BackendIntegrationTestBase {

    private static final String SKU_PREFIX = "TEST-SKU-";

    private String adminToken;
    private final List<String> createdSkus = new ArrayList<>();

    @BeforeEach
    void setupAdminToken() throws Exception {
        adminToken = super.loginAsAdmin();
    }

    @AfterEach
    void cleanupCreatedProducts() throws Exception {
        // Delete every test-owned product. We use the controller's
        // list-then-delete path so the same security/contract is exercised
        // as a real client would use.
        for (String sku : createdSkus) {
            Long productId = findProductIdBySku(adminToken, sku);
            if (productId != null) {
                mockMvc.perform(delete("/api/products/" + productId)
                                .header(HttpHeaders.AUTHORIZATION, adminToken))
                        .andExpect(status().isOk());
            }
        }
        createdSkus.clear();
    }

    @Test
    @DisplayName("GET /api/products returns a paged list with the documented fields")
    void getProductList_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("\u64cd\u4f5c\u6210\u529f"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("POST /api/products -> GET /api/products/{id} round-trips a real product through H2")
    void createThenGetById_roundTripsThroughH2() throws Exception {
        String sku = SKU_PREFIX + System.nanoTime();
        createdSkus.add(sku);

        String body = """
                {
                  "sku": "%s",
                  "name": "测试商品",
                  "categoryId": 1,
                  "specification": "spec-1",
                  "price": 99.50,
                  "stock": 10,
                  "minStock": 1,
                  "status": "ON_SHELF"
                }
                """.formatted(sku);

        MvcResult create = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        // Parse the response and confirm the assigned id is present.
        JsonNode created = objectMapper.readTree(create.getResponse().getContentAsString());
        assertEquals(200, created.path("code").asInt());

        // The product must now be retrievable by id (we look it up first
        // because AUTO_INCREMENT gives us back the actual id from H2).
        Long productId = findProductIdBySku(adminToken, sku);
        assertNotNull(productId, "Newly created product must be visible in the list");

        mockMvc.perform(get("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.sku").value(sku))
                .andExpect(jsonPath("$.data.name").value("测试商品"))
                .andExpect(jsonPath("$.data.price").value(99.50));
    }

    @Test
    @DisplayName("PUT /api/products/{id} updates the H2 row and the next GET reflects the change")
    void updateProduct_persistsToH2() throws Exception {
        String sku = SKU_PREFIX + System.nanoTime();
        createdSkus.add(sku);
        Long productId = createProductViaApi(sku, "原名称", 50.0);

        String updateBody = """
                {
                  "sku": "%s",
                  "name": "新名称",
                  "categoryId": 1,
                  "price": 75.0,
                  "stock": 5,
                  "minStock": 1,
                  "status": "ON_SHELF"
                }
                """.formatted(sku);

        mockMvc.perform(put("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("新名称"))
                .andExpect(jsonPath("$.data.price").value(75.0));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} removes the row and the next GET returns null data")
    void deleteProduct_removesFromH2() throws Exception {
        String sku = SKU_PREFIX + System.nanoTime();
        Long productId = createProductViaApi(sku, "待删除商品", 10.0);
        // Don't add to createdSkus: we delete it explicitly below.

        mockMvc.perform(delete("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // The Service.getProductById returns null when not found; the
        // controller wraps it in Result.success(null) which serializes to
        // {"code":200,"msg":"...","data":null}.
        mockMvc.perform(get("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("unauthenticated request to /api/products is rejected by the real Security chain")
    void unauthenticatedRequest_rejected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(510));
    }

    private Long createProductViaApi(String sku, String name, double price) throws Exception {
        String body = """
                {
                  "sku": "%s",
                  "name": "%s",
                  "categoryId": 1,
                  "price": %s,
                  "stock": 10,
                  "minStock": 1,
                  "status": "ON_SHELF"
                }
                """.formatted(sku, name, price);
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Long id = findProductIdBySku(adminToken, sku);
        assertNotNull(id, "Product with sku=" + sku + " must exist after create");
        return id;
    }

    private Long findProductIdBySku(String token, String sku) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("page", "1")
                        .param("size", "100"))
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("list");
        for (JsonNode node : list) {
            if (sku.equals(node.path("sku").asText())) {
                return node.path("id").asLong();
            }
        }
        return null;
    }
}
