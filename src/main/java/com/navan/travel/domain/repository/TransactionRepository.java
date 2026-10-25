package com.navan.travel.domain.repository;

import com.navan.travel.domain.entity.TransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    Optional<TransactionEntity> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    Slice<TransactionEntity> findSliceBy(Pageable pageable);
}
