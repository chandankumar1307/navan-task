# Navan Commerce: Travel Inventory & Invoicing Service

A high-performance Java and Spring Boot microservice for ingesting multi-modal travel transactions (`flight`, `hotel`, `rail`, `navan_fee`) and generating immutable invoice snapshots with automated tax and fee rollups.

---

## 1. Quick Start

### Prerequisites
- **Java 21 or Java 25** (`java -version`)
- *(Optional)* Docker and Docker Compose (only if running the PostgreSQL staging profile)

### Run Tests in One Command
```bash
./mvnw test
```
*Executes all 12 automated integration tests covering the 10 mandatory evaluation criteria, including the multithreaded concurrent race test.*

### Run the API in One Command (Default: File-Backed Database)
```bash
./mvnw spring-boot:run
```
*Starts the service on `http://localhost:8080` backed by a persistent SQL database on disk (`./data/navan_db.mv.db`). No external software or Docker daemon required.*

### (Optional) Run with Docker PostgreSQL 16
```bash
# 1. Start PostgreSQL 16
docker compose up -d

# 2. Run application with the stage profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=stage
```

### Run Automated Live Demonstration Script
With the application running on `http://localhost:8080`, execute the end-to-end curl demonstration script in another terminal:
```bash
./scripts/demo.sh
```
*Executes all required curl scenarios sequentially with color-coded verification.*

### Interactive Swagger UI & OpenAPI 3.0 Documentation
Once the service is started, access the complete interactive API specification and UI in your browser:
- **Swagger UI Interactive Console:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Raw OpenAPI 3.0 JSON Specification:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

*Contains detailed schemas, field-level constraints, request/response models, and live "Try it out" capabilities for all multi-modal transaction and invoice endpoints.*

---

## 2. API Documentation & Example Curls

### 2.1 Health Check
```bash
curl -s http://localhost:8080/health
```
**Response (`200 OK`):**
```json
{"status":"UP"}
```

---

### 2.2 Create Transactions (`POST /transactions`)

#### Flight Transaction
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.0,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.0 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.0 }],
    "metadata": {
      "origin": "TXL",
      "destination": "LHR",
      "fare": 180.0,
      "coupons": [
        {
          "carrier": "BA",
          "flight_number": "BA982",
          "from": "TXL",
          "to": "LHR",
          "departure": "2026-03-11T07:00:00Z",
          "arrival": "2026-03-11T08:20:00Z"
        }
      ]
    }
  }'
```

#### Hotel Transaction
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "hotel",
    "external_id": "hotel-LON-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T15:00:00Z",
    "total": 331.7,
    "tax_lines": [{ "name": "VAT", "rate": 0.07, "amount": 21.7 }],
    "fee_lines": [],
    "metadata": {
      "name": "Hotel Mitte London",
      "address": {
        "street": "12 Kingsway",
        "city": "London",
        "country": "GB"
      },
      "check_in": "2026-03-11",
      "check_out": "2026-03-13",
      "nights": 2,
      "room_rate_per_night": 140.0,
      "extras": [{ "name": "Breakfast", "amount": 30.0 }]
    }
  }'
```

#### Rail Transaction
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "rail",
    "external_id": "rail-STP-PAR-2026-03-13",
    "currency": "EUR",
    "occurred_at": "2026-03-13T09:00:00Z",
    "total": 94.0,
    "tax_lines": [],
    "fee_lines": [{ "name": "Booking fee", "amount": 4.0 }],
    "metadata": {
      "legs": [
        {
          "origin": "London St Pancras",
          "destination": "Lille Europe",
          "departure": "2026-03-13T09:00:00Z",
          "arrival": "2026-03-13T10:30:00Z",
          "train_number": "EST9108",
          "class": "standard"
        },
        {
          "origin": "Lille Europe",
          "destination": "Paris Nord",
          "departure": "2026-03-13T11:00:00Z",
          "arrival": "2026-03-13T12:05:00Z",
          "train_number": "EST9122",
          "class": "standard"
        }
      ]
    }
  }'
```

#### Navan Fee: Trip Fee
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "navan_fee",
    "external_id": "navan-trip-fee-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 25.0,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "fee_kind": "trip_fee",
      "description": "Navan trip fee",
      "related_external_id": "flight-TXL-LHR-2026-03-11"
    }
  }'
```

#### Navan Fee: Agent Call Fee
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "navan_fee",
    "external_id": "navan-agent-call-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T18:00:00Z",
    "total": 15.0,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "fee_kind": "agent_call_fee",
      "description": "Agent fee for support call",
      "related_external_id": "hotel-LON-2026-03-11"
    }
  }'
```

---

### 2.3 Duplicate Create Rejection (`409 Conflict`)
Post the exact same body twice with identical `external_id`:
```bash
curl -s -i -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.0,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.0 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.0 }],
    "metadata": {
      "origin": "TXL",
      "destination": "LHR",
      "fare": 180.0,
      "coupons": []
    }
  }'
