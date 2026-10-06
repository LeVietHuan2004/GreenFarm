package com.agri.ecommerce.service;

import com.agri.ecommerce.entity.PaymentMethod;
import com.agri.ecommerce.entity.PaymentStatus;
import com.agri.ecommerce.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceEmailRetryService {
    private final PaymentRepository payments;
    private final InvoiceEmailService invoiceEmails;
    private final int maxAttempts;

    public InvoiceEmailRetryService(PaymentRepository payments, InvoiceEmailService invoiceEmails,
                                    @Value("${app.invoice.max-attempts:5}") int maxAttempts) {
        this.payments = payments;
        this.invoiceEmails = invoiceEmails;
        this.maxAttempts = maxAttempts;
    }

    @Transactional(readOnly = true)
    public List<Long> findDueIds() {
        return payments.findInvoiceRetryIds(maxAttempts, LocalDateTime.now(), PaymentMethod.COD, PaymentStatus.COMPLETED);
    }

    @Transactional
    public void retry(Long paymentId) {
        payments.findByIdForUpdate(paymentId).ifPresent(payment -> {
            if (payment.getInvoiceEmailSentAt() == null && payment.getInvoiceEmailAttempts() < maxAttempts
                && payment.getInvoiceEmailNextRetryAt() != null
                && !payment.getInvoiceEmailNextRetryAt().isAfter(LocalDateTime.now())) {
                invoiceEmails.sendInvoiceIfEnabled(payment);
                payments.save(payment);
            }
        });
    }
}
