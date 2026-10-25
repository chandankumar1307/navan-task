package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ConflictException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record CreateInvoiceRequest(
    @JsonProperty("transaction_id")
    String transactionId,

    @JsonProperty("transaction_ids")
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