```
**Response (`409 Conflict`):**
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Transaction with external_id 'flight-TXL-LHR-2026-03-11' already exists.",
  "id": "txn_6c97a7d42cf34d98947ab59bb4dfdb19"
}
```
*Note: The response status is 409 Conflict, returns the existing server `id`, and inserts zero duplicate rows.*

---

### 2.4 Read & List Transactions

#### Get Transaction by ID
```bash
curl -s http://localhost:8080/transactions/txn_6c97a7d42cf34d98947ab59bb4dfdb19
```

#### Paginated List
```bash
curl -s "http://localhost:8080/transactions?limit=2&offset=0"
```
**Response:**
```json
{
  "items": [ /* transactions */ ],
  "limit": 2,
  "offset": 0,
  "total_count": 5
}
```

---

### 2.5 Invoices

#### Create Trip Invoice
```bash
curl -s -X POST http://localhost:8080/invoices \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_ids": [
      "<flight_id>",
      "<hotel_id>",
      "<rail_id>",
      "<trip_fee_id>",
      "<agent_call_fee_id>"
    ]
  }'
```
**Response (`201 Created` matching `expected-invoice.json`):**
```json
{
  "id": "inv_80d79ceeb92e4abfae7813a824479532",
  "currency": "EUR",
  "grand_total": 685.70,
  "totals": {
    "flight": 220.00,
    "hotel": 331.70,
    "rail": 94.00,
    "trip_fee": 25.00,
    "agent_call_fee": 15.00,
    "grand_total": 685.70
  },
  "tax_rollup": [
    { "name": "Airport tax", "amount": 15.00 },
    { "name": "VAT", "rate": 0.07, "amount": 21.70 }
  ],
  "fee_rollup": [
    { "name": "YQ", "amount": 25.00 },
    { "name": "Booking fee", "amount": 4.00 }
  ],
  "transactions": [ /* snapshot objects */ ],
  "created_at": "2026-10-25T15:16:09Z"
}
```

#### Read Invoice by ID
```bash
curl -s http://localhost:8080/invoices/inv_80d79ceeb92e4abfae7813a824479532
```

#### List Invoices
```bash
curl -s "http://localhost:8080/invoices?limit=10&offset=0"
```

---

### 2.6 Mutation Protection on Invoiced Transactions (`409 Conflict`)

#### Attempt PATCH on Invoiced Transaction
```bash
curl -s -i -X PATCH http://localhost:8080/transactions/<flight_id> \
  -H "Content-Type: application/json" \
  -d '{ "total": 999.00 }'
```
**Response:** `409 Conflict` (Persists nothing; preserves frozen invoice snapshot).

#### Attempt DELETE on Invoiced Transaction
```bash
curl -s -i -X DELETE http://localhost:8080/transactions/<flight_id>
```
**Response:** `409 Conflict` (Deletes nothing; preserves frozen invoice snapshot).

---

### 2.7 Invoice Rebuild & Deletion

#### Replace Invoice Transaction Set (`PATCH /invoices/{id}`)
```bash
curl -s -X PATCH http://localhost:8080/invoices/<invoice_id> \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_ids": ["<flight_id>", "<hotel_id>"]
  }'
```
*Rebuilds snapshot atomically from current transactions.*

#### Delete Invoice (`DELETE /invoices/{id}`)
```bash
curl -s -i -X DELETE http://localhost:8080/invoices/<invoice_id>
```
*Deletes invoice and snapshot records. Underlying transactions remain intact and retrievable via `GET /transactions/{id}`.*

---

## 3. Automated Test Suite Matrix

Run all tests in one command:
```bash
./mvnw test
```
*Executes all 113 automated tests (unit + integration + OpenAPI/Swagger contract validation) covering all primary criteria, isolated class logic, and rigorous edge cases in ~4.5 seconds.*

### 3.1 Primary Integration Suite (`TravelApiIntegrationTest` — 12 Tests)
| Test Name | Brief Criterion | Verified Invariant |
| :--- | :--- | :--- |
| `test1_createAndRetrieveAllSeedTypes` | Req #1 | Flight coupons, hotel address, rail legs, and fee kinds round-trip completely without schema loss. |
| `test2_duplicateExternalIdRejection` | Req #2 | Duplicate POST returns 409 Conflict with existing ID. Database has exactly 1 row. |
| `test3_createTripInvoice` | Req #3 | Trip invoice matches `expected-invoice.json` (grand total 685.70 EUR, exact tax & fee rollups). |
| `test4_invoiceSnapshotImmutability` | Req #4 | Changing flight total in DB leaves existing invoice snapshot intact at 220.00 EUR. |
| `test5_mixedCurrencyInvoiceRejection` | Req #5 | Invoicing EUR and USD transactions together returns 409 Conflict. |
| `test6_concurrentDuplicateCreateRace` | Req #6 | 10 concurrent threads with same `external_id` yield 1 success (201) and 9 conflicts (409). 1 row in DB. |
| `test7_patchInvoicedTransactionReturns409` | Req #7 | PATCH on invoiced transaction returns 409 Conflict; totals unaltered. |
| `test8_deleteInvoicedTransactionReturns409` | Req #8 | DELETE on invoiced transaction returns 409 Conflict; transaction remains stored. |
| `test9_deleteInvoiceLeavesTransactionsIntact` | Req #9 | DELETE invoice removes invoice; original transactions still return 200 OK. |
| `test10_paginationBounds` | Req #10 | Default pagination returns 50; `limit=1000` is rejected with 400 Bad Request. |
| `test11_unknownTypeAndFeeKind400` | Edge | Unknown transaction type or fee_kind returns 400 Bad Request. |
| `test12_healthCheck` | Operational | `GET /health` returns 200 UP. |

