package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.metadata.FlightCoupon;
import com.navan.travel.domain.metadata.FlightMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class FlightTransactionHandler extends AbstractTransactionHandler<FlightMetadata> {

    public FlightTransactionHandler(ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.FLIGHT;
    }

    @Override
    public Class<FlightMetadata> getMetadataClass() {
        return FlightMetadata.class;
    }

    @Override
    protected void validateCustom(FlightMetadata metadata) {
        if (metadata.coupons() == null || metadata.coupons().isEmpty()) {
            throw new BadRequestException("Flight metadata must include at least one coupon");
        }
        for (FlightCoupon coupon : metadata.coupons()) {
            if (coupon.flightNumber() == null || coupon.flightNumber().isBlank()) {
                throw new BadRequestException("Flight coupon must include flight_number");
            }
        }
    }

    @Override
    public String getSubtotalCategory(TransactionEntity transaction, FlightMetadata metadata) {
        return "flight";
    }
}
