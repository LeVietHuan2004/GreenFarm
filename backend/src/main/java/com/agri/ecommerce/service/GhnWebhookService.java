package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.config.GhnProperties;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GhnWebhookService {
    private final GhnProperties properties;
    private final ShipmentRepository shipments;
    private final ShipmentEventRepository events;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final OrderLifecycleService lifecycle;

    public GhnWebhookService(GhnProperties properties, ShipmentRepository shipments, ShipmentEventRepository events,
                             OrderRepository orders, PaymentRepository payments, OrderLifecycleService lifecycle) {
        this.properties=properties; this.shipments=shipments; this.events=events;
        this.orders=orders; this.payments=payments; this.lifecycle=lifecycle;
    }

    @Transactional
    public void receive(String secret, JsonNode body) {
        properties.requireEnabled();
        if(properties.webhookSecret().isBlank() || secret==null
            || !MessageDigest.isEqual(properties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                                      secret.getBytes(StandardCharsets.UTF_8)))
            throw new ApplicationException(HttpStatus.UNAUTHORIZED,"GHN_WEBHOOK_UNAUTHORIZED","Webhook GHN không hợp lệ");
        if(body==null || body.path("ShopID").asInt()!=properties.shopId())
            throw new ApplicationException(HttpStatus.BAD_REQUEST,"GHN_WEBHOOK_SHOP_MISMATCH","Shop GHN không hợp lệ");
        String code=body.path("OrderCode").asText();
        String clientCode=body.path("ClientOrderCode").asText();
        String type=body.path("Type").asText();
        String status=body.path("Status").asText();
        String rawTime=body.path("Time").asText();
        if(code.isBlank() || code.length()>64 || type.isBlank() || type.length()>50 || rawTime.isBlank())
            throw new ApplicationException(HttpStatus.BAD_REQUEST,"GHN_WEBHOOK_INVALID","Nội dung webhook GHN không hợp lệ");
        LocalDateTime eventAt;
        try {eventAt=OffsetDateTime.parse(rawTime).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();}
        catch(RuntimeException exception){throw new ApplicationException(HttpStatus.BAD_REQUEST,"GHN_WEBHOOK_TIME_INVALID","Thời gian webhook GHN không hợp lệ");}
        Shipment candidate=shipments.findByProviderOrderCode(code).orElseGet(()->shipments.findByClientOrderCode(clientCode)
            .orElseThrow(()->new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE,"GHN_SHIPMENT_PENDING","Chưa tìm thấy vận đơn GHN, vui lòng gửi lại")));
        orders.findByIdForUpdate(candidate.getOrder().getId()).orElseThrow(()->new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE,"GHN_ORDER_PENDING","Chưa tìm thấy đơn GHN"));
        Shipment shipment=shipments.findLockedByOrder_Id(candidate.getOrder().getId()).orElseThrow();
        if(shipment.getShopId()!=properties.shopId() || !shipment.getClientOrderCode().equals(clientCode)
            || (shipment.getProviderOrderCode()!=null && !shipment.getProviderOrderCode().equals(code)))
            throw new ApplicationException(HttpStatus.BAD_REQUEST,"GHN_WEBHOOK_ORDER_MISMATCH","Mã vận đơn GHN không khớp");
        if(shipment.getProviderOrderCode()==null) shipment.setProviderOrderCode(code);
        String key=sha256(code+"|"+type+"|"+rawTime);
        if(events.existsByShipment_IdAndEventKey(shipment.getId(),key)) return;
        ShipmentEvent event=new ShipmentEvent(); event.setShipment(shipment); event.setEventKey(key);
        event.setEventType(type); event.setShippingStatus(status); event.setEventAt(eventAt);
        event.setDescription(body.path("Description").asText().substring(0,Math.min(1000,body.path("Description").asText().length())));
        events.save(event);
        if("cod".equals(type) && body.path("CODAmount").isNumber()
            && body.path("CODAmount").decimalValue().compareTo(shipment.getCodAmount())==0) {
            String transfer=body.path("CODTransferDate").asText();
            if(!transfer.isBlank()) {
                try {shipment.setCodTransferredAt(OffsetDateTime.parse(transfer)
                    .atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime());}
                catch(RuntimeException ignored) { /* Keep COD pending until a valid transfer time arrives. */ }
            }
        }
        boolean fresh="switch_status".equals(type)
            && (shipment.getLastEventAt()==null || !eventAt.isBefore(shipment.getLastEventAt()));
        if(fresh) {
            shipment.setLastEventAt(eventAt);
            if(!status.isBlank()) shipment.setShippingStatus(status);
        }
        shipments.save(shipment);
        if(fresh) lifecycle.updateFromGhn(candidate.getOrder().getId(),status);
        reconcileCod(shipment,candidate.getOrder().getId());
    }

    private void reconcileCod(Shipment shipment, Long orderId) {
        if(shipment.getCodTransferredAt()==null) return;
        Order order=orders.findById(orderId).orElseThrow();
        if(order.getStatus()!=OrderStatus.DELIVERED && order.getStatus()!=OrderStatus.COMPLETED) return;
        Payment payment=payments.findByOrder_Id(orderId).orElse(null);
        if(payment!=null && payment.getPaymentMethod()==PaymentMethod.COD && payment.getStatus()==PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(shipment.getCodTransferredAt());
            payment.setGatewayResponseCode("GHN_COD_TRANSFERRED");
            payments.save(payment);
        }
    }

    private String sha256(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException exception){throw new IllegalStateException(exception);}
    }
}
