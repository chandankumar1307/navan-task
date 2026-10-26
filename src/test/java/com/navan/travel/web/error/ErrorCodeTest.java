package com.navan.travel.web.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("ErrorCode formatMessage replaces positional format specifiers")
    void testFormatMessageWithArgs() {
        String msg = ErrorCode.METADATA_REQUIRED.formatMessage("flight");
        assertThat(msg).isEqualTo("metadata object is required for transaction type: flight");

        String multiArg = ErrorCode.INVOICE_MIXED_CURRENCIES.formatMessage("USD", "EUR");
        assertThat(multiArg).isEqualTo("Mixed currencies detected: all transactions on an invoice must share the same currency (expected USD, found EUR)");
    }

    @Test
    @DisplayName("ErrorCode formatMessage strips format specifier when no args provided")
    void testFormatMessageWithoutArgs() {
        String msg = ErrorCode.BAD_REQUEST.formatMessage();
        assertThat(msg).isEmpty();

        String staticMsg = ErrorCode.LIMIT_EXCEEDED.formatMessage();
        assertThat(staticMsg).isEqualTo("limit cannot exceed 100");
    }

    @Test
    @DisplayName("ErrorCode properties return expected values")
    void testErrorCodeProperties() {
        assertThat(ErrorCode.TRANSACTION_NOT_FOUND.getCode()).isEqualTo("ERR_TXN_NOT_FOUND");
        assertThat(ErrorCode.TRANSACTION_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.TRANSACTION_NOT_FOUND.getMessageTemplate()).isEqualTo("Transaction not found: %s");
    }

    @Test
    @DisplayName("BadRequestException resolves message from ErrorCode")
    void testBadRequestExceptionWithErrorCode() {
        BadRequestException ex = new BadRequestException(ErrorCode.EXTERNAL_ID_REQUIRED);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_ID_REQUIRED);
        assertThat(ex.getMessage()).isEqualTo("external_id is required");
        assertThat(ex.getArgs()).isEmpty();
    }

    @Test
    @DisplayName("ResourceNotFoundException resolves message from ErrorCode with parameter")
    void testResourceNotFoundExceptionWithErrorCode() {
        ResourceNotFoundException ex = new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, "txn_123");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Transaction not found: txn_123");
        assertThat(ex.getArgs()).containsExactly("txn_123");
    }

    @Test
    @DisplayName("ConflictException resolves message from ErrorCode and retains existingId")
    void testConflictExceptionWithErrorCodeAndExistingId() {
        ConflictException ex = ConflictException.withExistingId(
                ErrorCode.TRANSACTION_EXTERNAL_ID_CONFLICT,
                "txn_existing_abc",
                "ext_999"
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_EXTERNAL_ID_CONFLICT);
        assertThat(ex.getExistingId()).isEqualTo("txn_existing_abc");
        assertThat(ex.getMessage()).isEqualTo("Transaction with external_id 'ext_999' already exists.");
        assertThat(ex.getArgs()).containsExactly("ext_999");
    }

    @Test
    @DisplayName("ConflictException without existingId resolves message from ErrorCode")
    void testConflictExceptionWithoutExistingId() {
        ConflictException ex = new ConflictException(ErrorCode.TRANSACTION_FROZEN_MUTATION_BLOCKED);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_FROZEN_MUTATION_BLOCKED);
        assertThat(ex.getExistingId()).isNull();
        assertThat(ex.getMessage()).isEqualTo("Transaction is included on an active invoice and is frozen. Mutations are rejected.");
    }
}
