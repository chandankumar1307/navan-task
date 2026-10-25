package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated list envelope")
public record PageResponse<T>(
    @Schema(description = "List of items in the current page")
    List<T> items,

    @Schema(description = "Page size limit (maximum 100)", example = "50")
    int limit,

    @Schema(description = "Offset index from which items were retrieved", example = "0")
    long offset,

    @JsonProperty("total_count")
    @Schema(description = "Total number of available items across all pages", example = "150")
    long totalCount
) {
}
