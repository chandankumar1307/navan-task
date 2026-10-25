package com.navan.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.repository.InvoiceRepository;
import com.navan.travel.domain.repository.InvoiceTransactionSnapshotRepository;
import com.navan.travel.domain.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TravelApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceTransactionSnapshotRepository invoiceSnapshotRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode seeds;
    private String baseUrl;
    private static String tripInvoiceId;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port;
        // Load fixtures
        File fixtureFile = new File("fixtures/task-b/seed-payloads.json");
        if (fixtureFile.exists()) {
            seeds = objectMapper.readTree(fixtureFile);
        } else {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("fixtures/task-b/seed-payloads.json")) {
                seeds = objectMapper.readTree(in);
            }
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // =========================================================================
    // TEST 1: Create flight, hotel, rail, and two navan_fee kinds (trip_fee, agent_call_fee)
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("1. Create and GET all 5 transaction types with rich metadata")
    void test1_createAndRetrieveAllSeedTypes() throws Exception {
        // 1. Flight
        ResponseEntity<JsonNode> flightRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(seeds.get("flight"), jsonHeaders()),
                JsonNode.class
        );
        assertThat(flightRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String flightId = flightRes.getBody().get("id").asText();
        assertThat(flightId).startsWith("txn_");

        // Verify Flight GET
        ResponseEntity<JsonNode> getFlight = restTemplate.getForEntity(baseUrl + "/transactions/" + flightId, JsonNode.class);
        assertThat(getFlight.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode flightMeta = getFlight.getBody().get("metadata");
        assertThat(flightMeta.get("origin").asText()).isEqualTo("TXL");
        assertThat(flightMeta.get("destination").asText()).isEqualTo("LHR");
        assertThat(flightMeta.get("coupons").isArray()).isTrue();
        assertThat(flightMeta.get("coupons").size()).isGreaterThanOrEqualTo(1);
        assertThat(flightMeta.get("coupons").get(0).get("flight_number").asText()).isEqualTo("BA982");

        // 2. Hotel
        ResponseEntity<JsonNode> hotelRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(seeds.get("hotel"), jsonHeaders()),
                JsonNode.class
        );
        assertThat(hotelRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String hotelId = hotelRes.getBody().get("id").asText();

        // Verify Hotel GET
        ResponseEntity<JsonNode> getHotel = restTemplate.getForEntity(baseUrl + "/transactions/" + hotelId, JsonNode.class);
        assertThat(getHotel.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode hotelMeta = getHotel.getBody().get("metadata");
        assertThat(hotelMeta.get("name").asText()).isEqualTo("Hotel Mitte London");
        assertThat(hotelMeta.get("address").get("street").asText()).isEqualTo("12 Kingsway");
        assertThat(hotelMeta.get("address").get("city").asText()).isEqualTo("London");
        assertThat(hotelMeta.get("address").get("country").asText()).isEqualTo("GB");
        assertThat(hotelMeta.get("nights").asInt()).isEqualTo(2);

        // 3. Rail
        ResponseEntity<JsonNode> railRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(seeds.get("rail"), jsonHeaders()),
                JsonNode.class
        );
        assertThat(railRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String railId = railRes.getBody().get("id").asText();

        // Verify Rail GET
        ResponseEntity<JsonNode> getRail = restTemplate.getForEntity(baseUrl + "/transactions/" + railId, JsonNode.class);
        assertThat(getRail.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode railMeta = getRail.getBody().get("metadata");
        assertThat(railMeta.get("legs").isArray()).isTrue();
        assertThat(railMeta.get("legs").size()).isEqualTo(2);
        assertThat(railMeta.get("legs").get(0).get("origin").asText()).isEqualTo("London St Pancras");
        assertThat(railMeta.get("legs").get(1).get("destination").asText()).isEqualTo("Paris Nord");

        // 4. Navan Fee: trip_fee
        ResponseEntity<JsonNode> tripFeeRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(seeds.get("trip_fee"), jsonHeaders()),
                JsonNode.class
        );
        assertThat(tripFeeRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tripFeeId = tripFeeRes.getBody().get("id").asText();

        ResponseEntity<JsonNode> getTripFee = restTemplate.getForEntity(baseUrl + "/transactions/" + tripFeeId, JsonNode.class);
        assertThat(getTripFee.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getTripFee.getBody().get("metadata").get("fee_kind").asText()).isEqualTo("trip_fee");

        // 5. Navan Fee: agent_call_fee
        ResponseEntity<JsonNode> agentFeeRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(seeds.get("agent_call_fee"), jsonHeaders()),
                JsonNode.class
        );
        assertThat(agentFeeRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String agentFeeId = agentFeeRes.getBody().get("id").asText();

        ResponseEntity<JsonNode> getAgentFee = restTemplate.getForEntity(baseUrl + "/transactions/" + agentFeeId, JsonNode.class);
        assertThat(getAgentFee.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getAgentFee.getBody().get("metadata").get("fee_kind").asText()).isEqualTo("agent_call_fee");
    }

    // =========================================================================
    // TEST 2: Duplicate external_id rejection with existing id and 409
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("2. Duplicate external_id returns 409 Conflict with existing id")
    void test2_duplicateExternalIdRejection() {
        // Attempt posting duplicate_flight from seeds (external_id = flight-TXL-LHR-2026-03-11)
        ResponseEntity<JsonNode> duplicateRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(seeds.get("duplicate_flight"), jsonHeaders()),
                JsonNode.class
        );

        assertThat(duplicateRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateRes.getBody().get("status").asInt()).isEqualTo(409);
        assertThat(duplicateRes.getBody().has("id")).isTrue();
        assertThat(duplicateRes.getBody().get("id").asText()).startsWith("txn_");

        // Ensure database has exactly ONE row with this external_id
        long count = transactionRepository.findAll().stream()
                .filter(t -> "flight-TXL-LHR-2026-03-11".equals(t.getExternalId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    // =========================================================================
    // TEST 3: Invoice by transaction id(s) matching expected-invoice.json (685.70 EUR)
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("3. Create trip invoice combining flight + hotel + rail + navan fees (Grand Total 685.70 EUR)")
    void test3_createTripInvoice() {
        // Fetch IDs of the stored seed transactions
        TransactionEntity flight = transactionRepository.findByExternalId("flight-TXL-LHR-2026-03-11").orElseThrow();
        TransactionEntity hotel = transactionRepository.findByExternalId("hotel-LON-2026-03-11").orElseThrow();
        TransactionEntity rail = transactionRepository.findByExternalId("rail-STP-PAR-2026-03-13").orElseThrow();
        TransactionEntity tripFee = transactionRepository.findByExternalId("navan-trip-fee-2026-03-11").orElseThrow();
        TransactionEntity agentFee = transactionRepository.findByExternalId("navan-agent-call-2026-03-11").orElseThrow();

        Map<String, Object> invoiceReq = Map.of(
                "transaction_ids", List.of(
                        flight.getId(),
                        hotel.getId(),
                        rail.getId(),
                        tripFee.getId(),
                        agentFee.getId()
                )
        );

        ResponseEntity<JsonNode> invoiceRes = restTemplate.postForEntity(
                baseUrl + "/invoices",
                new HttpEntity<>(invoiceReq, jsonHeaders()),
                JsonNode.class
        );

        assertThat(invoiceRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode inv = invoiceRes.getBody();
        tripInvoiceId = inv.get("id").asText();
        assertThat(inv.get("currency").asText()).isEqualTo("EUR");
        assertThat(inv.get("grand_total").asDouble()).isEqualTo(685.70);

        // Subtotals check
        JsonNode totals = inv.get("totals");
        assertThat(totals.get("flight").asDouble()).isEqualTo(220.00);
        assertThat(totals.get("hotel").asDouble()).isEqualTo(331.70);
        assertThat(totals.get("rail").asDouble()).isEqualTo(94.00);
        assertThat(totals.get("trip_fee").asDouble()).isEqualTo(25.00);
        assertThat(totals.get("agent_call_fee").asDouble()).isEqualTo(15.00);
        assertThat(totals.get("grand_total").asDouble()).isEqualTo(685.70);

        // Tax Rollup check
        JsonNode taxRollup = inv.get("tax_rollup");
        assertThat(taxRollup.isArray()).isTrue();
        assertThat(taxRollup.size()).isEqualTo(2);

        // Fee Rollup check
        JsonNode feeRollup = inv.get("fee_rollup");
        assertThat(feeRollup.isArray()).isTrue();
        assertThat(feeRollup.size()).isEqualTo(2);

        // Snapshot line count
        assertThat(inv.get("transactions").size()).isEqualTo(5);
    }

    // =========================================================================
    // TEST 4: Snapshot immutability: changing stored flight total leaves invoice unchanged
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("4. Invoice snapshot immutability: mutating flight total in DB does not alter existing invoice")
    void test4_invoiceSnapshotImmutability() {
        TransactionEntity flight = transactionRepository.findByExternalId("flight-TXL-LHR-2026-03-11").orElseThrow();
        String invoiceId = tripInvoiceId != null ? tripInvoiceId : invoiceRepository.findAll().get(0).getId();

        try {
            // Mutate original flight in database directly (simulating any back-channel edit)
            flight.setTotal(new BigDecimal("999.00"));
            transactionRepository.saveAndFlush(flight);

            // GET invoice must STILL return original 220.00 for flight and 685.70 grand total
            ResponseEntity<JsonNode> getInv = restTemplate.getForEntity(baseUrl + "/invoices/" + invoiceId, JsonNode.class);
            assertThat(getInv.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode inv = getInv.getBody();

            assertThat(inv.get("totals").get("flight").asDouble()).isEqualTo(220.00);
            assertThat(inv.get("grand_total").asDouble()).isEqualTo(685.70);
        } finally {
            // Revert flight total for subsequent tests
            flight.setTotal(new BigDecimal("220.00"));
            transactionRepository.saveAndFlush(flight);
        }
    }

    // =========================================================================
    // TEST 5: Mixed currency invoice rejection (409 Conflict)
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("5. Mixed currency invoice creation returns 409 Conflict")
    void test5_mixedCurrencyInvoiceRejection() {
        TransactionEntity flightEur = transactionRepository.findByExternalId("flight-TXL-LHR-2026-03-11").orElseThrow();

        // Create a USD transaction
        Map<String, Object> usdTxn = Map.of(
                "type", "flight",
                "external_id", "flight-JFK-LAX-USD-2026",
                "currency", "USD",
                "occurred_at", "2026-03-11T12:00:00Z",
                "total", 350.00,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "origin", "JFK",
                        "destination", "LAX",
                        "fare", 350.00,
                        "coupons", List.of(
                                Map.of(
                                        "flight_number", "AA100",
                                        "from", "JFK",
                                        "to", "LAX",
                                        "departure", "2026-03-11T12:00:00Z",
                                        "arrival", "2026-03-11T15:30:00Z"
                                )
                        )
                )
        );

        ResponseEntity<JsonNode> usdRes = restTemplate.postForEntity(
                baseUrl + "/transactions",
                new HttpEntity<>(usdTxn, jsonHeaders()),
                JsonNode.class
        );
        assertThat(usdRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String usdId = usdRes.getBody().get("id").asText();

        // Attempt invoice combining EUR flight with USD flight
        Map<String, Object> mixedReq = Map.of(
                "transaction_ids", List.of(flightEur.getId(), usdId)
        );

        ResponseEntity<JsonNode> mixedRes = restTemplate.postForEntity(
                baseUrl + "/invoices",
                new HttpEntity<>(mixedReq, jsonHeaders()),
                JsonNode.class
        );

        assertThat(mixedRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mixedRes.getBody().get("status").asInt()).isEqualTo(409);
    }

    // =========================================================================
    // TEST 6: Concurrent duplicate create race condition
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("6. Concurrent duplicate create race condition results in exactly 1 success and 409 conflicts")
    void test6_concurrentDuplicateCreateRace() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        String raceExternalId = "race-test-booking-" + UUID.randomUUID();

        Map<String, Object> racePayload = Map.of(
                "type", "flight",
                "external_id", raceExternalId,
                "currency", "EUR",
                "occurred_at", "2026-04-01T08:00:00Z",
                "total", 150.00,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of(
                        "origin", "BER",
                        "destination", "MUC",
                        "fare", 150.00,
                        "coupons", List.of(
                                Map.of(
                                        "flight_number", "LH123",
                                        "from", "BER",
                                        "to", "MUC",
                                        "departure", "2026-04-01T08:00:00Z",
                                        "arrival", "2026-04-01T09:10:00Z"
                                )
                        )
                )
        );

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ResponseEntity<JsonNode> res = restTemplate.postForEntity(
                            baseUrl + "/transactions",
                            new HttpEntity<>(racePayload, jsonHeaders()),
                            JsonNode.class
                    );
                    if (res.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else if (res.getStatusCode() == HttpStatus.CONFLICT) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore client transport error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Unleash all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threads - 1);

        // Verify exactly one transaction exists in database
        long rows = transactionRepository.findAll().stream()
                .filter(t -> raceExternalId.equals(t.getExternalId()))
                .count();
        assertThat(rows).isEqualTo(1);
    }

    // =========================================================================
    // TEST 7: PATCH transaction after it is on an invoice returns 409
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("7. PATCH on an invoiced transaction returns 409 Conflict")
    void test7_patchInvoicedTransactionReturns409() {
        TransactionEntity flight = transactionRepository.findByExternalId("flight-TXL-LHR-2026-03-11").orElseThrow();

        Map<String, Object> patchPayload = Map.of(
                "total", 500.00
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patchPayload, jsonHeaders());
        ResponseEntity<JsonNode> res = restTemplate.exchange(
                baseUrl + "/transactions/" + flight.getId(),
                HttpMethod.PATCH,
                request,
                JsonNode.class
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().get("status").asInt()).isEqualTo(409);

        // Verify flight total is untouched
        TransactionEntity refreshed = transactionRepository.findById(flight.getId()).orElseThrow();
        assertThat(refreshed.getTotal().compareTo(new BigDecimal("220.00"))).isEqualTo(0);
    }

    // =========================================================================
    // TEST 8: DELETE transaction that is on an invoice returns 409
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("8. DELETE on an invoiced transaction returns 409 Conflict")
    void test8_deleteInvoicedTransactionReturns409() {
        TransactionEntity hotel = transactionRepository.findByExternalId("hotel-LON-2026-03-11").orElseThrow();

        ResponseEntity<JsonNode> res = restTemplate.exchange(
                baseUrl + "/transactions/" + hotel.getId(),
                HttpMethod.DELETE,
                null,
                JsonNode.class
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().get("status").asInt()).isEqualTo(409);

        // Verify transaction still exists
        assertThat(transactionRepository.existsById(hotel.getId())).isTrue();
    }

    // =========================================================================
    // TEST 9: DELETE invoice removes invoice; transactions still GET
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("9. DELETE invoice removes invoice; underlying transactions still GET")
    void test9_deleteInvoiceLeavesTransactionsIntact() {
        String invoiceId = tripInvoiceId != null ? tripInvoiceId : invoiceRepository.findAll().get(0).getId();

        // Delete invoice
        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                baseUrl + "/invoices/" + invoiceId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // GET invoice must be 404
        ResponseEntity<JsonNode> getInv = restTemplate.getForEntity(baseUrl + "/invoices/" + invoiceId, JsonNode.class);
        assertThat(getInv.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Underlying transactions must still exist and return 200 OK
        TransactionEntity flight = transactionRepository.findByExternalId("flight-TXL-LHR-2026-03-11").orElseThrow();
        ResponseEntity<JsonNode> getFlight = restTemplate.getForEntity(baseUrl + "/transactions/" + flight.getId(), JsonNode.class);
        assertThat(getFlight.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // =========================================================================
    // TEST 10: GET /transactions bounded pagination (default 50, limit=1000 is 400)
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("10. GET /transactions enforces pagination bounds; limit=1000 returns 400")
    void test10_paginationBounds() {
        // Default pagination
        ResponseEntity<JsonNode> defaultRes = restTemplate.getForEntity(baseUrl + "/transactions", JsonNode.class);
        assertThat(defaultRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(defaultRes.getBody().get("limit").asInt()).isEqualTo(50);
        assertThat(defaultRes.getBody().get("offset").asLong()).isEqualTo(0);

        // Excessive limit query
        ResponseEntity<JsonNode> excessiveRes = restTemplate.getForEntity(baseUrl + "/transactions?limit=1000", JsonNode.class);
        assertThat(excessiveRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(excessiveRes.getBody().get("status").asInt()).isEqualTo(400);

        // Bounded custom page
        ResponseEntity<JsonNode> customRes = restTemplate.getForEntity(baseUrl + "/transactions?limit=2&offset=0", JsonNode.class);
        assertThat(customRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customRes.getBody().get("items").size()).isEqualTo(2);
        assertThat(customRes.getBody().get("limit").asInt()).isEqualTo(2);
    }

    // =========================================================================
    // EXTRA TESTS: Validation & Health Check
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("11. Unknown transaction type or fee_kind returns 400 Bad Request")
    void test11_unknownTypeAndFeeKind400() {
        // Unknown type
        Map<String, Object> badType = Map.of(
                "type", "spaceship",
                "external_id", "space-1",
                "currency", "EUR",
                "occurred_at", "2026-01-01T00:00:00Z",
                "total", 100.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of()
        );
        ResponseEntity<JsonNode> res1 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(badType, jsonHeaders()), JsonNode.class);
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Unknown fee_kind
        Map<String, Object> badFee = Map.of(
                "type", "navan_fee",
                "external_id", "fee-unknown-1",
                "currency", "EUR",
                "occurred_at", "2026-01-01T00:00:00Z",
                "total", 25.0,
                "tax_lines", List.of(),
                "fee_lines", List.of(),
                "metadata", Map.of("fee_kind", "invalid_fee_kind")
        );
        ResponseEntity<JsonNode> res2 = restTemplate.postForEntity(baseUrl + "/transactions", new HttpEntity<>(badFee, jsonHeaders()), JsonNode.class);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(12)
    @DisplayName("12. Health check endpoint returns 200 UP")
    void test12_healthCheck() {
        ResponseEntity<JsonNode> health = restTemplate.getForEntity(baseUrl + "/health", JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody().get("status").asText()).isEqualTo("UP");
    }
}
