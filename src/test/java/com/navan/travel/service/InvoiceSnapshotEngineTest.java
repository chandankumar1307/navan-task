package com.navan.travel.service;

import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceSnapshotEngineTest {

    private InvoiceSnapshotEngine snapshotEngine;

    @BeforeEach
    void setUp() {
        snapshotEngine = new InvoiceSnapshotEngine();
    }

    @Test
    @DisplayName("Creates deep-copy snapshot of transaction, tax lines, and fee lines")
    void testCreateSnapshotDeepCopy() {
        TransactionEntity txn = new TransactionEntity(
                "txn_1",
                "ext-1",
                TransactionType.FLIGHT,
                "EUR",
                Instant.parse("2026-03-11T06:40:00Z"),
                new BigDecimal("220.00"),
                "{\"origin\":\"TXL\"}"
        );
        txn.addTaxLine(new TransactionTaxLineEntity("Airport tax", null, new BigDecimal("15.00")));
        txn.addFeeLine(new TransactionFeeLineEntity("YQ", new BigDecimal("25.00")));

        InvoiceEntity invoice = new InvoiceEntity("inv_1", "EUR", new BigDecimal("220.00"));

        InvoiceTransactionSnapshotEntity snapshot = snapshotEngine.createSnapshot(txn, invoice);

        assertThat(snapshot.getOriginalTransactionId()).isEqualTo("txn_1");
        assertThat(snapshot.getTransactionType()).isEqualTo(TransactionType.FLIGHT);
        assertThat(snapshot.getTotal()).isEqualByComparingTo("220.00");
        assertThat(snapshot.getMetadataJson()).isEqualTo("{\"origin\":\"TXL\"}");
        assertThat(snapshot.getOccurredAt()).isEqualTo(Instant.parse("2026-03-11T06:40:00Z"));

        // Tax lines snapshot
        assertThat(snapshot.getTaxLines()).hasSize(1);
        assertThat(snapshot.getTaxLines().get(0).getName()).isEqualTo("Airport tax");
        assertThat(snapshot.getTaxLines().get(0).getAmount()).isEqualByComparingTo("15.00");
        assertThat(snapshot.getTaxLines().get(0).getSnapshot()).isEqualTo(snapshot);

        // Fee lines snapshot
        assertThat(snapshot.getFeeLines()).hasSize(1);
        assertThat(snapshot.getFeeLines().get(0).getName()).isEqualTo("YQ");
        assertThat(snapshot.getFeeLines().get(0).getAmount()).isEqualByComparingTo("25.00");
        assertThat(snapshot.getFeeLines().get(0).getSnapshot()).isEqualTo(snapshot);

        // Invoice relationship
        assertThat(invoice.getSnapshots()).contains(snapshot);
        assertThat(snapshot.getInvoice()).isEqualTo(invoice);
    }

    @Test
    @DisplayName("createSnapshots processes multiple transactions and adds all to invoice")
    void testCreateSnapshotsList() {
        TransactionEntity txn1 = new TransactionEntity("txn_1", "ext-1", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        TransactionEntity txn2 = new TransactionEntity("txn_2", "ext-2", TransactionType.HOTEL, "EUR", Instant.now(), new BigDecimal("200.00"), "{}");

        InvoiceEntity invoice = new InvoiceEntity("inv_1", "EUR", new BigDecimal("300.00"));

        List<InvoiceTransactionSnapshotEntity> snapshots = snapshotEngine.createSnapshots(List.of(txn1, txn2), invoice);

        assertThat(snapshots).hasSize(2);
        assertThat(invoice.getSnapshots()).hasSize(2);
        assertThat(invoice.getSnapshots().get(0).getOriginalTransactionId()).isEqualTo("txn_1");
        assertThat(invoice.getSnapshots().get(1).getOriginalTransactionId()).isEqualTo("txn_2");
    }
}
