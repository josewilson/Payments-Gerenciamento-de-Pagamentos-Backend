package com.josewilson.payments.repository;


import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}