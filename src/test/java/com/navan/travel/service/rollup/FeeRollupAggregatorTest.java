package com.navan.travel.service.rollup;

import com.navan.travel.domain.entity.InvoiceFeeLineSnapshotEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeeRollupAggregatorTest {

    private FeeRollupAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new FeeRollupAggregator();
    }

    @Test
    @DisplayName("Returns empty list when input is null or empty")
    void testNullOrEmpty() {
        assertThat(aggregator.aggregate(null)).isEmpty();
        assertThat(aggregator.aggregate(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Aggregates fee lines with identical name")
    void testIdenticalFeeName() {
        InvoiceFeeLineSnapshotEntity fee1 = new InvoiceFeeLineSnapshotEntity("YQ", new BigDecimal("25.00"));
        InvoiceFeeLineSnapshotEntity fee2 = new InvoiceFeeLineSnapshotEntity("YQ", new BigDecimal("35.00"));

        List<FeeRollupItem> rollup = aggregator.aggregate(List.of(fee1, fee2));

        assertThat(rollup).hasSize(1);
        assertThat(rollup.get(0).name()).isEqualTo("YQ");
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Maintains distinct rollup items for different fee names")
    void testDifferentFeeNames() {
        InvoiceFeeLineSnapshotEntity fee1 = new InvoiceFeeLineSnapshotEntity("YQ", new BigDecimal("25.00"));
        InvoiceFeeLineSnapshotEntity fee2 = new InvoiceFeeLineSnapshotEntity("Booking fee", new BigDecimal("4.00"));

        List<FeeRollupItem> rollup = aggregator.aggregate(List.of(fee1, fee2));

        assertThat(rollup).hasSize(2);
        assertThat(rollup.get(0).name()).isEqualTo("YQ");
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("25.00");

        assertThat(rollup.get(1).name()).isEqualTo("Booking fee");
        assertThat(rollup.get(1).amount()).isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("Rounds fractional fee amounts using HALF_UP to scale 2")
    void testHalfUpRounding() {
        InvoiceFeeLineSnapshotEntity fee1 = new InvoiceFeeLineSnapshotEntity("Surcharge", new BigDecimal("12.344"));
        InvoiceFeeLineSnapshotEntity fee2 = new InvoiceFeeLineSnapshotEntity("Surcharge", new BigDecimal("12.346"));

        // Sum: 24.690 -> 24.69
        List<FeeRollupItem> rollup = aggregator.aggregate(List.of(fee1, fee2));

        assertThat(rollup).hasSize(1);
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("24.69");
    }
}
