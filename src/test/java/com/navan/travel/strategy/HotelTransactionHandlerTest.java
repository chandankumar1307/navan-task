package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.metadata.HotelAddress;
import com.navan.travel.domain.metadata.HotelExtra;
import com.navan.travel.domain.metadata.HotelMetadata;
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

class HotelTransactionHandlerTest {

    private HotelTransactionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        handler = new HotelTransactionHandler(objectMapper, validator);
    }

    @Test
    @DisplayName("Returns HOTEL transaction type and HotelMetadata class")
    void testTypeAndClass() {
        assertThat(handler.getType()).isEqualTo(TransactionType.HOTEL);
        assertThat(handler.getMetadataClass()).isEqualTo(HotelMetadata.class);
        assertThat(handler.getSubtotalCategory(null)).isEqualTo("hotel");
    }

    @Test
    @DisplayName("Successfully parses and validates valid hotel metadata with address and extras")
    void testValidHotelMetadata() {
        HotelAddress address = new HotelAddress("12 Kingsway", "London", "GB");
        HotelExtra extra = new HotelExtra("Breakfast", new BigDecimal("30.00"));
        HotelMetadata metadata = new HotelMetadata(
                "Hotel Mitte London",
                address,
                "2026-03-11",
                "2026-03-13",
                2,
                new BigDecimal("140.00"),
                List.of(extra)
        );

        JsonNode node = objectMapper.valueToTree(metadata);
        HotelMetadata parsed = handler.parseAndValidateMetadata(node);

        assertThat(parsed.name()).isEqualTo("Hotel Mitte London");
        assertThat(parsed.address().street()).isEqualTo("12 Kingsway");
        assertThat(parsed.address().city()).isEqualTo("London");
        assertThat(parsed.address().country()).isEqualTo("GB");
        assertThat(parsed.nights()).isEqualTo(2);
        assertThat(parsed.roomRatePerNight()).isEqualByComparingTo("140.00");
        assertThat(parsed.extras()).hasSize(1);
    }

    @Test
    @DisplayName("Rejects missing hotel address")
    void testMissingAddress() {
        HotelMetadata metadata = new HotelMetadata(
                "Hotel No Address",
                null,
                "2026-03-11",
                "2026-03-13",
                2,
                new BigDecimal("140.00"),
                List.of()
        );
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Rejects missing address fields like city or country")
    void testIncompleteAddress() {
        HotelAddress address = new HotelAddress("12 Kingsway", "", "GB");
        HotelMetadata metadata = new HotelMetadata(
                "Hotel Incomplete",
                address,
                "2026-03-11",
                "2026-03-13",
                2,
                new BigDecimal("140.00"),
                List.of()
        );
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Rejects nights less than 1")
    void testInvalidNights() {
        HotelAddress address = new HotelAddress("12 Kingsway", "London", "GB");
        HotelMetadata metadata = new HotelMetadata(
                "Hotel Zero Nights",
                address,
                "2026-03-11",
                "2026-03-13",
                0,
                new BigDecimal("140.00"),
                List.of()
        );
        JsonNode node = objectMapper.valueToTree(metadata);

        assertThatThrownBy(() -> handler.parseAndValidateMetadata(node))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Serializes and deserializes hotel metadata cleanly")
    void testSerializeAndDeserialize() {
        HotelAddress address = new HotelAddress("12 Kingsway", "London", "GB");
        HotelMetadata metadata = new HotelMetadata(
                "Hotel Mitte",
                address,
                "2026-03-11",
                "2026-03-13",
                1,
                new BigDecimal("100.00"),
                null
        );

        String json = handler.serializeMetadata(metadata);
        assertThat(json).contains("Hotel Mitte");

        HotelMetadata deserialized = handler.deserializeMetadata(json);
        assertThat(deserialized.name()).isEqualTo("Hotel Mitte");
        assertThat(deserialized.address().city()).isEqualTo("London");
    }
}
