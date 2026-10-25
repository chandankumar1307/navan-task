package com.navan.travel.domain.repository;

import com.navan.travel.domain.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {

    Page<InvoiceEntity> findAll(Pageable pageable);
}