### 3.2 Edge Cases & Lifecycle Suite (`TravelApiEdgeCasesIntegrationTest` & `SwaggerApiIntegrationTest` — 19 Tests)
- **OpenAPI 3.0 & Swagger UI Verification:** Validates `GET /v3/api-docs` returns complete JSON schemas for all request/response models and endpoints; validates `GET /swagger-ui/index.html` loads the Swagger UI console (200 OK).
- **Validation Edge Cases:** Missing required top-level fields (type, external_id, currency, occurred_at, total, tax_lines, fee_lines, metadata) returning `400`.
- **Empty / Dummy Metadata:** Empty `{}` metadata, cross-type metadata mismatch (e.g., flight with hotel payload) returning `400`.
- **Modality Specifics:** Flight missing coupons or coupon missing flight number; Hotel missing address or 0 nights; Rail empty legs; Navan fee missing/invalid fee kind.
- **Resource Not Found:** `GET /transactions/{non_existent_id}` returning `404`.
- **Mutable Non-Invoiced Lifecycle:** Non-invoiced transaction `PATCH` updating totals, dates, tax/fee lines, and metadata; subsequent atomic `DELETE` returning `204`, with following `GET` returning `404`.
- **Invoice Request Shapes & Duplicates:** Single `transaction_id` format; duplicate IDs in `transaction_ids` returning `409`; non-existent transaction returning `404`; empty request body returning `400`.
- **Multi-Rate Tax Rollups:** Invoicing transactions with VAT 7% and VAT 19% simultaneously confirms distinct aggregation into separate rollup line items with exact mathematical precision.
- **Invoice Unfreezing Invariant:** When an invoice is deleted, its previously invoiced transactions are unfrozen and can now be modified (`PATCH`) or deleted (`DELETE`).
- **Pagination Boundary Limits:** `limit=100` succeeds (200 OK); `limit=101` rejected (400 Bad Request) on both `/transactions` and `/invoices`.
- **Financial Precision:** Ingestion of 3-decimal values (e.g. `100.555`, `10.124`, `5.678`) verified to strictly scale to 2 decimal places with `HALF_UP` (`100.56`, `10.12`, `5.68`).

### 3.3 Isolated Class-Level Unit Test Suites (82 Tests)
- `FlightTransactionHandlerTest`: 7 isolated tests validating coupons, flight numbers, serialization, and subtotal categories.
- `HotelTransactionHandlerTest`: 6 isolated tests validating address fields, nights, room rates, extras, and serialization.
- `RailTransactionHandlerTest`: 5 isolated tests validating multi-leg journeys, empty legs, and serialization.
- `NavanFeeTransactionHandlerTest`: 6 isolated tests validating `trip_fee`, `agent_call_fee`, and subtotal categories.
- `TransactionHandlerRegistryTest`: 2 tests verifying registry routing and null type rejection.
- `TaxRollupAggregatorTest`: 5 isolated tests for multi-rate taxes, null rates, rate normalization (e.g. `0.0700` vs `0.07`), and half-up rounding.
- `FeeRollupAggregatorTest`: 4 isolated tests for fee aggregation by name, multiple lines, and precision rounding.
- `CreateInvoiceRequestTest`: 6 isolated tests for single vs multiple transaction ID resolution, duplicates rejection (409), and empty request rejection (400).
- `TransactionServiceTest`: 8 isolated unit tests with Mockito for fast-path duplicate collision, mutation blocking, pagination, and atomic deletion.
- `InvoiceServiceTest`: 6 isolated unit tests with Mockito for missing transactions (404), mixed currency conflict (409), snapshot creation, and cascade deletion.
- `InvoiceSnapshotEngineTest`: 2 isolated tests for deep copying transaction, tax, and fee entities into Memento snapshots.
- `TransactionTypeTest` & `NavanFeeKindTest`: 18 tests for enum serialization, case-insensitive parsing, and unknown value validation.
- `GlobalExceptionHandlerTest`: 7 tests for RFC 7807 problem details mapping and conflict response structure with root `id`.
