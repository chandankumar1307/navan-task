package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.FlightCoupon;
import com.navan.travel.domain.metadata.FlightMetadata;
import com.navan.travel.web.error.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlightTransactionHandlerTest {

    private FlightTransactionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        handler = new FlightTransactionHandler(objectMapper, validator);
    }

    @Test
    @DisplayName("Returns FLIGHT transaction type and FlightMetadata class")
    void testTypeAndClass() {
        assertThat(handler.getType()).isEqualTo(TransactionType.FLIGHT);
        assertThat(handler.getMetadataClass()).isEqualTo(FlightMetadata.class);
        assertThat(handler.getSubtotalCategory(null)).isEqualTo("flight");
    }

    @Test
    @DisplayName("Successfully parses and validates valid flight metadata with multiple coupons")
    void testValidFlightMetadata() {
        FlightCoupon coupon1 = new FlightCoupon("BA", "BA982", "TXL", "LHR", "2026-03-11T07:00:00Z", "2026-03-11T08:20:00Z");
        FlightCoupon coupon2 = new FlightCoupon("BA", "BA983", "LHR", "TXL", "2026-03-15T18:00:00Z", "2026-03-15T21:20:00Z");
        FlightMetadata metadata = new FlightMetadata("TXL", "LHR", new BigDecimal("350.00"), List.of(coupon1, coupon2));

        JsonNode node = objectMapper.valueToTree(metadata);
        FlightMetadata parsed = handler.parseAndValidateMetadata(node);

        assertThat(parsed.origin()).isEqualTo("TXL");
        assertThat(parsed.destination()).isEqualTo("LHR");
        assertThat(parsed.fare()).isEqualByComparingTo("350.00");
        assertThat(parsed.coupons()).hasSize(2);
        assertThat(parsed.coupons().get(0).flightNumber()).isEqualTo("BA982");
    }

    @Test
    @DisplayName("Rejects null or empty metadata node")
    void testNullOrEmptyMetadata() {
        assertThatThrownBy(() -> handler.parseAndValidateMetadata(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("metadata object is required");

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(objectMapper.createObjectNode()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("metadata object is required");
    }

    @Test
    @DisplayName("Rejects missing origin or destination")
    void testMissingOriginOrDestination() {
        JsonNode node = objectMapper.createObjectNode()
                .put("origin", "")
                .put("destination", "LHR")
                .put("fare", 100.0);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).putArray("coupons");

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Rejects empty coupons list")
    void testEmptyCouponsList() {
        FlightMetadata metadata = new FlightMetadata("TXL", "LHR", new BigDecimal("100.00"), List.of());
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Rejects coupon with blank flight number")
    void testCouponMissingFlightNumber() {
        FlightCoupon coupon = new FlightCoupon("BA", "  ", "TXL", "LHR", "2026-03-11T07:00:00Z", "2026-03-11T08:20:00Z");
        FlightMetadata metadata = new FlightMetadata("TXL", "LHR", new BigDecimal("100.00"), List.of(coupon));
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Serializes and deserializes flight metadata cleanly")
    void testSerializeAndDeserialize() {
        FlightCoupon coupon = new FlightCoupon("BA", "BA982", "TXL", "LHR", "2026-03-11T07:00:00Z", "2026-03-11T08:20:00Z");
        FlightMetadata metadata = new FlightMetadata("TXL", "LHR", new BigDecimal("200.00"), List.of(coupon));

        String json = handler.serializeMetadata(metadata);
        assertThat(json).contains("BA982");

        FlightMetadata deserialized = handler.deserializeMetadata(json);
        assertThat(deserialized.origin()).isEqualTo("TXL");
        assertThat(deserialized.coupons().get(0).flightNumber()).isEqualTo("BA982");
    }
}
