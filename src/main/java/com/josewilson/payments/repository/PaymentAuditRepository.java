package com.josewilson.payments.repository;


import com.josewilson.payments.domain.PaymentAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentAuditRepository extends MongoRepository<PaymentAudit, String> {
    List<PaymentAudit> findByPaymentId(Long paymentId);
}
