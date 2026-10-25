package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.RailLeg;
import com.navan.travel.domain.metadata.RailMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RailTransactionHandlerTest {

    private RailTransactionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        handler = new RailTransactionHandler(objectMapper, validator);
    }

    @Test
    @DisplayName("Returns RAIL transaction type and RailMetadata class")
    void testTypeAndClass() {
        assertThat(handler.getType()).isEqualTo(TransactionType.RAIL);
        assertThat(handler.getMetadataClass()).isEqualTo(RailMetadata.class);
        assertThat(handler.getSubtotalCategory(null)).isEqualTo("rail");
    }

    @Test
    @DisplayName("Successfully parses and validates rail metadata with through journey legs")
    void testValidRailMetadata() {
        RailLeg leg1 = new RailLeg("London St Pancras", "Lille Europe", "2026-03-13T09:00:00Z", "2026-03-13T10:30:00Z", "EST9108", "standard");
        RailLeg leg2 = new RailLeg("Lille Europe", "Paris Nord", "2026-03-13T11:00:00Z", "2026-03-13T12:05:00Z", "EST9122", "standard");
        RailMetadata metadata = new RailMetadata(List.of(leg1, leg2));

        JsonNode node = objectMapper.valueToTree(metadata);
        RailMetadata parsed = handler.parseAndValidateMetadata(node);

        assertThat(parsed.legs()).hasSize(2);
        assertThat(parsed.legs().get(0).origin()).isEqualTo("London St Pancras");
        assertThat(parsed.legs().get(1).destination()).isEqualTo("Paris Nord");
    }

    @Test
    @DisplayName("Rejects empty legs list")
    void testEmptyLegsList() {
        RailMetadata metadata = new RailMetadata(List.of());
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Rejects leg missing origin or destination")
    void testIncompleteLeg() {
        RailLeg leg = new RailLeg("", "Paris Nord", "2026-03-13T11:00:00Z", "2026-03-13T12:05:00Z", "EST9122", "standard");
        RailMetadata metadata = new RailMetadata(List.of(leg));
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Serializes and deserializes rail metadata cleanly")
    void testSerializeAndDeserialize() {
        RailLeg leg = new RailLeg("London", "Paris", "2026-03-13T09:00:00Z", "2026-03-13T12:05:00Z", "EST1", "first");
        RailMetadata metadata = new RailMetadata(List.of(leg));

        String json = handler.serializeMetadata(metadata);
        assertThat(json).contains("EST1");

        RailMetadata deserialized = handler.deserializeMetadata(json);
        assertThat(deserialized.legs().get(0).trainNumber()).isEqualTo("EST1");
    }

    @Test
    @DisplayName("Rejects rail leg with departure after arrival")
    void testTemporalDepartureAfterArrival() {
        RailLeg leg = new RailLeg("London", "Paris", "2026-03-13T14:00:00Z", "2026-03-13T12:00:00Z", "EST1", "first");
        RailMetadata metadata = new RailMetadata(List.of(leg));
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Rail leg departure must be strictly before arrival");
    }

    @Test
    @DisplayName("Rejects rail leg with malformed timestamp")
    void testMalformedTimestamp() {
        RailLeg leg = new RailLeg("London", "Paris", "invalid-ts", "2026-03-13T12:00:00Z", "EST1", "first");
        RailMetadata metadata = new RailMetadata(List.of(leg));
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timestamp for rail leg departure or arrival");
    }
}
