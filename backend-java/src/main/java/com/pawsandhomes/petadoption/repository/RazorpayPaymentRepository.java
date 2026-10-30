package com.pawsandhomes.petadoption.repository;

import com.pawsandhomes.petadoption.entity.PaymentStatus;
import com.pawsandhomes.petadoption.entity.RazorpayPayment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RazorpayPaymentRepository extends JpaRepository<RazorpayPayment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from RazorpayPayment payment where payment.razorpayOrderId = :orderId "
            + "and payment.user.id = :userId")
    Optional<RazorpayPayment> findOwnedByOrderIdForUpdate(
            @Param("orderId") String orderId,
            @Param("userId") Long userId);

    Optional<RazorpayPayment> findFirstByAdoption_IdAndStatusOrderByCreatedAtDesc(
            Long adoptionId,
            PaymentStatus status);
}
