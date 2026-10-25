package com.navan.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TravelApiEdgeCasesIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String randomExternalId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // =========================================================================
    // 1. Transaction Validation & Bad Request Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Transaction creation: missing required top-level fields returns 400 Bad Request")
    void testMissingTopLevelFieldsReturns400() {
        // Missing type
        Map<String, Object> noType = Map.of(
                "external_id", randomExternalId("test"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of()
        );
        ResponseEntity<JsonNode> res1 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(noType, jsonHeaders()), JsonNode.class);
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Missing external_id
        Map<String, Object> noExtId = Map.of(
                "type", "flight",
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("origin", "BER", "destination", "FRA", "fare", 100.0, "coupons", List.of(Map.of("flight_number", "LH1")))
        );
        ResponseEntity<JsonNode> res2 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(noExtId, jsonHeaders()), JsonNode.class);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Missing total
        Map<String, Object> noTotal = Map.of(
                "type", "flight",
                "external_id", randomExternalId("test"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("origin", "BER", "destination", "FRA", "fare", 100.0, "coupons", List.of(Map.of("flight_number", "LH1")))
        );
        ResponseEntity<JsonNode> res3 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(noTotal, jsonHeaders()), JsonNode.class);
        assertThat(res3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Missing metadata
        Map<String, Object> noMetadata = Map.of(
                "type", "flight",
                "external_id", randomExternalId("test"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of()
        );
        ResponseEntity<JsonNode> res4 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(noMetadata, jsonHeaders()), JsonNode.class);
        assertThat(res4.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Empty metadata {}
        Map<String, Object> emptyMetadata = Map.of(
                "type", "flight",
                "external_id", randomExternalId("test"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of()
        );
        ResponseEntity<JsonNode> res5 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(emptyMetadata, jsonHeaders()), JsonNode.class);
        assertThat(res5.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Transaction creation: flight metadata missing coupons or flight_number returns 400")
    void testFlightMetadataValidationEdgeCases() {
        // Flight with empty coupons list
        Map<String, Object> emptyCoupons = Map.of(
                "type", "flight",
                "external_id", randomExternalId("flt-bad"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "origin", "BER",
                        "destination", "MUC",
                        "fare", 100.0,
                        "coupons", List.of()
                )
        );
        ResponseEntity<JsonNode> res1 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(emptyCoupons, jsonHeaders()), JsonNode.class);
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Flight with coupon missing flight_number
        Map<String, Object> missingFltNum = Map.of(
                "type", "flight",
                "external_id", randomExternalId("flt-bad"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "origin", "BER",
                        "destination", "MUC",
                        "fare", 100.0,
                        "coupons", List.of(
                                Map.of(
                                        "carrier", "LH",
                                        "from", "BER",
                                        "to", "MUC"
                                )
                        )
                )
        );
        ResponseEntity<JsonNode> res2 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(missingFltNum, jsonHeaders()), JsonNode.class);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Transaction creation: hotel metadata missing address or 0 nights returns 400")
    void testHotelMetadataValidationEdgeCases() {
        // Missing address
        Map<String, Object> missingAddress = Map.of(
                "type", "hotel",
                "external_id", randomExternalId("htl-bad"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T15:00:00Z",
                "total", 200.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "name", "Grand Hotel",
                        "check_in", "2026-03-11",
                        "check_out", "2026-03-12",
                        "nights", 1,
                        "room_rate_per_night", 200.0
                )
        );
        ResponseEntity<JsonNode> res1 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(missingAddress, jsonHeaders()), JsonNode.class);
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Nights = 0
        Map<String, Object> zeroNights = Map.of(
                "type", "hotel",
                "external_id", randomExternalId("htl-bad"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T15:00:00Z",
                "total", 200.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "name", "Grand Hotel",
                        "address", Map.of("street", "1 Main St", "city", "Berlin", "country", "DE"),
                        "check_in", "2026-03-11",
                        "check_out", "2026-03-12",
                        "nights", 0,
                        "room_rate_per_night", 200.0
                )
        );
        ResponseEntity<JsonNode> res2 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(zeroNights, jsonHeaders()), JsonNode.class);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Transaction creation: rail metadata with empty legs returns 400")
    void testRailMetadataEmptyLegsReturns400() {
        Map<String, Object> emptyLegs = Map.of(
                "type", "rail",
                "external_id", randomExternalId("rail-bad"),
                "currency", "EUR",
                "occurred_at", "2026-03-13T09:00:00Z",
                "total", 80.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("legs", List.of())
        );
        ResponseEntity<JsonNode> res = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(emptyLegs, jsonHeaders()), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Transaction creation: cross-type metadata mismatch (flight with hotel blob) returns 400")
    void testCrossTypeMismatchReturns400() {
        Map<String, Object> mismatch = Map.of(
                "type", "flight",
                "external_id", randomExternalId("flt-mismatch"),
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "name", "Grand Hotel",
                        "address", Map.of("street", "1 Main St", "city", "Berlin", "country", "DE"),
                        "nights", 1
                )
        );
        ResponseEntity<JsonNode> res = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(mismatch, jsonHeaders()), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 2. Transaction Read & Not Found Unhappy Paths
    // =========================================================================

    @Test
    @DisplayName("GET /transactions/{non_existent_id} returns 404 Not Found")
    void testGetNonExistentTransactionReturns404() {
        ResponseEntity<JsonNode> res = restTemplate.getForEntity(baseUrl + "/transactions/txn_non_existent_9999", JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().get("status").asInt()).isEqualTo(404);
        assertThat(res.getBody().get("title").asText()).isEqualTo("Not Found");
    }

    // =========================================================================
    // 3. Non-Invoiced Transaction Mutation (PATCH) & Deletion (DELETE)
    // =========================================================================

    @Test
    @DisplayName("Non-invoiced transaction can be PATCHed and updated, then DELETEd atomically")
    void testNonInvoicedTransactionLifecycle() {
        // 1. Create a non-invoiced flight transaction
        String extId = randomExternalId("flt-lifecycle");
        Map<String, Object> payload = Map.of(
                "type", "flight",
                "external_id", extId,
                "currency", "EUR",
                "occurred_at", "2026-05-01T08:00:00Z",
                "total", 150.00,
                "tax_lines", List.of(Map.of("name", "Tax1", "amount", 10.00)),
                "fee_lines", List.of(Map.of("name", "Fee1", "amount", 5.00)),
                "metadata", Map.of(
                        "origin", "BER",
                        "destination", "FRA",
                        "fare", 135.00,
                        "coupons", List.of(
                                Map.of(
                                        "flight_number", "LH100",
                                        "from", "BER",
                                        "to", "FRA",
                                        "departure", "2026-05-01T08:00:00Z",
                                        "arrival", "2026-05-01T09:10:00Z"
                                )
                        )
                )
        );

        ResponseEntity<JsonNode> createRes = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(payload, jsonHeaders()), JsonNode.class);
        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String txnId = createRes.getBody().get("id").asText();

        // 2. PATCH the non-invoiced transaction
        Map<String, Object> patchPayload = Map.of(
                "total", 250.00,
                "occurred_at", "2026-05-01T12:00:00Z",
                "tax_lines", List.of(Map.of("name", "TaxUpdated", "amount", 20.00)),
                "fee_lines", List.of(Map.of("name", "FeeUpdated", "amount", 10.00))
        );

        ResponseEntity<JsonNode> patchRes = restTemplate.exchange(
                baseUrl + "/transactions/" + txnId,
                HttpMethod.PATCH,
                new HttpEntity<>(patchPayload, jsonHeaders()),
                JsonNode.class
        );
        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchRes.getBody().get("total").asDouble()).isEqualTo(250.00);
        assertThat(patchRes.getBody().get("tax_lines").get(0).get("name").asText()).isEqualTo("TaxUpdated");

        // Verify GET returns updated data
        ResponseEntity<JsonNode> getRes = restTemplate.getForEntity(baseUrl + "/transactions/" + txnId, JsonNode.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody().get("total").asDouble()).isEqualTo(250.00);

        // 3. DELETE the non-invoiced transaction
        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                baseUrl + "/transactions/" + txnId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify subsequent GET returns 404
        ResponseEntity<JsonNode> getAfterDel = restTemplate.getForEntity(baseUrl + "/transactions/" + txnId, JsonNode.class);
        assertThat(getAfterDel.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PATCH and DELETE on non-existent transaction return 404 Not Found")
    void testPatchAndDeleteNonExistentTransactionReturn404() {
        ResponseEntity<JsonNode> patchRes = restTemplate.exchange(
                baseUrl + "/transactions/txn_ghost",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("total", 100.00), jsonHeaders()),
                JsonNode.class
        );
        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<JsonNode> delRes = restTemplate.exchange(
                baseUrl + "/transactions/txn_ghost",
                HttpMethod.DELETE,
                null,
                JsonNode.class
        );
        assertThat(delRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // 4. Invoice Creation Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Invoice creation: single transaction_id body shape succeeds")
    void testSingleTransactionIdInvoiceCreation() {
        String extId = randomExternalId("single-inv-test");
        Map<String, Object> payload = Map.of(
                "type", "navan_fee",
                "external_id", extId,
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 25.00,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("fee_kind", "trip_fee")
        );
        ResponseEntity<JsonNode> createTxn = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(payload, jsonHeaders()), JsonNode.class);
        String txnId = createTxn.getBody().get("id").asText();

        // Create invoice with single transaction_id shape
        Map<String, Object> invReq = Map.of("transaction_id", txnId);
        ResponseEntity<JsonNode> invRes = restTemplate.postForEntity(baseUrl + "/invoices", new HttpEntity<>(invReq, jsonHeaders()), JsonNode.class);

        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(invRes.getBody().get("grand_total").asDouble()).isEqualTo(25.00);
        assertThat(invRes.getBody().get("totals").get("trip_fee").asDouble()).isEqualTo(25.00);
        assertThat(invRes.getBody().get("transactions")).hasSize(1);
    }

    @Test
    @DisplayName("Invoice creation: duplicate transaction IDs in request returns 409 Conflict")
    void testDuplicateTransactionIdsInInvoiceRequestReturns409() {
        String extId = randomExternalId("dup-inv-test");
        Map<String, Object> payload = Map.of(
                "type", "navan_fee",
                "external_id", extId,
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 15.00,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("fee_kind", "agent_call_fee")
        );
        ResponseEntity<JsonNode> createTxn = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(payload, jsonHeaders()), JsonNode.class);
        String txnId = createTxn.getBody().get("id").asText();

        // Pass same ID twice in transaction_ids
        Map<String, Object> dupReq = Map.of("transaction_ids", List.of(txnId, txnId));
        ResponseEntity<JsonNode> invRes = restTemplate.postForEntity(baseUrl + "/invoices", new HttpEntity<>(dupReq, jsonHeaders()), JsonNode.class);

        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(invRes.getBody().get("status").asInt()).isEqualTo(409);
        assertThat(invRes.getBody().get("detail").asText()).contains("Duplicate transaction ID");
    }

    @Test
    @DisplayName("Invoice creation: missing transaction ID returns 404 Not Found")
    void testMissingTransactionIdInInvoiceReturns404() {
        Map<String, Object> invReq = Map.of("transaction_ids", List.of("txn_missing_12345"));
        ResponseEntity<JsonNode> invRes = restTemplate.postForEntity(baseUrl + "/invoices", new HttpEntity<>(invReq, jsonHeaders()), JsonNode.class);

        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Invoice creation: empty invoice request body returns 400 Bad Request")
    void testEmptyInvoiceRequestBodyReturns400() {
        Map<String, Object> invReq = Map.of();
        ResponseEntity<JsonNode> invRes = restTemplate.postForEntity(baseUrl + "/invoices", new HttpEntity<>(invReq, jsonHeaders()), JsonNode.class);

        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Invoice creation: multi-rate tax lines (VAT 7% and VAT 19%) produce distinct rollup entries")
    void testMultiRateTaxRollupInvoicing() {
        // Txn 1 with VAT 7%
        String ext1 = randomExternalId("tax-7pct");
        Map<String, Object> p1 = Map.of(
                "type", "hotel",
                "external_id", ext1,
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 107.00,
                "tax_lines", List.of(Map.of("name", "VAT", "rate", 0.07, "amount", 7.00)),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "name", "Hotel Low Tax",
                        "address", Map.of("street", "1 Main", "city", "Berlin", "country", "DE"),
                        "check_in", "2026-03-11",
                        "check_out", "2026-03-12",
                        "nights", 1,
                        "room_rate_per_night", 100.00
                )
        );
        String id1 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(p1, jsonHeaders()), JsonNode.class).getBody().get("id").asText();

        // Txn 2 with VAT 19%
        String ext2 = randomExternalId("tax-19pct");
        Map<String, Object> p2 = Map.of(
                "type", "hotel",
                "external_id", ext2,
                "currency", "EUR",
                "occurred_at", "2026-03-12T10:00:00Z",
                "total", 119.00,
                "tax_lines", List.of(Map.of("name", "VAT", "rate", 0.19, "amount", 19.00)),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "name", "Hotel Standard Tax",
                        "address", Map.of("street", "2 Main", "city", "Berlin", "country", "DE"),
                        "check_in", "2026-03-12",
                        "check_out", "2026-03-13",
                        "nights", 1,
                        "room_rate_per_night", 100.00
                )
        );
        String id2 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(p2, jsonHeaders()), JsonNode.class).getBody().get("id").asText();

        // Invoice both
        Map<String, Object> invReq = Map.of("transaction_ids", List.of(id1, id2));
        ResponseEntity<JsonNode> invRes = restTemplate.postForEntity(baseUrl + "/invoices", new HttpEntity<>(invReq, jsonHeaders()), JsonNode.class);

        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode taxRollup = invRes.getBody().get("tax_rollup");
        assertThat(taxRollup.isArray()).isTrue();
        assertThat(taxRollup).hasSize(2);

        // Verify distinct rates are maintained
        assertThat(taxRollup.get(0).get("name").asText()).isEqualTo("VAT");
        assertThat(taxRollup.get(0).get("rate").asDouble()).isEqualTo(0.07);
        assertThat(taxRollup.get(0).get("amount").asDouble()).isEqualTo(7.00);

        assertThat(taxRollup.get(1).get("name").asText()).isEqualTo("VAT");
        assertThat(taxRollup.get(1).get("rate").asDouble()).isEqualTo(0.19);
        assertThat(taxRollup.get(1).get("amount").asDouble()).isEqualTo(19.00);
    }

    // =========================================================================
    // 5. Invoice Update (PATCH), Deletion (DELETE), and Unfreezing Verification
    // =========================================================================

    @Test
    @DisplayName("Invoice PATCH replaces transaction set atomically; DELETE invoice frees transactions to be edited")
    void testInvoicePatchDeleteAndUnfreezing() {
        // 1. Create two transactions
        String ext1 = randomExternalId("unfreeze-1");
        Map<String, Object> p1 = Map.of(
                "type", "navan_fee",
                "external_id", ext1,
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 25.00,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("fee_kind", "trip_fee")
        );
        String id1 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(p1, jsonHeaders()), JsonNode.class).getBody().get("id").asText();

        String ext2 = randomExternalId("unfreeze-2");
        Map<String, Object> p2 = Map.of(
                "type", "navan_fee",
                "external_id", ext2,
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 15.00,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("fee_kind", "agent_call_fee")
        );
        String id2 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(p2, jsonHeaders()), JsonNode.class).getBody().get("id").asText();

        // 2. Create invoice with id1 only
        ResponseEntity<JsonNode> invCreate = restTemplate.postForEntity(
                baseUrl + "/invoices",
                new HttpEntity<>(Map.of("transaction_id", id1), jsonHeaders()),
                JsonNode.class
        );
        String invId = invCreate.getBody().get("id").asText();
        assertThat(invCreate.getBody().get("grand_total").asDouble()).isEqualTo(25.00);

        // 3. Invoiced transaction id1 cannot be PATCHed
        ResponseEntity<JsonNode> blockedPatch = restTemplate.exchange(
                baseUrl + "/transactions/" + id1,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("total", 99.00), jsonHeaders()),
                JsonNode.class
        );
        assertThat(blockedPatch.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 4. PATCH invoice to replace set with [id1, id2]
        ResponseEntity<JsonNode> invPatch = restTemplate.exchange(
                baseUrl + "/invoices/" + invId,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("transaction_ids", List.of(id1, id2)), jsonHeaders()),
                JsonNode.class
        );
        assertThat(invPatch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(invPatch.getBody().get("grand_total").asDouble()).isEqualTo(40.00);
        assertThat(invPatch.getBody().get("transactions")).hasSize(2);

        // 5. DELETE the invoice
        ResponseEntity<Void> invDel = restTemplate.exchange(
                baseUrl + "/invoices/" + invId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(invDel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify invoice is gone (404)
        ResponseEntity<JsonNode> getInvAfterDel = restTemplate.getForEntity(baseUrl + "/invoices/" + invId, JsonNode.class);
        assertThat(getInvAfterDel.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 6. UNFREEZING VERIFICATION:
        // Now that the invoice is deleted, id1 is NO LONGER invoiced and can now be modified and deleted!
        ResponseEntity<JsonNode> unfreezePatch = restTemplate.exchange(
                baseUrl + "/transactions/" + id1,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("total", 99.00), jsonHeaders()),
                JsonNode.class
        );
        assertThat(unfreezePatch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unfreezePatch.getBody().get("total").asDouble()).isEqualTo(99.00);

        // And it can now be cleanly deleted!
        ResponseEntity<Void> unfreezeDel = restTemplate.exchange(
                baseUrl + "/transactions/" + id1,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(unfreezeDel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // =========================================================================
    // 6. Pagination Boundaries for Invoices & Transactions
    // =========================================================================

    @Test
    @DisplayName("Pagination bounds: limit=100 succeeds, limit=101 returns 400 Bad Request on /invoices")
    void testInvoicePaginationBounds() {
        ResponseEntity<JsonNode> validBoundary = restTemplate.getForEntity(baseUrl + "/invoices?limit=100&offset=0", JsonNode.class);
        assertThat(validBoundary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validBoundary.getBody().get("limit").asInt()).isEqualTo(100);

        ResponseEntity<JsonNode> invalidBoundary = restTemplate.getForEntity(baseUrl + "/invoices?limit=101&offset=0", JsonNode.class);
        assertThat(invalidBoundary.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Pagination bounds: limit=100 succeeds, limit=101 returns 400 Bad Request on /transactions")
    void testTransactionPaginationBounds() {
        ResponseEntity<JsonNode> validBoundary = restTemplate.getForEntity(baseUrl + "/transactions?limit=100&offset=0", JsonNode.class);
        assertThat(validBoundary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validBoundary.getBody().get("limit").asInt()).isEqualTo(100);

        ResponseEntity<JsonNode> invalidBoundary = restTemplate.getForEntity(baseUrl + "/transactions?limit=101&offset=0", JsonNode.class);
        assertThat(invalidBoundary.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 7. Financial Precision & HALF_UP Scaling
    // =========================================================================

    @Test
    @DisplayName("Financial precision: 3-decimal inputs are strictly rounded to scale 2 using HALF_UP")
    void testFinancialPrecisionAndHalfUpScaling() {
        String extId = randomExternalId("precision-test");
        Map<String, Object> payload = Map.of(
                "type", "flight",
                "external_id", extId,
                "currency", "EUR",
                "occurred_at", "2026-03-11T10:00:00Z",
                "total", 100.555, // Should round to 100.56
                "tax_lines", List.of(Map.of("name", "Tax", "amount", 10.124)), // Should round to 10.12
                "fee_lines", List.of(Map.of("name", "Fee", "amount", 5.678)), // Should round to 5.68
                "metadata", Map.of(
                        "origin", "BER",
                        "destination", "FRA",
                        "fare", 84.753,
                        "coupons", List.of(
                                Map.of(
                                        "flight_number", "LH1",
                                        "from", "BER",
                                        "to", "FRA",
                                        "departure", "2026-03-11T10:00:00Z",
                                        "arrival", "2026-03-11T11:00:00Z"
                                )
                        )
                )
        );

        ResponseEntity<JsonNode> res = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(payload, jsonHeaders()), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = res.getBody();
        assertThat(body.get("total").asDouble()).isEqualTo(100.56);
        assertThat(body.get("tax_lines").get(0).get("amount").asDouble()).isEqualTo(10.12);
        assertThat(body.get("fee_lines").get(0).get("amount").asDouble()).isEqualTo(5.68);
    }

    @Test
    @DisplayName("Arbitrary non-multiple offset pagination (e.g. offset=5, limit=4) returns exact item slice")
    void testArbitraryOffsetPaginationPrecision() {
        // Create 12 transactions with identifiable external IDs
        for (int i = 0; i < 12; i++) {
            Map<String, Object> payload = Map.of(
                    "type", "rail",
                    "external_id", randomExternalId("page-slice-" + i),
                    "currency", "EUR",
                    "occurred_at", "2026-03-12T10:00:00Z",
                    "total", 50.00,
                    "tax_lines", List.of(),
                    "fee_lines", List.of(),
                    "metadata", Map.of(
                            "operator", "Eurostar",
                            "legs", List.of(
                                    Map.of("origin", "London", "destination", "Paris", "departure", "2026-03-12T10:00:00Z", "arrival", "2026-03-12T12:00:00Z")
                            )
                    )
            );
            ResponseEntity<JsonNode> postRes = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(payload, jsonHeaders()), JsonNode.class);
            assertThat(postRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Fetch full baseline (all transactions currently in DB)
        ResponseEntity<JsonNode> fullRes = restTemplate.getForEntity(baseUrl + "/transactions?offset=0&limit=100", JsonNode.class);
        assertThat(fullRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode allItems = fullRes.getBody().get("items");
        assertThat(allItems.size()).isGreaterThanOrEqualTo(12);

        // Fetch slice offset=5, limit=4
        ResponseEntity<JsonNode> sliceRes = restTemplate.getForEntity(baseUrl + "/transactions?offset=5&limit=4", JsonNode.class);
        assertThat(sliceRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode sliceItems = sliceRes.getBody().get("items");
        assertThat(sliceItems.size()).isEqualTo(4);

        // Verify that the 4 items match baseline items at indices 5, 6, 7, 8 exactly
        for (int i = 0; i < 4; i++) {
            String expectedId = allItems.get(5 + i).get("id").asText();
            String actualId = sliceItems.get(i).get("id").asText();
            assertThat(actualId).as("Mismatch at slice index " + i).isEqualTo(expectedId);
        }

        // Fetch another slice: offset=7, limit=3
        ResponseEntity<JsonNode> slice2Res = restTemplate.getForEntity(baseUrl + "/transactions?offset=7&limit=3", JsonNode.class);
        assertThat(slice2Res.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode slice2Items = slice2Res.getBody().get("items");
        assertThat(slice2Items.size()).isEqualTo(3);

        for (int i = 0; i < 3; i++) {
            String expectedId = allItems.get(7 + i).get("id").asText();
            String actualId = slice2Items.get(i).get("id").asText();
            assertThat(actualId).as("Mismatch at slice 2 index " + i).isEqualTo(expectedId);
        }
    }

    @Test
    @DisplayName("Concurrent PATCH on same invoice acquires row lock and eliminates snapshot tearing")
    void testConcurrentPatchInvoiceRaceWithRowLock() throws Exception {
        // Create 4 distinct transactions
        String[] txnIds = new String[4];
        for (int i = 0; i < 4; i++) {
            Map<String, Object> payload = Map.of(
                    "type", "navan_fee",
                    "external_id", randomExternalId("concurrent-inv-patch-" + i),
                    "currency", "EUR",
                    "occurred_at", "2026-03-12T10:00:00Z",
                    "total", 25.00 * (i + 1),
                    "tax_lines", List.of(),
                    "fee_lines", List.of(),
                    "metadata", Map.of("fee_kind", "trip_fee")
            );
            ResponseEntity<JsonNode> res = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(payload, jsonHeaders()), JsonNode.class);
            txnIds[i] = res.getBody().get("id").asText();
        }

        // Create initial invoice with [txnIds[0], txnIds[1]]
        Map<String, Object> createInv = Map.of("transaction_ids", List.of(txnIds[0], txnIds[1]));
        ResponseEntity<JsonNode> invRes = restTemplate.postForEntity(baseUrl + "/invoices", new HttpEntity<>(createInv, jsonHeaders()), JsonNode.class);
        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String invoiceId = invRes.getBody().get("id").asText();

        // 10 concurrent threads racing to patch the invoice with alternating sets
        int numThreads = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(numThreads);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        List<List<String>> sets = List.of(
                List.of(txnIds[0], txnIds[2]), // Set A: 25 + 75 = 100.00
                List.of(txnIds[1], txnIds[3])  // Set B: 50 + 100 = 150.00
        );

        for (int i = 0; i < numThreads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    List<String> targetSet = sets.get(threadIdx % 2);
                    Map<String, Object> patchPayload = Map.of("transaction_ids", targetSet);
                    HttpHeaders headers = jsonHeaders();
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(patchPayload, headers);
                    ResponseEntity<JsonNode> patchRes = restTemplate.exchange(
                            baseUrl + "/invoices/" + invoiceId,
                            HttpMethod.PATCH,
                            entity,
                            JsonNode.class
                    );
                    if (patchRes.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(numThreads);

        // Verify final state of invoice has no torn snapshots
        ResponseEntity<JsonNode> finalInvRes = restTemplate.getForEntity(baseUrl + "/invoices/" + invoiceId, JsonNode.class);
        assertThat(finalInvRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode finalBody = finalInvRes.getBody();

        // Snapshots must be exactly 2 (never torn or accumulated)
        assertThat(finalBody.get("transactions").size()).isEqualTo(2);

        // Grand total must match exactly one of the valid sets (100.00 or 150.00)
        double grandTotal = finalBody.get("grand_total").asDouble();
        assertThat(grandTotal).isIn(100.00, 150.00);
    }
}
