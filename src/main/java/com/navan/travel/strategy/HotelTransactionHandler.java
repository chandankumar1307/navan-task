package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.HotelMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class HotelTransactionHandler extends AbstractTransactionHandler<HotelMetadata> {

    public HotelTransactionHandler(ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.HOTEL;
    }

    @Override
    public Class<HotelMetadata> getMetadataClass() {
        return HotelMetadata.class;
    }

    @Override
    protected void validateCustom(HotelMetadata metadata) {
        if (metadata.address() == null) {
            throw new BadRequestException("Hotel address must include street, city, and country");
        }
        LocalDate checkIn;
        LocalDate checkOut;
        try {
            checkIn = LocalDate.parse(metadata.checkIn());
            checkOut = LocalDate.parse(metadata.checkOut());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format for check_in or check_out (expected YYYY-MM-DD)");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Hotel check_out date must be strictly after check_in date");
        }
    }

    @Override
    public String getSubtotalCategory(HotelMetadata metadata) {
        return "hotel";
    }
}
