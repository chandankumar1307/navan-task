package com.navan.travel.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_fee_line_snapshots", indexes = {
    @Index(name = "idx_fee_snapshot_entry_id", columnList = "snapshot_id")
})
public class InvoiceFeeLineSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private InvoiceTransactionSnapshotEntity snapshot;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public InvoiceFeeLineSnapshotEntity() {
    }

    public InvoiceFeeLineSnapshotEntity(String name, BigDecimal amount) {
        this.name = name;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
