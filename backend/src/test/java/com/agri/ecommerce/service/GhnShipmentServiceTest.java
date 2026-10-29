package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;

class GhnShipmentServiceTest {
    private final OrderRepository orders=mock(OrderRepository.class);
    private final ShipmentRepository shipments=mock(ShipmentRepository.class);
    private final ShipmentEventRepository events=mock(ShipmentEventRepository.class);
    private final PaymentRepository payments=mock(PaymentRepository.class);
    private final GhnShippingService ghn=mock(GhnShippingService.class);
    private final OrderLifecycleService lifecycle=mock(OrderLifecycleService.class);
    private final ObjectMapper json=new ObjectMapper();
    private GhnShipmentService service;
    private Order order;
    private Shipment shipment;

    @BeforeEach void setUp() {
        service=new GhnShipmentService(orders,shipments,events,payments,ghn,lifecycle);
        order=new Order(); ReflectionTestUtils.setField(order,"id",31L);
        order.setShippingProvider("ghn");order.setStatus(OrderStatus.READY_FOR_DELIVERY);
        order.setTotalPrice(new BigDecimal("120900"));
        shipment=new Shipment(); ReflectionTestUtils.setField(shipment,"id",40L);
        shipment.setOrder(order);shipment.setClientOrderCode("GF-31");
        shipment.setCarrierShippingFee(new BigDecimal("20900"));
        shipment.setCodAmount(new BigDecimal("120900"));
        lenient().when(orders.findByIdForUpdate(31L)).thenReturn(Optional.of(order));
        lenient().when(shipments.findLockedByOrder_Id(31L)).thenReturn(Optional.of(shipment));
        lenient().when(shipments.save(any(Shipment.class))).thenAnswer(call->call.getArgument(0));
    }

    @Test void createAttachesProviderCodeAfterFeeCheck() throws Exception {
        Payment payment=new Payment();payment.setPaymentMethod(PaymentMethod.COD);payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        when(ghn.previewOrder(order,new BigDecimal("120900")))
            .thenReturn(new GhnShippingService.Quote(new BigDecimal("20900"),2,null));
        when(ghn.detailByClientCode("GF-31")).thenReturn(json.readTree("{}"));
        when(ghn.create(order,"GF-31",new BigDecimal("120900")))
            .thenReturn(new GhnShippingService.Created("LADFYR",new BigDecimal("20900"),null));
        var result=service.create(31L);
        assertThat(result.providerOrderCode()).isEqualTo("LADFYR");
        verify(ghn).create(order,"GF-31",new BigDecimal("120900"));
        verify(events).save(any(ShipmentEvent.class));
    }

    @Test void repeatedCreateNeverCallsGhnAgain() {
        shipment.setProviderOrderCode("LADFYR");
        Payment payment=new Payment();payment.setPaymentMethod(PaymentMethod.COD);payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        assertThat(service.create(31L).providerOrderCode()).isEqualTo("LADFYR");
        verifyNoInteractions(ghn);
    }

    @Test void recoversExistingShipmentWithoutRequiringANewQuote() throws Exception {
        Payment payment=new Payment();payment.setPaymentMethod(PaymentMethod.COD);payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        when(ghn.detailByClientCode("GF-31")).thenReturn(json.readTree("{\"order_code\":\"LADFYR\"}"));
        assertThat(service.create(31L).providerOrderCode()).isEqualTo("LADFYR");
        verify(ghn,never()).previewOrder(any(),any());
        verify(ghn,never()).create(any(),any(),any());
    }

    @Test void rejectedLookupDoesNotCreateShipment() {
        Payment payment=new Payment();payment.setPaymentMethod(PaymentMethod.COD);payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        when(ghn.detailByClientCode("GF-31")).thenThrow(new ApplicationException(HttpStatus.BAD_REQUEST,"GHN_REQUEST_REJECTED","GHN từ chối"));
        assertThatThrownBy(()->service.create(31L)).hasMessageContaining("GHN");
        verify(ghn,never()).create(any(),any(),any());
    }

    @Test void onlinePaymentMustBeCompletedBeforeCreatingShipment() {
        Payment payment=new Payment();payment.setPaymentMethod(PaymentMethod.VNPAY);payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        assertThatThrownBy(()->service.create(31L)).hasMessageContaining("thanh toán");
        verifyNoInteractions(ghn);
    }

