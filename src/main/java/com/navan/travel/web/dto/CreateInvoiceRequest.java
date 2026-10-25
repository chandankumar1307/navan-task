package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ConflictException;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "Request payload for creating or replacing an invoice (accepts single or multiple transaction IDs)")
public record CreateInvoiceRequest(
    @JsonProperty("transaction_id")
    @Schema(description = "Single transaction ID to invoice", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19", nullable = true)
    String transactionId,

    @JsonProperty("transaction_ids")
    @Schema(description = "List of transaction IDs to combine on a single trip invoice", example = "[\"txn_flight_1\", \"txn_hotel_2\", \"txn_fee_3\"]", nullable = true)
    List<String> transactionIds
) {

    public List<String> resolveTransactionIds() {
        List<String> result = new ArrayList<>();
        if (transactionIds != null && !transactionIds.isEmpty()) {
            Set<String> seen = new HashSet<>();
            for (String id : transactionIds) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                if (!seen.add(id)) {
                    throw new ConflictException("Duplicate transaction ID listed on invoice request: " + id);
                }
                result.add(id);
            }
        }
        if (transactionId != null && !transactionId.isBlank()) {
            if (result.contains(transactionId)) {
                throw new ConflictException("Duplicate transaction ID listed on invoice request: " + transactionId);
            }
            result.add(transactionId);
        }
        if (result.isEmpty()) {
            throw new BadRequestException("At least one transaction_id or transaction_ids must be provided to create an invoice");
        }
        return result;
    }
}
