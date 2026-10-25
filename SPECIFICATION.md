# Technical Specification: Travel Inventory & Invoicing Service

**Service:** Flight, Hotel, Rail, Navan Fee Transactions and Invoice Service  
**Target Platform:** Java 25 / Spring Boot 3.4  
**Persistence Architecture:** Dual-Profile (File-backed SQL DB by default / Docker PostgreSQL on demand)  

---

## 1. System Overview & Core Invariants

This service provides an HTTP API for ingesting multi-modal corporate travel transactions (`flight`, `hotel`, `rail`, `navan_fee`) and generating frozen, immutable invoice snapshots with automated tax and fee rollups.

### Core Invariants
1. **Database-Enforced Uniqueness**: Duplicate `external_id` values are rejected at the database level. Overlapping concurrent submissions result in exactly one stored record; the duplicate returns `409 Conflict` containing the existing record ID.
2. **Immutable Invoice Snapshots**: Creating an invoice creates a detached snapshot of all included transactions (headers, tax lines, fee lines, metadata). Subsequent mutations to source transactions never alter an existing invoice.
3. **Billed Transaction Protection**: Any `PATCH` or `DELETE` attempt on a transaction that exists on any invoice returns `409 Conflict` and persists no changes.
4. **Zero-Drift Financial Precision**: All monetary calculations utilize `BigDecimal` with a fixed scale of 2 decimal places and `RoundingMode.HALF_UP`. Floating-point arithmetic is prohibited.
5. **Bounded Queries & Targeted Indexing**: All collection endpoints enforce strict pagination (default 50, maximum 100) using database-level bounds. Unbounded table dumps and in-memory pagination are prohibited.
6. **Dual-Profile Persistence Flexibility**:
   - **Default & `local` Profile**: Uses file-backed SQL storage (`./data/navan_db.mv.db`) so the service runs immediately out-of-the-box on `./mvnw test` and `./mvnw spring-boot:run` without requiring Docker or external services.
   - **`stage` Profile**: Uses Docker PostgreSQL 16 via a provided `docker-compose.yml` (`./mvnw spring-boot:run -Dspring-boot.run.profiles=stage`) for reviewers who wish to evaluate against PostgreSQL.

---

## 2. High-Level Design (HLD) & Architecture Patterns

### 2.1 Applied Design Patterns Architecture

To prevent monolithic "god classes" and satisfy the Open-Closed Principle (OCP), the architecture implements the following industry design patterns:

```
                               HTTP Requests
                                     │
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                      Web Controller Tier                    │
      │   (TransactionController, InvoiceController, HealthCtrl)   │
      │   - RFC 7807 Problem Details                                │
      │   - DTO Builders (Builder Pattern)                          │
      └──────────────────────────────┬──────────────────────────────┘
                                     │
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                     Service Business Tier                   │
      │   (TransactionService, InvoiceService)                      │
      │   - Coordinates workflows, short @Transactional boundaries  │
      └──────────────┬──────────────────────────────┬───────────────┘
                     │                              │
                     ▼                              ▼
      ┌──────────────────────────────┐┌──────────────────────────────┐
      │  TransactionHandlerRegistry  ││    InvoiceSnapshotEngine     │
      │  (Registry / Factory Pattern)││      (Memento Pattern)       │
      └──────────────┬───────────────┘└─────────────┬────────────────┘
                     │                              │
                     ▼                              ▼
      ┌──────────────────────────────┐┌──────────────────────────────┐
      │    TransactionTypeHandler    ││       RollupAggregators      │
      │      (Strategy Pattern)      ││   (Composite / Aggregator)   │
      │  - FlightTransactionHandler  ││   - TaxRollupAggregator      │
      │  - HotelTransactionHandler   ││   - FeeRollupAggregator      │
      │  - RailTransactionHandler    │└──────────────────────────────┘
      │  - NavanFeeTransactionHandler│
      └──────────────┬───────────────┘
                     │
                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                   Data Persistence Tier                     │
      │   (Spring Data JPA Repositories)                            │
      │   - TransactionRepository, InvoiceRepository                │
      │   - InvoiceTransactionSnapshotRepository                    │
      └──────────────────────────────┬──────────────────────────────┘
                                     │
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │               Dual-Profile Persistence Engine               │
      │   (ACID, MVCC, B-Tree Indexes, CHECK constraints)           │
      │   - Default / 'local': File-backed SQL (./data/navan_db)    │
      │   - Optional 'stage': Docker PostgreSQL 16 (Port 5432)      │
      └─────────────────────────────────────────────────────────────┘
```

