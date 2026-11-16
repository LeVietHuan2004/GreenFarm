package com.agri.ecommerce.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** SMTP for local/compatible hosts; Resend's HTTPS API for Railway Trial/Hobby. */
@Component
public class InvoiceEmailSender {
    private final JavaMailSender smtp;
    private final String provider;
    private final String from;
    private final String apiKey;
    private final RestClient http;

    @Autowired
    public InvoiceEmailSender(@Nullable JavaMailSender smtp,
            @Value("${app.invoice.provider:smtp}") String provider,
            @Value("${app.invoice.from:}") String from,
            @Value("${app.invoice.resend-api-key:}") String apiKey) {
        this(smtp, provider, from, apiKey, client());
    }

    InvoiceEmailSender(JavaMailSender smtp, String provider, String from, String apiKey, RestClient http) {
        this.smtp = smtp;
        this.provider = provider.trim().toLowerCase(Locale.ROOT);
        if (!List.of("smtp", "resend").contains(this.provider))
            throw new IllegalArgumentException("INVOICE_EMAIL_PROVIDER must be smtp or resend");
        this.from = from.trim();
        this.apiKey = apiKey.trim();
        this.http = http;
    }

    private static RestClient client() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    public boolean configured() {
        return "resend".equals(provider) ? StringUtils.hasText(apiKey) && StringUtils.hasText(from) : smtp != null;
    }

    public void send(String recipient, String subject, String html, String idempotencyKey) {
        if (!configured()) throw new MailSendException("Invoice email provider is not configured");
        if ("resend".equals(provider)) {
            sendWithResend(recipient, subject, html, idempotencyKey);
            return;
        }
        try {
            var message = smtp.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            if (StringUtils.hasText(from)) helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            smtp.send(message);
        } catch (jakarta.mail.MessagingException exception) {
            throw new MailSendException("Unable to create invoice email");
        }
    }

    private void sendWithResend(String recipient, String subject, String html, String idempotencyKey) {
        try {
            Map<?, ?> result = http.post().uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("from", from, "to", List.of(recipient), "subject", subject, "html", html))
                .retrieve().body(Map.class);
            if (result == null || !(result.get("id") instanceof String id) || id.isBlank())
                throw new MailSendException("Resend did not acknowledge the invoice email");
        } catch (RestClientResponseException exception) {
            // Do not persist/log provider response bodies or credentials.
            throw new MailSendException("Resend rejected invoice email (HTTP " + exception.getStatusCode().value() + ")");
        } catch (RestClientException exception) {
            throw new MailSendException("Unable to connect to Resend email API");
        }
    }
}
