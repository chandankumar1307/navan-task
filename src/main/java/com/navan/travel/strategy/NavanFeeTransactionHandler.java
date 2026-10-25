package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.metadata.NavanFeeMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class NavanFeeTransactionHandler extends AbstractTransactionHandler<NavanFeeMetadata> {

    public NavanFeeTransactionHandler(ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.NAVAN_FEE;
    }

    @Override
    public Class<NavanFeeMetadata> getMetadataClass() {
        return NavanFeeMetadata.class;
    }

    @Override
    protected void validateCustom(NavanFeeMetadata metadata) {
        if (metadata.feeKind() == null) {
            throw new BadRequestException("navan_fee must include a valid fee_kind ('trip_fee' or 'agent_call_fee')");
        }
    }

    @Override
    public String getSubtotalCategory(TransactionEntity transaction, NavanFeeMetadata metadata) {
        if (metadata != null && metadata.feeKind() != null) {
            return metadata.feeKind().getValue();
        }
        return "navan_fee";
    }
}
