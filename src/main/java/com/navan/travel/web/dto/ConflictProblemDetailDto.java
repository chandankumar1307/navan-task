package com.navan.travel.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RFC 7807 Conflict response containing the existing resource identifier")
public record ConflictProblemDetailDto(
        @Schema(description = "URI reference identifying the problem type", example = "about:blank")
        String type,

        @Schema(description = "Short summary of the problem type", example = "Conflict")
        String title,

        @Schema(description = "HTTP status code", example = "409")
        int status,

        @Schema(description = "Human-readable explanation specific to this occurrence", example = "Transaction with external_id 'flight-TXL-LHR-2026-03-11' already exists.")
        String detail,

        @Schema(description = "Server identifier of the existing conflicting transaction", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19")
        String id
) {
}
