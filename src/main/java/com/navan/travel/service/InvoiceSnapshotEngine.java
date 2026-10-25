package com.navan.travel.service;

import com.navan.travel.domain.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceSnapshotEngine {

    public InvoiceTransactionSnapshotEntity createSnapshot(TransactionEntity transaction, InvoiceEntity invoice) {
        InvoiceTransactionSnapshotEntity snapshot = new InvoiceTransactionSnapshotEntity(
                transaction.getId(),
                transaction.getType(),
                transaction.getOccurredAt(),
                transaction.getTotal(),
                transaction.getMetadataJson()
        );

        if (transaction.getTaxLines() != null) {
            for (TransactionTaxLineEntity tax : transaction.getTaxLines()) {
                InvoiceTaxLineSnapshotEntity taxSnapshot = new InvoiceTaxLineSnapshotEntity(
                        tax.getName(),
                        tax.getRate(),
                        tax.getAmount()
                );
                snapshot.addTaxLine(taxSnapshot);
            }
        }

        if (transaction.getFeeLines() != null) {
            for (TransactionFeeLineEntity fee : transaction.getFeeLines()) {
                InvoiceFeeLineSnapshotEntity feeSnapshot = new InvoiceFeeLineSnapshotEntity(
                        fee.getName(),
                        fee.getAmount()
                );
                snapshot.addFeeLine(feeSnapshot);
            }
        }

        invoice.addSnapshot(snapshot);
        return snapshot;
    }

    public List<InvoiceTransactionSnapshotEntity> createSnapshots(List<TransactionEntity> transactions, InvoiceEntity invoice) {
        List<InvoiceTransactionSnapshotEntity> snapshots = new ArrayList<>();
        for (TransactionEntity transaction : transactions) {
            snapshots.add(createSnapshot(transaction, invoice));
        }
        return snapshots;
    }
}
