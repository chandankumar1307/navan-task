package com.navan.travel.domain.repository;

import com.navan.travel.domain.entity.InvoiceTransactionSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceTransactionSnapshotRepository extends JpaRepository<InvoiceTransactionSnapshotEntity, Long> {

    boolean existsByOriginalTransactionId(String originalTransactionId);

    List<InvoiceTransactionSnapshotEntity> findByInvoiceId(String invoiceId);

    List<InvoiceTransactionSnapshotEntity> findByOriginalTransactionId(String originalTransactionId);
}
