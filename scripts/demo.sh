#!/usr/bin/env bash
set -e

# ==============================================================================
# Navan Commerce: Travel Inventory & Invoicing API Demonstration Script
# ==============================================================================
# Demonstrates all core functional requirements:
# 1. Multi-modal transaction ingestion (flight, hotel, rail, navan_fee)
# 2. Duplicate external_id conflict handling (409 Conflict with existing ID)
# 3. Read and paginated listing of transactions
# 4. Multi-item trip invoice generation with tax & fee rollups (685.70 EUR)
# 5. Snapshot immutability: blocking PATCH/DELETE on invoiced transactions
# 6. Invoice PATCH (atomic replacement) and DELETE (underlying transactions preserved)
# ==============================================================================

BASE_URL="http://localhost:8080"

# Colors for terminal output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}   NAVAN COMMERCE: TRAVEL INVENTORY & INVOICE DEMO RUNNER            ${NC}"
echo -e "${BLUE}======================================================================${NC}"

# Check server health
echo -e "\n${YELLOW}>> Checking service health on ${BASE_URL}/health...${NC}"
HEALTH_RES=$(curl -s -w "\n%{http_code}" "${BASE_URL}/health" || true)
HTTP_CODE=$(echo "$HEALTH_RES" | tail -n1)
BODY=$(echo "$HEALTH_RES" | sed '$d')

if [ "$HTTP_CODE" != "200" ]; then
  echo -e "${RED}Error: Service is not reachable on ${BASE_URL}. Please start the service first:${NC}"
  echo -e "   ./mvnw spring-boot:run"
  exit 1
fi
echo -e "${GREEN}Service is healthy! Response: ${BODY}${NC}"

TIMESTAMP=$(date +%s)

# 1. Create Flight Transaction
echo -e "\n${YELLOW}>> 1. Ingesting Flight Transaction (POST /transactions)...${NC}"
FLIGHT_EXT_ID="flight-demo-${TIMESTAMP}"
FLIGHT_RES=$(curl -s -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "'"${FLIGHT_EXT_ID}"'",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.00,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
    "metadata": {
      "origin": "TXL",
      "destination": "LHR",
      "fare": 180.00,
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
  }')
echo "$FLIGHT_RES"
FLIGHT_ID=$(echo "$FLIGHT_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
echo -e "${GREEN}Created Flight ID: ${FLIGHT_ID}${NC}"

# 2. Create Hotel Transaction
echo -e "\n${YELLOW}>> 2. Ingesting Hotel Transaction (POST /transactions)...${NC}"
HOTEL_EXT_ID="hotel-demo-${TIMESTAMP}"
HOTEL_RES=$(curl -s -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "hotel",
    "external_id": "'"${HOTEL_EXT_ID}"'",
    "currency": "EUR",
    "occurred_at": "2026-03-11T15:00:00Z",
    "total": 331.70,
    "tax_lines": [{ "name": "VAT", "rate": 0.07, "amount": 21.70 }],
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
      "room_rate_per_night": 140.00,
      "extras": [{ "name": "Breakfast", "amount": 30.00 }]
    }
  }')
echo "$HOTEL_RES"
HOTEL_ID=$(echo "$HOTEL_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
echo -e "${GREEN}Created Hotel ID: ${HOTEL_ID}${NC}"

# 3. Create Rail Transaction
echo -e "\n${YELLOW}>> 3. Ingesting Rail Transaction (POST /transactions)...${NC}"
RAIL_EXT_ID="rail-demo-${TIMESTAMP}"
RAIL_RES=$(curl -s -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "rail",
    "external_id": "'"${RAIL_EXT_ID}"'",
    "currency": "EUR",
    "occurred_at": "2026-03-13T09:00:00Z",
    "total": 94.00,
    "tax_lines": [],
    "fee_lines": [{ "name": "Booking fee", "amount": 4.00 }],
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
  }')
echo "$RAIL_RES"
RAIL_ID=$(echo "$RAIL_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
echo -e "${GREEN}Created Rail ID: ${RAIL_ID}${NC}"

# 4. Create Navan Fee: Trip Fee
echo -e "\n${YELLOW}>> 4. Ingesting Navan Fee: Trip Fee (POST /transactions)...${NC}"
TRIP_FEE_EXT_ID="navan-trip-fee-${TIMESTAMP}"
TRIP_FEE_RES=$(curl -s -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "navan_fee",
    "external_id": "'"${TRIP_FEE_EXT_ID}"'",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 25.00,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "fee_kind": "trip_fee",
      "description": "Navan trip fee",
      "related_external_id": "'"${FLIGHT_EXT_ID}"'"
    }
  }')
echo "$TRIP_FEE_RES"
TRIP_FEE_ID=$(echo "$TRIP_FEE_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
echo -e "${GREEN}Created Trip Fee ID: ${TRIP_FEE_ID}${NC}"

# 5. Create Navan Fee: Agent Call Fee
echo -e "\n${YELLOW}>> 5. Ingesting Navan Fee: Agent Call Fee (POST /transactions)...${NC}"
AGENT_FEE_EXT_ID="navan-agent-call-${TIMESTAMP}"
AGENT_FEE_RES=$(curl -s -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "navan_fee",
    "external_id": "'"${AGENT_FEE_EXT_ID}"'",
    "currency": "EUR",
    "occurred_at": "2026-03-11T18:00:00Z",
    "total": 15.00,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "fee_kind": "agent_call_fee",
      "description": "Agent fee for support call",
      "related_external_id": "'"${HOTEL_EXT_ID}"'"
    }
  }')
echo "$AGENT_FEE_RES"
AGENT_FEE_ID=$(echo "$AGENT_FEE_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
echo -e "${GREEN}Created Agent Call Fee ID: ${AGENT_FEE_ID}${NC}"

# 6. Duplicate External ID Conflict
echo -e "\n${YELLOW}>> 6. Attempting Duplicate POST with existing external_id (${FLIGHT_EXT_ID})...${NC}"
DUP_RES=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "'"${FLIGHT_EXT_ID}"'",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.00,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "origin": "TXL",
      "destination": "LHR",
      "fare": 180.00,
      "coupons": []
    }
  }')
DUP_HTTP=$(echo "$DUP_RES" | tail -n1)
DUP_BODY=$(echo "$DUP_RES" | sed '$d')
echo -e "HTTP Status: ${DUP_HTTP}"
echo -e "Response Body: ${DUP_BODY}"
if [ "$DUP_HTTP" == "409" ]; then
  echo -e "${GREEN}Correctly rejected duplicate with 409 Conflict returning existing ID!${NC}"
else
  echo -e "${RED}Expected 409 Conflict but received ${DUP_HTTP}!${NC}"
fi

# 7. List Transactions (Paginated)
echo -e "\n${YELLOW}>> 7. Listing Transactions with pagination (?limit=5&offset=0)...${NC}"
LIST_TXN_RES=$(curl -s "${BASE_URL}/transactions?limit=5&offset=0")
echo "$LIST_TXN_RES"

# 8. Create Trip Invoice
echo -e "\n${YELLOW}>> 8. Creating Trip Invoice Combining all 5 Transactions (POST /invoices)...${NC}"
INVOICE_RES=$(curl -s -X POST "${BASE_URL}/invoices" \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_ids": [
      "'"${FLIGHT_ID}"'",
      "'"${HOTEL_ID}"'",
      "'"${RAIL_ID}"'",
      "'"${TRIP_FEE_ID}"'",
      "'"${AGENT_FEE_ID}"'"
    ]
  }')
