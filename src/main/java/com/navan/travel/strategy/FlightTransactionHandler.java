package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.FlightCoupon;
import com.navan.travel.domain.metadata.FlightMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

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
            Instant departure;
            Instant arrival;
            try {
                departure = Instant.parse(coupon.departure());
                arrival = Instant.parse(coupon.arrival());
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid timestamp for flight coupon departure or arrival (expected ISO-8601)");
            }
            if (!arrival.isAfter(departure)) {
                throw new BadRequestException("Flight coupon departure must be strictly before arrival");
            }
        }
    }

    @Override
    public String getSubtotalCategory(FlightMetadata metadata) {
        return "flight";
    }
}
