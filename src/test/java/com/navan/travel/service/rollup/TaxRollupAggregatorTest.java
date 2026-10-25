package com.navan.travel.service.rollup;

import com.navan.travel.domain.entity.InvoiceTaxLineSnapshotEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaxRollupAggregatorTest {

    private TaxRollupAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new TaxRollupAggregator();
    }

    @Test
    @DisplayName("Returns empty list when input is null or empty")
    void testNullOrEmpty() {
        assertThat(aggregator.aggregate(null)).isEmpty();
        assertThat(aggregator.aggregate(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Aggregates tax lines with identical name and rate")
    void testIdenticalNameAndRate() {
        InvoiceTaxLineSnapshotEntity line1 = new InvoiceTaxLineSnapshotEntity("Airport tax", null, new BigDecimal("15.00"));
        InvoiceTaxLineSnapshotEntity line2 = new InvoiceTaxLineSnapshotEntity("Airport tax", null, new BigDecimal("25.50"));

        List<TaxRollupItem> rollup = aggregator.aggregate(List.of(line1, line2));

        assertThat(rollup).hasSize(1);
        assertThat(rollup.get(0).name()).isEqualTo("Airport tax");
        assertThat(rollup.get(0).rate()).isNull();
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("40.50");
    }

    @Test
    @DisplayName("Separates tax lines with the same name but different rates (VAT 7% vs VAT 19%)")
    void testDifferentRatesSameName() {
        InvoiceTaxLineSnapshotEntity vatLow = new InvoiceTaxLineSnapshotEntity("VAT", new BigDecimal("0.07"), new BigDecimal("21.70"));
        InvoiceTaxLineSnapshotEntity vatStandard = new InvoiceTaxLineSnapshotEntity("VAT", new BigDecimal("0.19"), new BigDecimal("57.00"));

        List<TaxRollupItem> rollup = aggregator.aggregate(List.of(vatLow, vatStandard));

        assertThat(rollup).hasSize(2);
        assertThat(rollup.get(0).name()).isEqualTo("VAT");
        assertThat(rollup.get(0).rate()).isEqualByComparingTo("0.07");
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("21.70");

        assertThat(rollup.get(1).name()).isEqualTo("VAT");
        assertThat(rollup.get(1).rate()).isEqualByComparingTo("0.19");
        assertThat(rollup.get(1).amount()).isEqualByComparingTo("57.00");
    }

    @Test
    @DisplayName("Normalizes rate trailing zeros so 0.0700 and 0.07 aggregate together")
    void testRateTrailingZeroNormalization() {
        InvoiceTaxLineSnapshotEntity vat1 = new InvoiceTaxLineSnapshotEntity("VAT", new BigDecimal("0.0700"), new BigDecimal("10.00"));
        InvoiceTaxLineSnapshotEntity vat2 = new InvoiceTaxLineSnapshotEntity("VAT", new BigDecimal("0.07"), new BigDecimal("15.00"));

        List<TaxRollupItem> rollup = aggregator.aggregate(List.of(vat1, vat2));

        assertThat(rollup).hasSize(1);
        assertThat(rollup.get(0).rate()).isEqualByComparingTo("0.07");
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("Preserves financial precision and applies HALF_UP rounding")
    void testHalfUpRounding() {
        InvoiceTaxLineSnapshotEntity line1 = new InvoiceTaxLineSnapshotEntity("City tax", null, new BigDecimal("10.124"));
        InvoiceTaxLineSnapshotEntity line2 = new InvoiceTaxLineSnapshotEntity("City tax", null, new BigDecimal("10.126"));

        // Sum: 20.250 -> 20.25
        List<TaxRollupItem> rollup = aggregator.aggregate(List.of(line1, line2));

        assertThat(rollup).hasSize(1);
        assertThat(rollup.get(0).amount()).isEqualByComparingTo("20.25");
    }
}