### 2.2 Pattern Catalog & Responsibilities

| Design Pattern | Implementation Component | Architectural Benefit |
| :--- | :--- | :--- |
| **Strategy Pattern** | `TransactionTypeHandler<T>` (`FlightTransactionHandler`, `HotelTransactionHandler`, `RailTransactionHandler`, `NavanFeeTransactionHandler`) | Eliminates `switch`/`if-else` conditionals. Encapsulates type-specific validation, parsing, and subtotal categorization. Adding a new travel type requires only a new handler class without touching existing services. |
| **Factory / Registry Pattern** | `TransactionHandlerRegistry` | Automatically discovers all strategy handlers via Spring dependency injection and provides $O(1)$ lookup by `TransactionType` enum. |
| **Template Method Pattern** | `AbstractTransactionHandler<T>` | Factors out common validation (presence of required headers, positive total, non-empty collections) while deferring type-specific metadata validation (`coupons`, `address`, `legs`, `fee_kind`) to concrete subclasses. |
| **Memento Pattern** | `InvoiceSnapshotEngine` | Captures an immutable, deep-copied memento of the transaction state at invoice-creation time, ensuring subsequent changes to the originator never mutate historical invoices. |
| **Composite / Aggregator Pattern** | `TaxRollupAggregator`, `FeeRollupAggregator` | Encapsulates financial summation algorithms into standalone, pure domain services that are 100% unit-testable without database dependencies. |
| **Builder Pattern** | `TransactionResponse.Builder`, `InvoiceResponse.Builder` | Safely constructs complex immutable DTOs, eliminating parameter transposition errors across financial fields. |

### 2.3 Invoiced Transaction Lifecycle

```
                    ┌────────────────────────┐
                    │   POST /transactions   │
                    └───────────┬────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │   Transaction Stored   │
                    │   (State: UNINVOICED)  │
                    └─────┬────────────┬─────┘
                          │            │
       PATCH/DELETE /txn  │            │ POST /invoices
           (Allowed)      │            │ (Snapshots transaction)
                          ▼            ▼
                    ┌───────────┐┌────────────────────────┐
                    │  Updated/ ││  Transaction Invoiced  │
                    │  Deleted  ││  (State: INVOICED)     │
                    └───────────┘└─────┬────────────┬─────┘
                                       │            │
                    PATCH /transactions│            │ DELETE /transactions
                                       ▼            ▼
                                 ┌───────────┐┌───────────┐
                                 │    409    ││    409    │
                                 │  Conflict ││  Conflict │
                                 └───────────┘└───────────┘
```

---

## 3. API Contracts & Specifications

### 3.1 Transactions API

