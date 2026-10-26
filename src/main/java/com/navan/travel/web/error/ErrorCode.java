package com.navan.travel.web.error;

import org.springframework.http.HttpStatus;

/**
 * Standard error codes and message templates for the Navan Travel Inventory & Invoicing Service.
 */
public enum ErrorCode {

    // 400 Bad Request - General & Pagination
    BAD_REQUEST("ERR_BAD_REQUEST", HttpStatus.BAD_REQUEST, "%s"),
    VALIDATION_FAILED("ERR_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Validation failed: %s"),
    INVALID_JSON_BODY("ERR_INVALID_JSON_BODY", HttpStatus.BAD_REQUEST, "%s"),
    LIMIT_EXCEEDED("ERR_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "limit cannot exceed 100"),
    OFFSET_NEGATIVE("ERR_OFFSET_NEGATIVE", HttpStatus.BAD_REQUEST, "Offset must not be negative"),
    LIMIT_NON_POSITIVE("ERR_LIMIT_NON_POSITIVE", HttpStatus.BAD_REQUEST, "Limit must be greater than zero"),

    // 400 Bad Request - Transactions & Metadata
    TRANSACTION_TYPE_REQUIRED("ERR_TXN_TYPE_REQUIRED", HttpStatus.BAD_REQUEST, "transaction type is required"),
    EXTERNAL_ID_REQUIRED("ERR_EXTERNAL_ID_REQUIRED", HttpStatus.BAD_REQUEST, "external_id is required"),
    METADATA_REQUIRED("ERR_METADATA_REQUIRED", HttpStatus.BAD_REQUEST, "metadata object is required for transaction type: %s"),
    METADATA_PARSE_FAILED("ERR_METADATA_PARSE_FAILED", HttpStatus.BAD_REQUEST, "metadata could not be parsed for %s"),
    METADATA_VALIDATION_FAILED("ERR_METADATA_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Validation failed for %s metadata: %s"),
    METADATA_INVALID("ERR_METADATA_INVALID", HttpStatus.BAD_REQUEST, "Invalid %s metadata: %s"),
    NO_HANDLER_FOR_TYPE("ERR_NO_HANDLER_FOR_TYPE", HttpStatus.BAD_REQUEST, "No handler registered for transaction type: %s"),
    UNKNOWN_TRANSACTION_TYPE("ERR_UNKNOWN_TXN_TYPE", HttpStatus.BAD_REQUEST, "Unknown transaction type: %s"),
    UNKNOWN_NAVAN_FEE_KIND("ERR_UNKNOWN_NAVAN_FEE_KIND", HttpStatus.BAD_REQUEST, "Unknown navan_fee fee_kind: %s"),

    // 400 Bad Request - Modality Metadata Specifics
    FLIGHT_COUPONS_REQUIRED("ERR_FLIGHT_COUPONS_REQUIRED", HttpStatus.BAD_REQUEST, "Flight metadata must include at least one coupon"),
    FLIGHT_COUPON_NUMBER_REQUIRED("ERR_FLIGHT_COUPON_NUMBER_REQUIRED", HttpStatus.BAD_REQUEST, "Flight coupon must include flight_number"),
    FLIGHT_TIMESTAMP_INVALID("ERR_FLIGHT_TIMESTAMP_INVALID", HttpStatus.BAD_REQUEST, "Invalid timestamp for flight coupon departure or arrival (expected ISO-8601)"),
    FLIGHT_DEPARTURE_AFTER_ARRIVAL("ERR_FLIGHT_DEPARTURE_AFTER_ARRIVAL", HttpStatus.BAD_REQUEST, "Flight coupon departure must be strictly before arrival"),

    HOTEL_ADDRESS_REQUIRED("ERR_HOTEL_ADDRESS_REQUIRED", HttpStatus.BAD_REQUEST, "Hotel address must include street, city, and country"),
    HOTEL_DATE_FORMAT_INVALID("ERR_HOTEL_DATE_FORMAT_INVALID", HttpStatus.BAD_REQUEST, "Invalid date format for check_in or check_out (expected YYYY-MM-DD)"),
    HOTEL_CHECKOUT_BEFORE_CHECKIN("ERR_HOTEL_CHECKOUT_BEFORE_CHECKIN", HttpStatus.BAD_REQUEST, "Hotel check_out date must be strictly after check_in date"),

    RAIL_LEGS_REQUIRED("ERR_RAIL_LEGS_REQUIRED", HttpStatus.BAD_REQUEST, "Rail metadata must include at least one leg"),
    RAIL_TIMESTAMP_INVALID("ERR_RAIL_TIMESTAMP_INVALID", HttpStatus.BAD_REQUEST, "Invalid timestamp for rail leg departure or arrival (expected ISO-8601)"),
    RAIL_DEPARTURE_AFTER_ARRIVAL("ERR_RAIL_DEPARTURE_AFTER_ARRIVAL", HttpStatus.BAD_REQUEST, "Rail leg departure must be strictly before arrival"),

    NAVAN_FEE_KIND_REQUIRED("ERR_NAVAN_FEE_KIND_REQUIRED", HttpStatus.BAD_REQUEST, "navan_fee must include a valid fee_kind ('trip_fee' or 'agent_call_fee')"),

    // 400 Bad Request - Invoices
    INVOICE_EMPTY_REQUEST("ERR_INVOICE_EMPTY_REQUEST", HttpStatus.BAD_REQUEST, "At least one transaction_id or transaction_ids must be provided to create an invoice"),

    // 404 Not Found
    TRANSACTION_NOT_FOUND("ERR_TXN_NOT_FOUND", HttpStatus.NOT_FOUND, "Transaction not found: %s"),
    TRANSACTION_NOT_FOUND_FOR_INVOICE("ERR_TXN_NOT_FOUND_FOR_INVOICE", HttpStatus.NOT_FOUND, "Transaction not found for invoice: %s"),
    INVOICE_NOT_FOUND("ERR_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice not found: %s"),
    RESOURCE_NOT_FOUND("ERR_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "%s"),

    // 409 Conflict
    TRANSACTION_EXTERNAL_ID_CONFLICT("ERR_TXN_EXTERNAL_ID_CONFLICT", HttpStatus.CONFLICT, "Transaction with external_id '%s' already exists."),
    TRANSACTION_EXTERNAL_ID_DUPLICATE("ERR_TXN_EXTERNAL_ID_DUPLICATE", HttpStatus.CONFLICT, "Transaction with this external_id already exists."),
    TRANSACTION_FROZEN_MUTATION_BLOCKED("ERR_TXN_FROZEN_MUTATION", HttpStatus.CONFLICT, "Transaction is included on an active invoice and is frozen. Mutations are rejected."),
    TRANSACTION_FROZEN_DELETION_BLOCKED("ERR_TXN_FROZEN_DELETION", HttpStatus.CONFLICT, "Transaction is included on an active invoice and cannot be deleted."),
    INVOICE_DUPLICATE_TXN_ID("ERR_INVOICE_DUPLICATE_TXN_ID", HttpStatus.CONFLICT, "Duplicate transaction ID listed on invoice request: %s"),
    INVOICE_MIXED_CURRENCIES("ERR_INVOICE_MIXED_CURRENCIES", HttpStatus.CONFLICT, "Mixed currencies detected: all transactions on an invoice must share the same currency (expected %s, found %s)"),
    CONFLICT("ERR_CONFLICT", HttpStatus.CONFLICT, "%s"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR("ERR_INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: %s");

    private final String code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;

    ErrorCode(String code, HttpStatus httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * Resolves and formats the human-readable message using the enum's message template and arguments.
     */
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0) {
            // Strip any leftover format specifiers if no arguments were supplied
            return messageTemplate.replace("%s", "").trim();
        }
        return String.format(messageTemplate, args);
    }
}
