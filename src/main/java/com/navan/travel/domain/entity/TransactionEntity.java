package com.navan.travel.domain.entity;

import com.navan.travel.domain.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_txn_external_id", columnList = "external_id", unique = true),
    @Index(name = "idx_txn_created_at", columnList = "created_at, id"),
    @Index(name = "idx_txn_type", columnList = "type")
})
public class TransactionEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "external_id", nullable = false, unique = true, length = 255)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TransactionType type;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Lob
    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionTaxLineEntity> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionFeeLineEntity> feeLines = new ArrayList<>();

    public TransactionEntity() {
    }

    public TransactionEntity(String id, String externalId, TransactionType type, String currency,
                             Instant occurredAt, BigDecimal total, String metadataJson) {
        this.id = id;
        this.externalId = externalId;
        this.type = type;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.total = total;
        this.metadataJson = metadataJson;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addTaxLine(TransactionTaxLineEntity taxLine) {
        taxLines.add(taxLine);
        taxLine.setTransaction(this);
    }

    public void clearTaxLines() {
        taxLines.clear();
    }

    public void addFeeLine(TransactionFeeLineEntity feeLine) {
        feeLines.add(feeLine);
        feeLine.setTransaction(this);
    }

    public void clearFeeLines() {
        feeLines.clear();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TransactionTaxLineEntity> getTaxLines() {
        return taxLines;
    }

    public void setTaxLines(List<TransactionTaxLineEntity> taxLines) {
        this.taxLines = taxLines;
    }

    public List<TransactionFeeLineEntity> getFeeLines() {
        return feeLines;
    }

    public void setFeeLines(List<TransactionFeeLineEntity> feeLines) {
        this.feeLines = feeLines;
    }
}