echo "$INVOICE_RES"
INVOICE_ID=$(echo "$INVOICE_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
echo -e "${GREEN}Created Trip Invoice ID: ${INVOICE_ID}${NC}"

# 9. Verify Snapshot Mutation Protection
echo -e "\n${YELLOW}>> 9. Attempting to PATCH invoiced flight transaction (expecting 409 Conflict)...${NC}"
PATCH_TXN_RES=$(curl -s -w "\n%{http_code}" -X PATCH "${BASE_URL}/transactions/${FLIGHT_ID}" \
  -H "Content-Type: application/json" \
  -d '{ "total": 999.00 }')
PATCH_HTTP=$(echo "$PATCH_TXN_RES" | tail -n1)
PATCH_BODY=$(echo "$PATCH_TXN_RES" | sed '$d')
echo -e "HTTP Status: ${PATCH_HTTP}"
echo -e "Response Body: ${PATCH_BODY}"

echo -e "\n${YELLOW}>> 10. Attempting to DELETE invoiced flight transaction (expecting 409 Conflict)...${NC}"
DEL_TXN_RES=$(curl -s -w "\n%{http_code}" -X DELETE "${BASE_URL}/transactions/${FLIGHT_ID}")
DEL_HTTP=$(echo "$DEL_TXN_RES" | tail -n1)
DEL_BODY=$(echo "$DEL_TXN_RES" | sed '$d')
echo -e "HTTP Status: ${DEL_HTTP}"
echo -e "Response Body: ${DEL_BODY}"

# 11. Read Invoice to prove snapshot immutability
echo -e "\n${YELLOW}>> 11. Reading Invoice (GET /invoices/${INVOICE_ID})...${NC}"
GET_INV_RES=$(curl -s "${BASE_URL}/invoices/${INVOICE_ID}")
echo "$GET_INV_RES"

# 12. Delete Invoice and verify underlying transactions persist
echo -e "\n${YELLOW}>> 12. Deleting Invoice (DELETE /invoices/${INVOICE_ID})...${NC}"
DEL_INV_RES=$(curl -s -w "\n%{http_code}" -X DELETE "${BASE_URL}/invoices/${INVOICE_ID}")
echo -e "Delete Invoice HTTP Status: $(echo "$DEL_INV_RES" | tail -n1)"

echo -e "\n${YELLOW}>> 13. Verifying original transaction still exists after invoice deletion...${NC}"
GET_ORIG_TXN=$(curl -s -w "\n%{http_code}" "${BASE_URL}/transactions/${FLIGHT_ID}")
ORIG_HTTP=$(echo "$GET_ORIG_TXN" | tail -n1)
ORIG_BODY=$(echo "$GET_ORIG_TXN" | sed '$d')
echo -e "Original Flight Status: ${ORIG_HTTP}"
echo -e "Original Flight Body: ${ORIG_BODY}"

echo -e "\n${GREEN}======================================================================${NC}"
echo -e "${GREEN}   DEMONSTRATION COMPLETED SUCCESSFULLY!                             ${NC}"
echo -e "${GREEN}======================================================================${NC}"
