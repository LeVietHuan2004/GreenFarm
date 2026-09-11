package com.agri.ecommerce.scheduler;

import com.agri.ecommerce.service.PaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentExpirationJob {
    private final PaymentService paymentService;

    public PaymentExpirationJob(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelayString = "${app.payment.expiration-scan-ms:60000}")
    public void expirePendingPayments() {
        paymentService.expirePendingVnpayPayments();
    }
}
