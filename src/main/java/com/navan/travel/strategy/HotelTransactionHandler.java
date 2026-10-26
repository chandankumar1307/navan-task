package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.HotelMetadata;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ErrorCode;
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
            throw new BadRequestException(ErrorCode.HOTEL_ADDRESS_REQUIRED);
        }
        LocalDate checkIn;
        LocalDate checkOut;
        try {
            checkIn = LocalDate.parse(metadata.checkIn());
            checkOut = LocalDate.parse(metadata.checkOut());
        } catch (DateTimeParseException e) {
            throw new BadRequestException(ErrorCode.HOTEL_DATE_FORMAT_INVALID);
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException(ErrorCode.HOTEL_CHECKOUT_BEFORE_CHECKIN);
        }
    }

    @Override
    public String getSubtotalCategory(HotelMetadata metadata) {
        return "hotel";
    }
}
