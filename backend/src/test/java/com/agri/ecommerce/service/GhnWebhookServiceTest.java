package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.config.GhnProperties;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GhnWebhookServiceTest {
    private final ShipmentRepository shipments=mock(ShipmentRepository.class);
    private final ShipmentEventRepository events=mock(ShipmentEventRepository.class);
    private final OrderRepository orders=mock(OrderRepository.class);
    private final PaymentRepository payments=mock(PaymentRepository.class);
    private final OrderLifecycleService lifecycle=mock(OrderLifecycleService.class);
    private final ObjectMapper json=new ObjectMapper();
    private GhnWebhookService service;
    private Order order;
    private Shipment shipment;
    private Payment payment;

    @BeforeEach void setUp() {
        service=new GhnWebhookService(new GhnProperties(true,"https://dev-online-gateway.ghn.vn",
            "test-token",123,"test-secret",1,"CHOXEMHANGKHONGTHU"),shipments,events,orders,payments,lifecycle);
        order=new Order(); ReflectionTestUtils.setField(order,"id",21L);
        order.setShippingProvider("ghn"); order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        shipment=new Shipment(); ReflectionTestUtils.setField(shipment,"id",30L);
        shipment.setOrder(order); shipment.setShopId(123); shipment.setClientOrderCode("GF-21");
        shipment.setProviderOrderCode("LADFYR"); shipment.setCodAmount(new BigDecimal("120900"));
        payment=new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING);
        lenient().when(shipments.findByProviderOrderCode("LADFYR")).thenReturn(Optional.of(shipment));
        lenient().when(orders.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        lenient().when(orders.findById(21L)).thenReturn(Optional.of(order));
        lenient().when(shipments.findLockedByOrder_Id(21L)).thenReturn(Optional.of(shipment));
        lenient().when(payments.findByOrder_Id(21L)).thenReturn(Optional.of(payment));
    }

    @Test void rejectsWrongSecretBeforeReadingShipment() throws Exception {
        assertThatThrownBy(()->service.receive("wrong",body("switch_status","delivered","2026-10-20T02:00:00Z",null)))
            .hasMessageContaining("GHN");
        verifyNoInteractions(shipments,events,orders,payments,lifecycle);
    }

    @Test void duplicateWebhookIsIdempotent() throws Exception {
        when(events.existsByShipment_IdAndEventKey(eq(30L),any())).thenReturn(true);
        service.receive("test-secret",body("switch_status","delivered","2026-10-20T02:00:00Z",null));
        verify(events,never()).save(any());
        verifyNoInteractions(lifecycle,payments);
    }

    @Test void oldEventCannotOverwriteNewShippingStatus() throws Exception {
        shipment.setShippingStatus("delivered");
        shipment.setLastEventAt(java.time.LocalDateTime.of(2026,10,20,10,0));
        service.receive("test-secret",body("switch_status","picking","2026-10-20T02:00:00Z",null));
        assertThat(shipment.getShippingStatus()).isEqualTo("delivered");
        verify(lifecycle,never()).updateFromGhn(any(),any());
        verify(events).save(any());
    }

    @Test void codTransferBeforeDeliveryCompletesOnlyAfterDeliveryEvent() throws Exception {
        service.receive("test-secret",body("cod","delivering","2026-10-20T02:00:00Z","2026-10-20T02:01:00Z"));
        assertThat(shipment.getCodTransferredAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        doAnswer(call->{order.setStatus(OrderStatus.DELIVERED);return null;})
            .when(lifecycle).updateFromGhn(21L,"delivered");
        service.receive("test-secret",body("switch_status","delivered","2026-10-20T02:02:00Z",null));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getGatewayResponseCode()).isEqualTo("GHN_COD_TRANSFERRED");
        verify(payments).save(payment);
    }

    @Test void incorrectCodAmountNeverCompletesPayment() throws Exception {
        order.setStatus(OrderStatus.DELIVERED);
        var incorrect=(com.fasterxml.jackson.databind.node.ObjectNode)
            body("cod","delivered","2026-10-20T02:00:00Z","2026-10-20T02:01:00Z");
        incorrect.put("CODAmount",120000);
        service.receive("test-secret",incorrect);
        assertThat(shipment.getCodTransferredAt()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(payments,never()).save(any());
    }

    @Test void laterCodEventDoesNotBlockEarlierDeliveryStatusArrivingAfterward() throws Exception {
        service.receive("test-secret",body("cod","delivering","2026-10-20T03:00:00Z","2026-10-20T03:01:00Z"));
        doAnswer(call->{order.setStatus(OrderStatus.DELIVERED);return null;})
            .when(lifecycle).updateFromGhn(21L,"delivered");
        service.receive("test-secret",body("switch_status","delivered","2026-10-20T02:00:00Z",null));
        assertThat(shipment.getShippingStatus()).isEqualTo("delivered");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    private JsonNode body(String type,String status,String time,String transfer) throws Exception {
        var node=json.createObjectNode();
        node.put("ShopID",123);node.put("OrderCode","LADFYR");node.put("ClientOrderCode","GF-21");
        node.put("Type",type);node.put("Status",status);node.put("Time",time);node.put("CODAmount",120900);
        if(transfer!=null)node.put("CODTransferDate",transfer);
        return node;
    }
}
