package com.b2b.warehouse.order.repository;

import com.b2b.warehouse.order.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransactionEntity> findByIdempotencyKey(String idempotencyKey);
}
