package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.metadata.RailMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

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
    }

    @Override
    public String getSubtotalCategory(TransactionEntity transaction, RailMetadata metadata) {
        return "rail";
    }
}
