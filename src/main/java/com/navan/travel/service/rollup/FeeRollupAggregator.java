package com.navan.travel.service.rollup;

import com.navan.travel.domain.entity.InvoiceFeeLineSnapshotEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FeeRollupAggregator {

    public List<FeeRollupItem> aggregate(List<InvoiceFeeLineSnapshotEntity> feeLines) {
        if (feeLines == null || feeLines.isEmpty()) {
            return List.of();
        }

        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (InvoiceFeeLineSnapshotEntity fee : feeLines) {
            map.merge(fee.getName(), fee.getAmount(), BigDecimal::add);
        }

        List<FeeRollupItem> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : map.entrySet()) {
            BigDecimal scaledAmount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            result.add(new FeeRollupItem(entry.getKey(), scaledAmount));
        }
        return result;
    }
}
