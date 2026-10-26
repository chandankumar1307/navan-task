package com.navan.travel.strategy;

import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.TransactionMetadata;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionHandlerRegistry {

    private final Map<TransactionType, TransactionTypeHandler<?>> handlerMap = new EnumMap<>(TransactionType.class);

    public TransactionHandlerRegistry(List<TransactionTypeHandler<?>> handlers) {
        for (TransactionTypeHandler<?> handler : handlers) {
            handlerMap.put(handler.getType(), handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends TransactionMetadata> TransactionTypeHandler<T> getHandler(TransactionType type) {
        if (type == null) {
            throw new BadRequestException(ErrorCode.TRANSACTION_TYPE_REQUIRED);
        }
        TransactionTypeHandler<?> handler = handlerMap.get(type);
        if (handler == null) {
            throw new BadRequestException(ErrorCode.NO_HANDLER_FOR_TYPE, type.getValue());
        }
        return (TransactionTypeHandler<T>) handler;
    }

    public TransactionTypeHandler<?> getHandlerAny(TransactionType type) {
        return getHandler(type);
    }
}
