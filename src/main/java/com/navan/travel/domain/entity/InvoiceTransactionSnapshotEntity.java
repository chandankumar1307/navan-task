package com.navan.travel.domain.entity;

import com.navan.travel.domain.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice_transaction_snapshots", indexes = {
    @Index(name = "idx_snapshot_orig_txn_id", columnList = "original_transaction_id"),
    @Index(name = "idx_snapshot_invoice_id", columnList = "invoice_id")
})
public class InvoiceTransactionSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "original_transaction_id", nullable = false, length = 36)
    private String originalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Lob
    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceTaxLineSnapshotEntity> taxLines = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceFeeLineSnapshotEntity> feeLines = new ArrayList<>();

    public InvoiceTransactionSnapshotEntity() {
    }

    public InvoiceTransactionSnapshotEntity(String originalTransactionId, TransactionType transactionType,
                                            Instant occurredAt, BigDecimal total, String metadataJson) {
        this.originalTransactionId = originalTransactionId;
        this.transactionType = transactionType;
        this.occurredAt = occurredAt;
        this.total = total;
        this.metadataJson = metadataJson;
    }

    public void addTaxLine(InvoiceTaxLineSnapshotEntity taxLine) {
        taxLines.add(taxLine);
        taxLine.setSnapshot(this);
    }

    public void addFeeLine(InvoiceFeeLineSnapshotEntity feeLine) {
        feeLines.add(feeLine);
        feeLine.setSnapshot(this);
    }

    public Long getId() {
        return id;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceEntity invoice) {
        this.invoice = invoice;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public List<InvoiceTaxLineSnapshotEntity> getTaxLines() {
        return taxLines;
    }

    public void setTaxLines(List<InvoiceTaxLineSnapshotEntity> taxLines) {
        this.taxLines = taxLines;
    }

    public List<InvoiceFeeLineSnapshotEntity> getFeeLines() {
        return feeLines;
    }

    public void setFeeLines(List<InvoiceFeeLineSnapshotEntity> feeLines) {
        this.feeLines = feeLines;
    }
}
