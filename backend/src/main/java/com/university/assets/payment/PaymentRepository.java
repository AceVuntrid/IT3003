package com.university.assets.payment;

import com.university.assets.common.model.Enums.TransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    @EntityGraph(attributePaths = {"payerUser", "payerDepartment", "reservation", "asset", "originalPayment"})
    Optional<Payment> findDetailedById(UUID id);

    @Query("select coalesce(sum(p.amount), 0) from Payment p "
            + "where p.transactionType <> com.university.assets.common.model.Enums.TransactionType.REFUND "
            + "and p.paymentDate between :from and :to")
    BigDecimal totalCollectedBetween(@Param("from") Instant from, @Param("to") Instant to);

    long countByTransactionType(TransactionType type);
}
