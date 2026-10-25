package com.navan.travel.service.rollup;

import com.navan.travel.domain.entity.InvoiceTaxLineSnapshotEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaxRollupAggregator {

    public record TaxKey(String name, BigDecimal rate) {
    }

    public List<TaxRollupItem> aggregate(List<InvoiceTaxLineSnapshotEntity> taxLines) {
        if (taxLines == null || taxLines.isEmpty()) {
            return List.of();
        }

        Map<TaxKey, BigDecimal> map = new LinkedHashMap<>();
        for (InvoiceTaxLineSnapshotEntity tax : taxLines) {
            BigDecimal normalizedRate = tax.getRate() != null ? tax.getRate().stripTrailingZeros() : null;
            TaxKey key = new TaxKey(tax.getName(), normalizedRate);
            map.merge(key, tax.getAmount(), BigDecimal::add);
        }

        List<TaxRollupItem> result = new ArrayList<>();
        for (Map.Entry<TaxKey, BigDecimal> entry : map.entrySet()) {
            BigDecimal scaledAmount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            result.add(new TaxRollupItem(entry.getKey().name(), entry.getKey().rate(), scaledAmount));
        }
        return result;
    }
}
