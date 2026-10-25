package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.RailLeg;
import com.navan.travel.domain.metadata.RailMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Component
public class RailTransactionHandler extends AbstractTransactionHandler<RailMetadata> {

    public RailTransactionHandler(ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.RAIL;
    }

    @Override
    public Class<RailMetadata> getMetadataClass() {
        return RailMetadata.class;
    }

    @Override
    protected void validateCustom(RailMetadata metadata) {
        if (metadata.legs() == null || metadata.legs().isEmpty()) {
            throw new BadRequestException("Rail metadata must include at least one leg");
        }
        for (RailLeg leg : metadata.legs()) {
            Instant departure;
            Instant arrival;
            try {
                departure = Instant.parse(leg.departure());
                arrival = Instant.parse(leg.arrival());
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid timestamp for rail leg departure or arrival (expected ISO-8601)");
            }
            if (!arrival.isAfter(departure)) {
                throw new BadRequestException("Rail leg departure must be strictly before arrival");
            }
        }
    }

    @Override
    public String getSubtotalCategory(RailMetadata metadata) {
        return "rail";
    }
}