    @Test void syncUpdatesOrderLifecycleAndKeepsCodUnpaid() throws Exception {
        shipment.setProviderOrderCode("LADFYR");
        when(ghn.detail("LADFYR")).thenReturn(json.readTree("{\"status\":\"delivered\",\"updated_date\":\"2026-10-20T03:00:00Z\"}"));
        service.sync(31L);
        assertThat(shipment.getShippingStatus()).isEqualTo("delivered");
        verify(lifecycle).updateFromGhn(31L,"delivered");
        verifyNoInteractions(payments);
    }

    @Test void staleSyncDoesNotReplaceNewerWebhookStatus() throws Exception {
        shipment.setProviderOrderCode("LADFYR");
        shipment.setShippingStatus("delivered");
        shipment.setLastEventAt(java.time.LocalDateTime.of(2026,10,20,10,0));
        when(ghn.detail("LADFYR")).thenReturn(json.readTree("{\"status\":\"picking\",\"updated_date\":\"2026-10-20T02:00:00Z\"}"));
        service.sync(31L);
        assertThat(shipment.getShippingStatus()).isEqualTo("delivered");
        verify(lifecycle,never()).updateFromGhn(any(),any());
    }

    @Test void syncReconcilesTransferredCodWhenWebhookWasMissed() throws Exception {
        shipment.setProviderOrderCode("LADFYR");
        order.setStatus(OrderStatus.DELIVERED);
        Payment payment=new Payment(); payment.setPaymentMethod(PaymentMethod.COD); payment.setStatus(PaymentStatus.PENDING);
        when(orders.findById(31L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        when(ghn.detail("LADFYR")).thenReturn(json.readTree("{\"status\":\"delivered\",\"updated_date\":\"2026-10-20T03:00:00Z\",\"is_cod_transferred\":true,\"cod_amount\":120900,\"cod_transfer_date\":\"2026-10-20T02:30:00Z\"}"));
        service.sync(31L);
        assertThat(shipment.getCodTransferredAt()).isEqualTo(java.time.LocalDateTime.of(2026,10,20,9,30));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(payments).save(payment);
    }

    @Test void syncDoesNotReconcileWrongCodAmount() throws Exception {
        shipment.setProviderOrderCode("LADFYR");
        when(ghn.detail("LADFYR")).thenReturn(json.readTree("{\"status\":\"delivered\",\"is_cod_transferred\":true,\"cod_amount\":120000,\"cod_transfer_date\":\"2026-10-20T02:30:00Z\"}"));
        service.sync(31L);
        assertThat(shipment.getCodTransferredAt()).isNull();
        verifyNoInteractions(payments);
    }

    @Test void cancelRequiresGhnConfirmation() {
        shipment.setProviderOrderCode("LADFYR");
        when(ghn.cancel("LADFYR")).thenReturn(false);
        assertThatThrownBy(()->service.cancel(31L)).hasMessageContaining("GHN");
        assertThat(shipment.getShippingStatus()).isNotEqualTo("cancel");
    }

    @Test void timeoutWithoutProviderCodeRetainsErrorForAdminAndAllowsRetry() throws Exception {
        Payment payment=new Payment();payment.setPaymentMethod(PaymentMethod.COD);payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByOrder_Id(31L)).thenReturn(Optional.of(payment));
        when(ghn.previewOrder(order,new BigDecimal("120900")))
            .thenReturn(new GhnShippingService.Quote(new BigDecimal("20900"),2,null));
        when(ghn.detailByClientCode("GF-31")).thenReturn(json.readTree("{}"))
            .thenThrow(new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE,"GHN_UNAVAILABLE","Timeout GHN"));
        when(ghn.create(order,"GF-31",new BigDecimal("120900")))
            .thenThrow(new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE,"GHN_UNAVAILABLE","Timeout GHN"));
        assertThatThrownBy(()->service.create(31L)).hasMessageContaining("Timeout GHN");
        assertThat(shipment.getProviderOrderCode()).isNull();
        assertThat(shipment.getLastError()).isEqualTo("Timeout GHN");
        verify(shipments).save(shipment);
    }
}
