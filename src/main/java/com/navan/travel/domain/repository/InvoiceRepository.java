package com.navan.travel.domain.repository;

import com.navan.travel.domain.entity.InvoiceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {

    Slice<InvoiceEntity> findSliceBy(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvoiceEntity i WHERE i.id = :id")
    Optional<InvoiceEntity> findByIdForUpdate(@Param("id") String id);
}
