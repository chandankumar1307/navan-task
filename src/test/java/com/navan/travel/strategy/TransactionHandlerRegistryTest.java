package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.TransactionMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionHandlerRegistryTest {

    private TransactionHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        List<TransactionTypeHandler<? extends TransactionMetadata>> handlers = List.of(
                new FlightTransactionHandler(objectMapper, validator),
                new HotelTransactionHandler(objectMapper, validator),
                new RailTransactionHandler(objectMapper, validator),
                new NavanFeeTransactionHandler(objectMapper, validator)
        );

        registry = new TransactionHandlerRegistry(handlers);
    }

    @Test
    @DisplayName("Successfully retrieves handlers for all 4 supported transaction types")
    void testHandlerLookup() {
        assertThat(registry.getHandler(TransactionType.FLIGHT)).isInstanceOf(FlightTransactionHandler.class);
        assertThat(registry.getHandler(TransactionType.HOTEL)).isInstanceOf(HotelTransactionHandler.class);
        assertThat(registry.getHandler(TransactionType.RAIL)).isInstanceOf(RailTransactionHandler.class);
        assertThat(registry.getHandler(TransactionType.NAVAN_FEE)).isInstanceOf(NavanFeeTransactionHandler.class);
    }

    @Test
    @DisplayName("Rejects null transaction type with BadRequestException")
    void testNullTypeLookup() {
        assertThatThrownBy(() -> registry.getHandler(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("transaction type is required");
    }
}
