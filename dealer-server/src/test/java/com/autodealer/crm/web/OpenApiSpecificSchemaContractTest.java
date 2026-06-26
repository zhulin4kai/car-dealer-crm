package com.autodealer.crm.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenApiSpecificSchemaContractTest {

    private static final Path OPENAPI_FILE = Path.of("../docs/api/openapi.yaml");

    @Test
    void pathResponses_shouldNotUseGenericSuccessSchemas() throws IOException {
        String openApi = Files.readString(OPENAPI_FILE);

        assertFalse(openApi.contains("$ref: \"#/components/responses/PageOk\""));
        assertFalse(openApi.contains("$ref: \"#/components/responses/ObjectOk\""));
        assertFalse(openApi.contains("$ref: \"#/components/responses/ArrayOk\""));
    }

    @Test
    void legacyEndpointsAndPaginationParams_shouldNotRemainInOpenApi() throws IOException {
        String openApi = Files.readString(OPENAPI_FILE);

        assertFalse(openApi.contains("deprecated: true"));
        assertFalse(openApi.contains("  /api/activitys:"));
        assertFalse(openApi.contains("  /api/customer/list:"));
        assertFalse(openApi.contains("  /api/tran/create:"));
        assertFalse(openApi.contains("  /api/tran/batch-delete:"));
        assertFalse(openApi.contains("  /api/tran/payment/{id}/refund:"));
        assertFalse(openApi.contains("name: current"));
        assertFalse(openApi.contains("name: pageSize"));
        assertFalse(openApi.contains("#/components/parameters/Current"));
        assertFalse(openApi.contains("#/components/parameters/PageSize"));
    }
}
