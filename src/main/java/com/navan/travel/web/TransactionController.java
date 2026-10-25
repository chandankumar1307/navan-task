package com.navan.travel.web;

import com.navan.travel.service.TransactionService;
import com.navan.travel.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Endpoints for ingesting, querying, updating, and deleting multi-modal travel transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(
            summary = "Create a travel transaction",
            description = "Ingests a multi-modal travel transaction (flight, hotel, rail, or navan_fee). " +
                          "Enforces database uniqueness on external_id, returning 409 Conflict with the winning transaction ID on duplicates."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate external_id conflict",
                    content = @Content(schema = @Schema(implementation = ConflictProblemDetailDto.class)))
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request.externalId() != null) {
            httpRequest.setAttribute("external_id", request.externalId().trim());
        }
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get transaction by ID",
            description = "Retrieves the stored transaction record including type, currency, total, tax_lines, fee_lines, and typed metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "Server-generated transaction ID", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19")
            @PathVariable String id) {
        TransactionResponse response = transactionService.getTransaction(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List transactions (paginated)",
            description = "Retrieves a paginated list of transactions ordered by creation date descending. Maximum page limit is 100."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Pagination limit exceeded 100",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<PageResponse<TransactionResponse>> listTransactions(
            @Parameter(description = "Page size limit (default: 50, maximum: 100)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset count from which to retrieve items", example = "0")
            @RequestParam(defaultValue = "0") long offset) {
        PageResponse<TransactionResponse> response = transactionService.listTransactions(limit, offset);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Patch transaction fields",
            description = "Updates mutable transaction fields (total, occurred_at, tax_lines, fee_lines, metadata). " +
                          "If this transaction is referenced by ANY invoice, mutation is rejected with 409 Conflict to preserve frozen snapshots."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for patch payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "409", description = "Transaction is frozen on an invoice and cannot be modified",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<TransactionResponse> patchTransaction(
            @Parameter(description = "Server-generated transaction ID", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19")
            @PathVariable String id,
            @Valid @RequestBody PatchTransactionRequest request) {
        TransactionResponse response = transactionService.patchTransaction(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a transaction",
            description = "Atomically deletes a transaction and its child tax/fee lines. " +
                          "If the transaction is referenced on ANY invoice, deletion is rejected with 409 Conflict."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "409", description = "Transaction is frozen on an invoice and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public void deleteTransaction(
            @Parameter(description = "Server-generated transaction ID", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19")
            @PathVariable String id) {
        transactionService.deleteTransaction(id);
    }
}
