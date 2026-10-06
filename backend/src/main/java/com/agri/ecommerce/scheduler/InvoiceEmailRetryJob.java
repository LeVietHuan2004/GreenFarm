package com.agri.ecommerce.scheduler;

import com.agri.ecommerce.service.InvoiceEmailRetryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvoiceEmailRetryJob {
    private final InvoiceEmailRetryService retries;

    public InvoiceEmailRetryJob(InvoiceEmailRetryService retries) { this.retries = retries; }

    @Scheduled(fixedDelayString = "${app.invoice.retry-scan-ms:60000}")
    public void retryFailedInvoices() {
        retries.findDueIds().forEach(retries::retry);
    }
}
