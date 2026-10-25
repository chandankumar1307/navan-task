package com.navan.travel.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffsetLimitRequestTest {

    @Test
    @DisplayName("OffsetLimitRequest returns exact arbitrary offset and limit")
    void testArbitraryOffsetAndLimit() {
        OffsetLimitRequest req = new OffsetLimitRequest(5, 10, Sort.by("id"));

        assertThat(req.getOffset()).isEqualTo(5L);
        assertThat(req.getPageSize()).isEqualTo(10);
        assertThat(req.getPageNumber()).isEqualTo(0);
        assertThat(req.getSort()).isEqualTo(Sort.by("id"));
        assertThat(req.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Non-multiple offset returns correct navigation and pages")
    void testNavigation() {
        OffsetLimitRequest req = new OffsetLimitRequest(7, 4);

        assertThat(req.getOffset()).isEqualTo(7L);
        assertThat(req.getPageSize()).isEqualTo(4);

        OffsetLimitRequest next = (OffsetLimitRequest) req.next();
        assertThat(next.getOffset()).isEqualTo(11L);
        assertThat(next.getPageSize()).isEqualTo(4);

        OffsetLimitRequest prev = (OffsetLimitRequest) req.previousOrFirst();
        assertThat(prev.getOffset()).isEqualTo(3L);

        OffsetLimitRequest first = (OffsetLimitRequest) req.first();
        assertThat(first.getOffset()).isEqualTo(0L);

        OffsetLimitRequest page2 = (OffsetLimitRequest) req.withPage(2);
        assertThat(page2.getOffset()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Rejects negative offset or zero/negative limit")
    void testInvalidArguments() {
        assertThatThrownBy(() -> new OffsetLimitRequest(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Offset must not be negative");

        assertThatThrownBy(() -> new OffsetLimitRequest(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than zero");
    }

    @Test
    @DisplayName("Supports equals and hashCode")
    void testEqualsAndHashCode() {
        OffsetLimitRequest req1 = new OffsetLimitRequest(5, 10);
        OffsetLimitRequest req2 = new OffsetLimitRequest(5, 10);
        OffsetLimitRequest req3 = new OffsetLimitRequest(6, 10);

        assertThat(req1).isEqualTo(req2);
        assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        assertThat(req1).isNotEqualTo(req3);
    }
}
