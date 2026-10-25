package com.navan.travel.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RFC 7807 Problem Details representation for HTTP error responses")
public record ProblemDetailDto(
        @Schema(description = "URI reference identifying the problem type", example = "about:blank")
        String type,

        @Schema(description = "Short summary of the problem type", example = "Bad Request")
        String title,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Human-readable explanation specific to this occurrence", example = "Validation failed: external_id is required")
        String detail
) {
}
