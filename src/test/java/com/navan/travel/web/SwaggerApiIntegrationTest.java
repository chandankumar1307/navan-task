package com.navan.travel.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("OpenAPI JSON endpoint (/v3/api-docs) returns complete OpenAPI specification")
    void testOpenApiJsonSpecification() {
        String url = "http://localhost:" + port + "/v3/api-docs";
        ResponseEntity<JsonNode> res = restTemplate.getForEntity(url, JsonNode.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = res.getBody();
        assertThat(root).isNotNull();

        // Info block
        assertThat(root.get("openapi").asText()).startsWith("3.");
        assertThat(root.get("info").get("title").asText()).isEqualTo("Navan Commerce: Travel Inventory & Invoicing API");
        assertThat(root.get("info").get("version").asText()).isEqualTo("1.0.0");

        // Paths presence
        JsonNode paths = root.get("paths");
        assertThat(paths.has("/transactions")).isTrue();
        assertThat(paths.has("/transactions/{id}")).isTrue();
        assertThat(paths.has("/invoices")).isTrue();
        assertThat(paths.has("/invoices/{id}")).isTrue();
        assertThat(paths.has("/health")).isTrue();

        // Components & Schemas
        JsonNode schemas = root.get("components").get("schemas");
        assertThat(schemas.has("CreateTransactionRequest")).isTrue();
        assertThat(schemas.has("TransactionResponse")).isTrue();
        assertThat(schemas.has("CreateInvoiceRequest")).isTrue();
        assertThat(schemas.has("InvoiceResponse")).isTrue();
        assertThat(schemas.has("FlightMetadata")).isTrue();
        assertThat(schemas.has("HotelMetadata")).isTrue();
        assertThat(schemas.has("RailMetadata")).isTrue();
        assertThat(schemas.has("NavanFeeMetadata")).isTrue();
        assertThat(schemas.has("TaxLineDto")).isTrue();
        assertThat(schemas.has("FeeLineDto")).isTrue();
        assertThat(schemas.has("TaxRollupItem")).isTrue();
        assertThat(schemas.has("FeeRollupItem")).isTrue();
        assertThat(schemas.has("ConflictProblemDetailDto")).isTrue();
    }

    @Test
    @DisplayName("Swagger UI HTML endpoint (/swagger-ui/index.html) returns 200 OK")
    void testSwaggerUiHtml() {
        String url = "http://localhost:" + port + "/swagger-ui/index.html";
        ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("swagger-ui");
    }
}
