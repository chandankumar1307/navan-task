package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int limit,
    long offset,
    @JsonProperty("total_count")
    long totalCount
) {
}
