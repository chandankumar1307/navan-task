package com.navan.travel.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_tax_line_snapshots", indexes = {
    @Index(name = "idx_tax_snapshot_entry_id", columnList = "snapshot_id")
})
public class InvoiceTaxLineSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private InvoiceTransactionSnapshotEntity snapshot;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "rate", precision = 6, scale = 4)
    private BigDecimal rate;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public InvoiceTaxLineSnapshotEntity() {
    }

    public InvoiceTaxLineSnapshotEntity(String name, BigDecimal rate, BigDecimal amount) {
        this.name = name;
        this.rate = rate;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public InvoiceTransactionSnapshotEntity getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(InvoiceTransactionSnapshotEntity snapshot) {
        this.snapshot = snapshot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
