package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated list envelope avoiding table-scanning count(*) bottlenecks")
public record PageResponse<T>(
    @Schema(description = "List of items in the current page")
    List<T> items,

    @Schema(description = "Page size limit (maximum 100)", example = "50")
    int limit,

    @Schema(description = "Offset index from which items were retrieved", example = "0")
    long offset,

    @JsonProperty("has_more")
    @Schema(description = "Indicates whether more items exist beyond the current page", example = "true")
    boolean hasMore
) {
}
