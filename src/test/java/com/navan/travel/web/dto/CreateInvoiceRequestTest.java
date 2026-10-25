package com.navan.travel.web.dto;

import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateInvoiceRequestTest {

    @Test
    @DisplayName("Resolves single transaction_id")
    void testSingleTransactionId() {
        CreateInvoiceRequest req = new CreateInvoiceRequest("txn_100", null);
        List<String> ids = req.resolveTransactionIds();

        assertThat(ids).containsExactly("txn_100");
    }

    @Test
    @DisplayName("Resolves multiple transaction_ids")
    void testMultipleTransactionIds() {
        CreateInvoiceRequest req = new CreateInvoiceRequest(null, List.of("txn_1", "txn_2", "txn_3"));
        List<String> ids = req.resolveTransactionIds();

        assertThat(ids).containsExactly("txn_1", "txn_2", "txn_3");
    }

    @Test
    @DisplayName("Resolves combined single and multiple non-overlapping IDs")
    void testCombinedDisjointIds() {
        CreateInvoiceRequest req = new CreateInvoiceRequest("txn_3", List.of("txn_1", "txn_2"));
        List<String> ids = req.resolveTransactionIds();

        assertThat(ids).containsExactly("txn_1", "txn_2", "txn_3");
    }

    @Test
    @DisplayName("Rejects duplicate IDs inside transaction_ids list with ConflictException (409)")
    void testDuplicateInList() {
        CreateInvoiceRequest req = new CreateInvoiceRequest(null, List.of("txn_1", "txn_2", "txn_1"));

        assertThatThrownBy(req::resolveTransactionIds)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Duplicate transaction ID listed on invoice request");
    }

    @Test
    @DisplayName("Rejects duplicate when transaction_id overlaps with transaction_ids list with ConflictException (409)")
    void testDuplicateOverlap() {
        CreateInvoiceRequest req = new CreateInvoiceRequest("txn_1", List.of("txn_1", "txn_2"));

        assertThatThrownBy(req::resolveTransactionIds)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Duplicate transaction ID listed on invoice request");
    }

    @Test
    @DisplayName("Rejects empty request with BadRequestException (400)")
    void testEmptyRequest() {
        CreateInvoiceRequest req1 = new CreateInvoiceRequest(null, null);
        assertThatThrownBy(req1::resolveTransactionIds)
                .isInstanceOf(BadRequestException.class);

        CreateInvoiceRequest req2 = new CreateInvoiceRequest("  ", List.of());
        assertThatThrownBy(req2::resolveTransactionIds)
                .isInstanceOf(BadRequestException.class);
    }
}
