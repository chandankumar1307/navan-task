package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navan.travel.domain.NavanFeeKind;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.NavanFeeMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NavanFeeTransactionHandlerTest {

    private NavanFeeTransactionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        handler = new NavanFeeTransactionHandler(objectMapper, validator);
    }

    @Test
    @DisplayName("Returns NAVAN_FEE transaction type and NavanFeeMetadata class")
    void testTypeAndClass() {
        assertThat(handler.getType()).isEqualTo(TransactionType.NAVAN_FEE);
        assertThat(handler.getMetadataClass()).isEqualTo(NavanFeeMetadata.class);
    }

    @Test
    @DisplayName("Resolves subtotal categories as trip_fee vs agent_call_fee")
    void testSubtotalCategories() {
        NavanFeeMetadata tripFee = new NavanFeeMetadata(NavanFeeKind.TRIP_FEE, "Trip fee", "ref-1");
        NavanFeeMetadata agentFee = new NavanFeeMetadata(NavanFeeKind.AGENT_CALL_FEE, "Support call", "ref-2");

        assertThat(handler.getSubtotalCategory(tripFee)).isEqualTo("trip_fee");
        assertThat(handler.getSubtotalCategory(agentFee)).isEqualTo("agent_call_fee");
        assertThat(handler.getSubtotalCategory(null)).isEqualTo("navan_fee");
    }

    @Test
    @DisplayName("Successfully parses and validates trip_fee and agent_call_fee")
    void testValidFeeKinds() {
        NavanFeeMetadata metadata = new NavanFeeMetadata(NavanFeeKind.TRIP_FEE, "Trip fee", "flight-1");
        JsonNode node = objectMapper.valueToTree(metadata);

        NavanFeeMetadata parsed = handler.parseAndValidateMetadata(node);
        assertThat(parsed.feeKind()).isEqualTo(NavanFeeKind.TRIP_FEE);
        assertThat(parsed.description()).isEqualTo("Trip fee");
        assertThat(parsed.relatedExternalId()).isEqualTo("flight-1");
    }

    @Test
    @DisplayName("Rejects missing or null fee_kind")
    void testMissingFeeKind() {
        JsonNode node = objectMapper.createObjectNode()
                .put("description", "A fee with no kind");

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Rejects invalid fee_kind string")
    void testInvalidFeeKind() {
        JsonNode node = objectMapper.createObjectNode()
                .put("fee_kind", "unknown_kind");

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Serializes and deserializes navan fee metadata cleanly")
    void testSerializeAndDeserialize() {
        NavanFeeMetadata metadata = new NavanFeeMetadata(NavanFeeKind.AGENT_CALL_FEE, "Phone call", "hotel-1");
        String json = handler.serializeMetadata(metadata);
        assertThat(json).contains("agent_call_fee");

        NavanFeeMetadata deserialized = handler.deserializeMetadata(json);
        assertThat(deserialized.feeKind()).isEqualTo(NavanFeeKind.AGENT_CALL_FEE);
        assertThat(deserialized.description()).isEqualTo("Phone call");
    }
}