#### 3.1.1 Create Transaction
- **Method**: `POST`
- **Path**: `/transactions`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "type": "flight | hotel | rail | navan_fee",
  "external_id": "string (required, unique)",
  "currency": "string (ISO-4217, 3 chars, e.g. EUR)",
  "occurred_at": "string (ISO-8601, e.g. 2026-03-11T06:40:00Z)",
  "total": 220.00,
  "tax_lines": [
    { "name": "Airport tax", "rate": 0.07, "amount": 15.00 }
  ],
  "fee_lines": [
    { "name": "YQ", "amount": 25.00 }
  ],
  "metadata": { /* type-specific fields */ }
}
```

##### Metadata Requirements by Type:
1. **`flight`**:
   - `origin`: string (required)
   - `destination`: string (required)
   - `fare`: decimal (required)
   - `coupons`: array of objects (required, $\ge 1$ item):
     - `flight_number`: string (required)
     - `from`: string (required)
     - `to`: string (required)
     - `departure`: ISO-8601 string (required)
     - `arrival`: ISO-8601 string (required)
     - `carrier`: string (optional)
2. **`hotel`**:
   - `name`: string (required)
   - `address`: object (required):
     - `street`: string (required)
     - `city`: string (required)
     - `country`: string (required)
   - `check_in`: string / YYYY-MM-DD (required)
   - `check_out`: string / YYYY-MM-DD (required)
   - `nights`: integer (required, $\ge 1$)
   - `room_rate_per_night`: decimal (required)
   - `extras`: array of `{ "name": string, "amount": decimal }` (optional)
3. **`rail`**:
   - `legs`: array of objects (required, $\ge 1$ item; change of train = $\ge 2$ legs):
     - `origin`: string (required)
     - `destination`: string (required)
     - `departure`: ISO-8601 string (required)
     - `arrival`: ISO-8601 string (required)
     - `train_number`: string (optional)
     - `class`: string (optional)
4. **`navan_fee`**:
   - `fee_kind`: string (required, must be `trip_fee` or `agent_call_fee`)
   - `description`: string (optional)
   - `related_external_id`: string (optional)

- **Responses**:
  - `201 Created`: Returns stored transaction object including server `id`.
  - `400 Bad Request`: Validation failure, missing fields, unknown `type`, unknown `fee_kind`, or invalid metadata structure.
  - `409 Conflict`: `external_id` already exists. Returns existing transaction `id`:
    ```json
    {
      "type": "about:blank",
      "title": "Conflict",
      "status": 409,
      "detail": "Transaction with external_id 'flight-TXL-LHR-2026-03-11' already exists.",
      "id": "txn_01J..."
    }
    ```

#### 3.1.2 Read Transaction
- **Method**: `GET`
- **Path**: `/transactions/{id}`
- **Responses**:
  - `200 OK`: Stored transaction including `type`, `metadata`, `tax_lines`, `fee_lines`.
  - `404 Not Found`: ID does not exist.

#### 3.1.3 List Transactions
- **Method**: `GET`
- **Path**: `/transactions?limit=50&offset=0`
- **Query Parameters**:
  - `limit`: integer (optional, default `50`, maximum `100`). If $> 100$, returns `400 Bad Request`.
  - `offset`: integer (optional, default `0`, minimum `0`).
- **Responses**:
  - `200 OK`:
    ```json
    {
      "items": [ /* transactions */ ],
      "limit": 50,
      "offset": 0,
      "total_count": 120
    }
    ```

#### 3.1.4 Update Transaction
- **Method**: `PATCH`
- **Path**: `/transactions/{id}`
- **Body**: Partial or full update payload (`total`, `tax_lines`, `fee_lines`, `metadata`, `occurred_at`).
- **Responses**:
  - `200 OK`: Updated transaction.
  - `404 Not Found`: Transaction does not exist.
  - `409 Conflict`: Transaction is present on an invoice. No changes are written.

#### 3.1.5 Delete Transaction
- **Method**: `DELETE`
- **Path**: `/transactions/{id}`
- **Responses**:
  - `204 No Content`: Successfully deleted.
  - `404 Not Found`: Transaction does not exist.
  - `409 Conflict`: Transaction is present on an invoice. Record is not deleted.

---

### 3.2 Invoices API

#### 3.2.1 Create Invoice
- **Method**: `POST`
- **Path**: `/invoices`
- **Body**: Supports single transaction or multiple transactions:
  ```json
  { "transaction_id": "txn_123" }
  ```
  or
  ```json
  { "transaction_ids": ["txn_123", "txn_456"] }
  ```
- **Validation Rules**:
  - Missing transaction ID $\rightarrow$ `404 Not Found`.
  - Duplicate transaction IDs within request $\rightarrow$ `409 Conflict`.
  - Mixed currencies across transactions $\rightarrow$ `409 Conflict`.
- **Responses**:
  - `201 Created`: Stored invoice with snapshots and rollups.

#### 3.2.2 Read Invoice
- **Method**: `GET`
- **Path**: `/invoices/{id}`
- **Response Format (`200 OK`)**:
  ```json
  {
    "id": "inv_01J...",
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
    "transactions": [
      {
        "original_transaction_id": "txn_123",
        "type": "flight",
        "occurred_at": "2026-03-11T06:40:00Z",
        "total": 220.00,
        "tax_lines": [ { "name": "Airport tax", "amount": 15.00 } ],
        "fee_lines": [ { "name": "YQ", "amount": 25.00 } ],
        "metadata": { /* snapshotted metadata */ }
      }
    ],
    "created_at": "2026-03-11T19:00:00Z"
  }
  ```

#### 3.2.3 Update Invoice (Replace Transaction Set)
- **Method**: `PATCH`
- **Path**: `/invoices/{id}`
- **Body**: `{ "transaction_ids": ["txn_123", "txn_789"] }`
- **Behavior**: Atomically clears old snapshot lines and creates new snapshots from the current state of referenced transactions.
- **Responses**:
  - `200 OK`: Rebuilt invoice.
  - `404 Not Found`: Invoice or any transaction ID missing.
  - `409 Conflict`: Mixed currencies or duplicate IDs.

#### 3.2.4 Delete Invoice
- **Method**: `DELETE`
- **Path**: `/invoices/{id}`
- **Responses**:
  - `204 No Content`: Invoice and snapshot lines deleted.
  - `404 Not Found`: Invoice does not exist.
  - **Invariant**: Underlying source transactions are unaffected and remain intact.

#### 3.2.5 List Invoices
- **Method**: `GET`
- **Path**: `/invoices?limit=50&offset=0`
- **Query Parameters**: `limit` (default 50, max 100), `offset` (default 0).

---

## 4. Low-Level Design (LLD)

### 4.1 Domain Enums & Model Contracts

#### 4.1.1 `TransactionType` Enum
```java
public enum TransactionType {
    FLIGHT("flight"),
    HOTEL("hotel"),
    RAIL("rail"),
    NAVAN_FEE("navan_fee");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionType fromValue(String value) {
        for (TransactionType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new BadRequestException("Unknown transaction type: " + value);
    }
}
```

#### 4.1.2 `NavanFeeKind` Enum
```java
public enum NavanFeeKind {
    TRIP_FEE("trip_fee"),
    AGENT_CALL_FEE("agent_call_fee");

