package com.navan.travel.web;

import com.navan.travel.service.InvoiceService;
import com.navan.travel.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices", description = "Endpoints for generating, querying, updating, and deleting immutable invoice snapshots")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @Operation(
            summary = "Create an invoice by transaction ID(s)",
            description = "Creates an immutable invoice snapshot from one or more stored transaction IDs. " +
                          "Deep-copies transaction data, computes categorized subtotals, rolls up taxes by (name, rate), " +
                          "and rolls up fees by name. Rejects mixed currencies or duplicate transaction IDs with 409 Conflict."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invoice created successfully",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body (no transaction IDs provided)",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "One or more transaction IDs not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "409", description = "Mixed currency conflict or duplicate transaction ID on invoice",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<InvoiceResponse> createInvoice(@RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get invoice by ID",
            description = "Retrieves the immutable invoice snapshot including grand total, categorized totals, " +
                          "itemized tax rollup, fee rollup, and frozen transaction lines. Does not join mutable transaction rows."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice retrieved successfully",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<InvoiceResponse> getInvoice(
            @Parameter(description = "Server-generated invoice ID", example = "inv_80d79ceeb92e4abfae7813a824479532")
            @PathVariable String id) {
        InvoiceResponse response = invoiceService.getInvoice(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List invoices (paginated)",
            description = "Retrieves a paginated list of invoices ordered by creation date descending. Maximum page limit is 100."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of invoices retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Pagination limit exceeded 100",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<PageResponse<InvoiceResponse>> listInvoices(
            @Parameter(description = "Page size limit (default: 50, maximum: 100)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset count from which to retrieve items", example = "0")
            @RequestParam(defaultValue = "0") long offset) {
        PageResponse<InvoiceResponse> response = invoiceService.listInvoices(limit, offset);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Replace invoice transaction set",
            description = "Atomically rebuilds the invoice snapshot from current stored transactions for the new transaction ID set. " +
                          "Rejects mixed currencies with 409 Conflict leaving the invoice unchanged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice snapshot rebuilt successfully",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "Invoice or one of the replacement transaction IDs not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class))),
            @ApiResponse(responseCode = "409", description = "Mixed currency conflict among replacement transactions",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public ResponseEntity<InvoiceResponse> patchInvoice(
            @Parameter(description = "Server-generated invoice ID", example = "inv_80d79ceeb92e4abfae7813a824479532")
            @PathVariable String id,
            @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.patchInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete an invoice",
            description = "Deletes the invoice and its snapshot records. Does NOT delete or alter underlying transaction rows."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invoice and snapshots deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetailDto.class)))
    })
    public void deleteInvoice(
            @Parameter(description = "Server-generated invoice ID", example = "inv_80d79ceeb92e4abfae7813a824479532")
            @PathVariable String id) {
        invoiceService.deleteInvoice(id);
    }
}
