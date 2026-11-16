package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class InvoiceEmailSenderTest {
    @Test void resendUsesHttpsAuthorizationAndStableIdempotencyKeyWithoutSmtp() {
        var smtp = mock(JavaMailSender.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var sender = new InvoiceEmailSender(smtp, "resend", "GreenFarm <billing@example.com>", "test-key", builder.build());
        server.expect(requestTo("https://api.resend.com/emails")).andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(header("Idempotency-Key", "greenfarm-invoice/42"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {"from":"GreenFarm <billing@example.com>","to":["customer@example.com"],"subject":"Invoice #42","html":"<p>80.000 đ</p>"}
                """))
            .andRespond(withSuccess("{\"id\":\"accepted-id\"}", MediaType.APPLICATION_JSON));
        sender.send("customer@example.com", "Invoice #42", "<p>80.000 đ</p>", "greenfarm-invoice/42");
        server.verify();
        verifyNoInteractions(smtp);
    }

    @Test void rejectedEmailDoesNotExposeTheProviderBodyInTheFailure() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var sender = new InvoiceEmailSender(null, "resend", "billing@example.com", "test-key", builder.build());
        server.expect(requestTo("https://api.resend.com/emails"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN).body("private provider details"));
        assertThatThrownBy(() -> sender.send("customer@example.com", "Invoice", "<p>Invoice</p>", "invoice/42"))
            .isInstanceOf(MailSendException.class).hasMessage("Resend rejected invoice email (HTTP 403)")
            .hasMessageNotContaining("private provider details").hasMessageNotContaining("test-key");
        server.verify();
    }

    @Test void successWithoutAcknowledgementIsNotTreatedAsSent() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var sender = new InvoiceEmailSender(null, "resend", "billing@example.com", "test-key", builder.build());
        server.expect(requestTo("https://api.resend.com/emails"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> sender.send("customer@example.com", "Invoice", "<p>Invoice</p>", "invoice/42"))
            .isInstanceOf(MailSendException.class).hasMessageContaining("did not acknowledge");
        server.verify();
    }

    @Test void missingKeyOrFromDoesNotFallBackToSmtp() {
        var smtp = mock(JavaMailSender.class);
        var missingKey = new InvoiceEmailSender(smtp, "resend", "billing@example.com", "");
        var missingFrom = new InvoiceEmailSender(smtp, "resend", "", "test-key");
        assertThat(missingKey.configured()).isFalse();
        assertThat(missingFrom.configured()).isFalse();
        assertThatThrownBy(() -> missingKey.send("customer@example.com", "Invoice", "Invoice", "invoice/42"))
            .isInstanceOf(MailSendException.class);
        verifyNoInteractions(smtp);
    }

    @Test void smtpStillSendsUtf8HtmlForLocalDevelopment() throws Exception {
        var smtp = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(smtp.createMimeMessage()).thenReturn(message);
        var sender = new InvoiceEmailSender(smtp, "smtp", "billing@example.com", "");
        sender.send("customer@example.com", "Hóa đơn", "<p>Cà chua</p>", "invoice/42");
        verify(smtp).send(message);
        assertThat(message.getSubject()).isEqualTo("Hóa đơn");
        assertThat(message.getContent().toString()).contains("Cà chua");
    }
}