    private final String value;

    NavanFeeKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NavanFeeKind fromValue(String value) {
        for (NavanFeeKind kind : values()) {
            if (kind.value.equalsIgnoreCase(value)) {
                return kind;
            }
        }
        throw new BadRequestException("Unknown navan_fee fee_kind: " + value);
    }
}
```

### 4.2 Strategy & Factory Pattern Design

#### 4.2.1 `TransactionTypeHandler` Interface
```java
public interface TransactionTypeHandler<T extends TransactionMetadata> {
    TransactionType getType();
    Class<T> getMetadataClass();
    T parseAndValidateMetadata(JsonNode metadataNode);
    String serializeMetadata(T metadata);
    T deserializeMetadata(String metadataJson);
    String getSubtotalCategory(TransactionEntity transaction, T metadata);
}
```

#### 4.2.2 `TransactionHandlerRegistry`
```java
@Component
public class TransactionHandlerRegistry {
    private final Map<TransactionType, TransactionTypeHandler<?>> handlers = new EnumMap<>(TransactionType.class);

    public TransactionHandlerRegistry(List<TransactionTypeHandler<?>> handlerList) {
        for (TransactionTypeHandler<?> handler : handlerList) {
            handlers.put(handler.getType(), handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends TransactionMetadata> TransactionTypeHandler<T> getHandler(TransactionType type) {
        TransactionTypeHandler<?> handler = handlers.get(type);
        if (handler == null) {
            throw new BadRequestException("No handler registered for transaction type: " + type);
        }
        return (TransactionTypeHandler<T>) handler;
    }
}
```

#### 4.2.3 Subtotal Category Resolution
The `NavanFeeTransactionHandler` dynamically routes transaction totals to the correct subtotal category in the invoice:
```java
@Component
public class NavanFeeTransactionHandler extends AbstractTransactionHandler<NavanFeeMetadata> {
    @Override
    public TransactionType getType() {
        return TransactionType.NAVAN_FEE;
    }

    @Override
    public String getSubtotalCategory(TransactionEntity transaction, NavanFeeMetadata metadata) {
        return metadata.feeKind().getValue(); // returns "trip_fee" or "agent_call_fee"
    }
}
```

---

### 4.3 Relational Database Schema (DDL)

```sql
-- 1. Transactions Master Table
CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL CHECK (type IN ('flight', 'hotel', 'rail', 'navan_fee')),
    currency VARCHAR(3) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    metadata_json CLOB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_transactions_external_id UNIQUE (external_id)
);

CREATE INDEX idx_transactions_created_at ON transactions (created_at DESC, id DESC);
CREATE INDEX idx_transactions_type ON transactions (type);

-- 2. Transaction Tax Lines
CREATE TABLE transaction_tax_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    rate NUMERIC(6, 4),
    amount NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_tax_lines_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_tax_lines_txn_id ON transaction_tax_lines (transaction_id);

-- 3. Transaction Fee Lines
CREATE TABLE transaction_fee_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_fee_lines_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_fee_lines_txn_id ON transaction_fee_lines (transaction_id);

-- 4. Invoices Master Table
CREATE TABLE invoices (
    id VARCHAR(36) PRIMARY KEY,
    currency VARCHAR(3) NOT NULL,
    grand_total NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_invoices_created_at ON invoices (created_at DESC, id DESC);

-- 5. Invoice Transaction Snapshots (Immutable copy)
CREATE TABLE invoice_transaction_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    original_transaction_id VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL CHECK (transaction_type IN ('flight', 'hotel', 'rail', 'navan_fee')),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    metadata_json CLOB NOT NULL,
    CONSTRAINT fk_snapshot_invoice FOREIGN KEY (invoice_id) 
        REFERENCES invoices(id) ON DELETE CASCADE
);

-- Crucial index for O(1) "is this transaction on any invoice?" checks
CREATE INDEX idx_snapshot_orig_txn_id ON invoice_transaction_snapshots (original_transaction_id);
CREATE INDEX idx_snapshot_invoice_id ON invoice_transaction_snapshots (invoice_id);

-- 6. Invoice Tax Line Snapshots
CREATE TABLE invoice_tax_line_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    rate NUMERIC(6, 4),
    amount NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_tax_snapshot_entry FOREIGN KEY (snapshot_id) 
        REFERENCES invoice_transaction_snapshots(id) ON DELETE CASCADE
);

CREATE INDEX idx_tax_snapshot_entry_id ON invoice_tax_line_snapshots (snapshot_id);

-- 7. Invoice Fee Line Snapshots
CREATE TABLE invoice_fee_line_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_fee_snapshot_entry FOREIGN KEY (snapshot_id) 
        REFERENCES invoice_transaction_snapshots(id) ON DELETE CASCADE
);

CREATE INDEX idx_fee_snapshot_entry_id ON invoice_fee_line_snapshots (snapshot_id);
```

### 4.4 Class & Component Structure

```
com.navan.travel/
├── config/
│   ├── JacksonConfig.java              # JavaTimeModule, BigDecimal serialization
│   └── DatabaseConfig.java             # DataSource & HikariCP Configuration
├── resources/
│   ├── application.properties          # Base / Default configuration (File-backed SQL DB)
│   ├── application-local.properties    # Local development profile (File-backed SQL DB)
│   └── application-stage.properties    # Staging profile (Docker PostgreSQL 16)
├── docker-compose.yml                  # PostgreSQL 16 Alpine container definition
├── domain/
│   ├── TransactionType.java            # FLIGHT, HOTEL, RAIL, NAVAN_FEE (Enum)
│   ├── NavanFeeKind.java               # TRIP_FEE, AGENT_CALL_FEE (Enum)
│   ├── metadata/
│   │   ├── TransactionMetadata.java    # Base interface
│   │   ├── FlightMetadata.java         # origin, destination, fare, coupons
│   │   ├── FlightCoupon.java           # flightNumber, carrier, from, to, dep, arr
│   │   ├── HotelMetadata.java          # name, address, checkIn, checkOut, nights, rate, extras
│   │   ├── HotelAddress.java           # street, city, country
│   │   ├── HotelExtra.java             # name, amount
│   │   ├── RailMetadata.java           # legs
│   │   ├── RailLeg.java                # origin, dest, dep, arr, trainNumber, class
│   │   └── NavanFeeMetadata.java       # feeKind, description, relatedExternalId
│   ├── entity/
│   │   ├── TransactionEntity.java
│   │   ├── TransactionTaxLineEntity.java
│   │   ├── TransactionFeeLineEntity.java
│   │   ├── InvoiceEntity.java
│   │   ├── InvoiceTransactionSnapshotEntity.java
│   │   ├── InvoiceTaxLineSnapshotEntity.java
│   │   └── InvoiceFeeLineSnapshotEntity.java
│   └── repository/
│       ├── TransactionRepository.java
│       ├── InvoiceRepository.java
│       └── InvoiceTransactionSnapshotRepository.java
├── strategy/
│   ├── TransactionTypeHandler.java     # Strategy interface
│   ├── AbstractTransactionHandler.java # Template Method base class
│   ├── FlightTransactionHandler.java   # Flight strategy
│   ├── HotelTransactionHandler.java    # Hotel strategy
│   ├── RailTransactionHandler.java     # Rail strategy
│   ├── NavanFeeTransactionHandler.java # NavanFee strategy
│   └── TransactionHandlerRegistry.java # Strategy factory/registry
├── service/
│   ├── TransactionService.java         # Create, read, update, delete, pagination
│   ├── InvoiceService.java             # Coordination & validation
│   ├── InvoiceSnapshotEngine.java      # Memento pattern snapshot cloning
│   └── rollup/
│       ├── RollupAggregator.java       # Aggregator interface
│       ├── TaxRollupAggregator.java    # Groups by (name, rate)
│       └── FeeRollupAggregator.java    # Groups by (name)
├── web/
│   ├── TransactionController.java      # /transactions endpoints
│   ├── InvoiceController.java          # /invoices endpoints
│   ├── HealthController.java           # /health endpoint
│   ├── dto/                            # Request & Response POJOs (with Builders)
│   │   ├── CreateTransactionRequest.java
│   │   ├── TransactionResponse.java
│   │   ├── CreateInvoiceRequest.java
│   │   ├── InvoiceResponse.java
│   │   └── PageResponse.java
│   └── error/
│       ├── GlobalExceptionHandler.java # RFC 7807 ProblemDetails
│       ├── ConflictException.java      # 409
│       ├── ResourceNotFoundException.java # 404
│       └── BadRequestException.java    # 400
└── Application.java                    # Spring Boot Main
```

---

## 5. Rollup & Financial Precision Algorithms

### 5.1 Tax Rollup Grouping Algorithm
Taxes are grouped by both `name` and `rate` (accounting for null rates, e.g., fixed fee taxes):
```java
public record TaxRollupKey(String name, BigDecimal rate) {}

public List<TaxRollupItem> aggregate(List<InvoiceTaxLineSnapshotEntity> taxLines) {
    Map<TaxRollupKey, BigDecimal> taxMap = new LinkedHashMap<>();
    for (InvoiceTaxLineSnapshotEntity tax : taxLines) {
        BigDecimal normalizedRate = tax.getRate() != null ? tax.getRate().stripTrailingZeros() : null;
        TaxRollupKey key = new TaxRollupKey(tax.getName(), normalizedRate);
        taxMap.merge(key, tax.getAmount(), BigDecimal::add);
    }
    return taxMap.entrySet().stream()
        .map(entry -> new TaxRollupItem(
            entry.getKey().name(),
            entry.getKey().rate(),
            entry.getValue().setScale(2, RoundingMode.HALF_UP)
        ))
        .toList();
}
```

### 5.2 Fee Rollup Grouping Algorithm
Fees are grouped by `name`:
```java
public List<FeeRollupItem> aggregate(List<InvoiceFeeLineSnapshotEntity> feeLines) {
    Map<String, BigDecimal> feeMap = new LinkedHashMap<>();
    for (InvoiceFeeLineSnapshotEntity fee : feeLines) {
        feeMap.merge(fee.getName(), fee.getAmount(), BigDecimal::add);
    }
    return feeMap.entrySet().stream()
        .map(entry -> new FeeRollupItem(
            entry.getKey(),
            entry.getValue().setScale(2, RoundingMode.HALF_UP)
        ))
        .toList();
}
```

### 5.3 Subtotal Breakdown
The invoice response calculates and returns exact subtotals by category:
- `flight`: $\sum$ flight transaction totals
- `hotel`: $\sum$ hotel transaction totals
- `rail`: $\sum$ rail transaction totals
- `trip_fee`: $\sum$ navan_fee transactions where `fee_kind == "trip_fee"`
- `agent_call_fee`: $\sum$ navan_fee transactions where `fee_kind == "agent_call_fee"`
- `grand_total`: $\sum$ all transaction totals

---

## 6. High-Volume Performance & Concurrency Strategy

1. **Duplicate Elimination under Concurrency**:
   - `transactions.external_id` has a unique database index.
   - When concurrent requests race to insert the same `external_id`, the database permits exactly one write and throws a constraint violation on the second.
   - The application intercepts `DataIntegrityViolationException`, performs a lookup for the existing record, and returns `409 Conflict` containing the existing record's `id`.
2. **$O(1)$ Invoiced State Validation**:
   - Before executing `PATCH` or `DELETE` on `/transactions/{id}`, the service executes:
     ```sql
     SELECT EXISTS(
         SELECT 1 FROM invoice_transaction_snapshots 
         WHERE original_transaction_id = :id
     )
     ```
   - Backed by `idx_snapshot_orig_txn_id`, this query executes in sub-millisecond time.
3. **Connection Pool Protection**:
   - All schema validation, JSON deserialization, and metadata integrity checks execute before opening a transaction.
   - Database transactions are tightly scoped only to persistence operations to minimize connection lease duration.
4. **Guaranteed Bounded Pagination**:
   - Requests with `limit > 100` are rejected immediately (`400 Bad Request`).
   - SQL queries strictly execute `ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset`, avoiding table scans.

---

## 7. Mandatory Test Matrix & Verification

Automated test command: `./mvnw test`

| # | Test Scenario | Verified Behavior |
|---|---|---|
| **1** | **Create & Retrieve All Types** | Ingest flight, hotel, rail, `trip_fee`, and `agent_call_fee` from seeds. `GET` returns coupons, address, rail legs, and fee kinds. |
| **2** | **Duplicate `external_id` Rejection** | Second POST with identical `external_id` yields `409 Conflict` containing existing `id`. Only 1 row stored. |
| **3** | **Trip Invoice Generation** | Invoice combining all 5 seed transactions matches `expected-invoice.json` (grand total `685.70 EUR`, exact tax and fee rollups). |
| **4** | **Snapshot Immutability** | Modifying stored flight `total` in database leaves invoice snapshot intact (`GET /invoices/{id}` shows flight `220.00`). |
| **5** | **Mixed Currency Rejection** | Creating invoice with EUR and USD transactions returns `409 Conflict`. |
| **6** | **Concurrent Duplicate Race** | 10 concurrent threads POSTing identical `external_id` simultaneously yield exactly 1 success (`201`) and 9 conflicts (`409`). DB has 1 row. |
| **7** | **Invoiced Transaction PATCH 409** | Attempting `PATCH` on transaction present on an invoice returns `409 Conflict`. Zero data altered. |
| **8** | **Invoiced Transaction DELETE 409** | Attempting `DELETE` on transaction present on an invoice returns `409 Conflict`. Record remains intact. |
| **9** | **Invoice Deletion Isolation** | Deleting an invoice removes invoice and snapshots; underlying transactions remain retrievable via `GET /transactions/{id}`. |
| **10** | **Pagination Guardrails** | Default pagination returns 50 items. Request with `limit=1000` returns `400 Bad Request`. |

---

## 8. Implementation Plan

1. **Bootstrap Build**:
   - Initialize Maven project with committed wrapper (`./mvnw`).
   - Configure dependencies: Spring Boot Web, Spring Data JPA, H2 Database, Validation, Jackson.
2. **Persistence & Entities**:
   - Implement JPA entities matching the relational schema with `CHECK` constraints on `type`.
   - Configure file-backed database storage in `application.properties`.
3. **Domain Enums & Strategy Pattern**:
   - Implement `TransactionType` and `NavanFeeKind` enums.
   - Implement `TransactionTypeHandler` strategy interface and `AbstractTransactionHandler` base class.
   - Implement concrete handlers: `FlightTransactionHandler`, `HotelTransactionHandler`, `RailTransactionHandler`, `NavanFeeTransactionHandler`.
   - Implement `TransactionHandlerRegistry` for $O(1)$ dispatch.
4. **Snapshot & Aggregator Services**:
   - Implement `InvoiceSnapshotEngine` (Memento Pattern).
   - Implement `TaxRollupAggregator` and `FeeRollupAggregator` (Composite/Aggregator Pattern).
5. **Service Layer**:
   - Implement `TransactionService` (atomic create, lookup, patch, delete, invoiced check).
   - Implement `InvoiceService` (coordination, currency check, atomic rebuild).
6. **Controllers & RFC 7807 Error Handling**:
   - Implement REST controllers and global exception handler.
   - Build DTOs using fluent Builder pattern.
7. **Automated Test Suite**:
   - Implement all 10 integration tests in `src/test/java`.
8. **Documentation & Demonstration**:
   - Write `README.md` (one-command run, one-command test, curl scripts).
   - Write `ARCHITECTURE.md` (data model defense, design patterns, concurrency, snapshots, high-volume analysis).
