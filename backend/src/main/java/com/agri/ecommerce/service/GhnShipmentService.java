package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.ShipmentResponse;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GhnShipmentService {
    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final ShipmentEventRepository events;
    private final PaymentRepository payments;
    private final GhnShippingService ghn;
    private final OrderLifecycleService lifecycle;

    public GhnShipmentService(OrderRepository orders, ShipmentRepository shipments, ShipmentEventRepository events,
                              PaymentRepository payments, GhnShippingService ghn, OrderLifecycleService lifecycle) {
        this.orders=orders; this.shipments=shipments; this.events=events; this.payments=payments; this.ghn=ghn;
        this.lifecycle=lifecycle;
    }

    @Transactional(noRollbackFor=ApplicationException.class)
    public ShipmentResponse create(Long orderId) {
        Order order=orders.findByIdForUpdate(orderId).orElseThrow(this::notFound);
        Shipment shipment=required(orderId);
        if(order.getStatus()!=OrderStatus.READY_FOR_DELIVERY)
            throw conflict("GHN_ORDER_NOT_READY","Đơn phải được chuẩn bị xong trước khi tạo vận đơn GHN");
        Payment payment=payments.findByOrder_Id(orderId).orElseThrow(()->conflict("PAYMENT_NOT_FOUND","Đơn chưa có giao dịch thanh toán"));
        if(payment.getPaymentMethod()!=PaymentMethod.COD && payment.getStatus()!=PaymentStatus.COMPLETED)
            throw conflict("PAYMENT_NOT_COMPLETED","Đơn thanh toán trực tuyến chưa thành công");
        if(shipment.getProviderOrderCode()!=null) return response(shipment);
        BigDecimal cod=payment.getPaymentMethod()==PaymentMethod.COD?order.getTotalPrice():BigDecimal.ZERO;
        if(shipment.getCodAmount().compareTo(cod)!=0)
            throw conflict("GHN_COD_MISMATCH","Số tiền COD đã thay đổi, cần kiểm tra lại đơn");
        GhnShippingService.Quote quote=ghn.previewOrder(order,cod);
        if(quote.fee().compareTo(shipment.getCarrierShippingFee())!=0)
            throw conflict("GHN_FEE_CHANGED","Phí GHN đã thay đổi sau khi khách đặt hàng; cần xử lý trước khi tạo vận đơn");
        JsonNode existing=null;
        try { existing=ghn.detailByClientCode(shipment.getClientOrderCode()); }
        catch(ApplicationException exception) {
            if(!"GHN_REQUEST_REJECTED".equals(exception.getCode())) throw exception;
        }
        if(existing!=null && !existing.path("order_code").asText().isBlank()) {
            attach(shipment,existing.path("order_code").asText(),quote.fee(),"Đã khôi phục vận đơn GHN");
            return response(shipment);
        }
        try {
            GhnShippingService.Created created=ghn.create(order,shipment.getClientOrderCode(),cod);
            attach(shipment,created.orderCode(),created.fee(),"Đã tạo vận đơn GHN");
            shipment.setEstimatedDeliveryAt(created.estimatedDeliveryAt());
            return response(shipments.save(shipment));
        } catch(ApplicationException exception) {
            if("GHN_UNAVAILABLE".equals(exception.getCode())) {
                try {
                    JsonNode found=ghn.detailByClientCode(shipment.getClientOrderCode());
                    if(!found.path("order_code").asText().isBlank()) {
                        attach(shipment,found.path("order_code").asText(),quote.fee(),"Đã khôi phục vận đơn sau timeout");
                        return response(shipment);
                    }
                } catch(ApplicationException ignored) { /* GHN may not have created the order. */ }
            }
            shipment.setLastError(exception.getMessage());
            shipments.save(shipment);
            throw exception;
        }
    }

    @Transactional
    public ShipmentResponse sync(Long orderId) {
        orders.findByIdForUpdate(orderId).orElseThrow(this::notFound);
        Shipment shipment=required(orderId);
        if(shipment.getProviderOrderCode()==null) throw conflict("GHN_SHIPMENT_NOT_CREATED","Chưa tạo vận đơn GHN");
        JsonNode data=ghn.detail(shipment.getProviderOrderCode());
        String status=data.path("status").asText();
        if(!status.isBlank() && !"cancel".equals(shipment.getShippingStatus())) {
            shipment.setShippingStatus(status); shipments.save(shipment);
            addEvent(shipment,"sync:"+status+":"+data.path("updated_date").asText(),"sync",status,"Đồng bộ từ GHN");
            lifecycle.updateFromGhn(orderId,status);
        }
        reconcileCod(shipment,orderId);
        return response(shipment);
    }

    @Transactional
    public ShipmentResponse cancel(Long orderId) {
        Order order=orders.findByIdForUpdate(orderId).orElseThrow(this::notFound);
        Shipment shipment=required(orderId);
        if(shipment.getProviderOrderCode()==null) throw conflict("GHN_SHIPMENT_NOT_CREATED","Chưa tạo vận đơn GHN");
        if("cancel".equals(shipment.getShippingStatus())) return response(shipment);
        if(order.getStatus()!=OrderStatus.READY_FOR_DELIVERY && order.getStatus()!=OrderStatus.DELIVERY_FAILED)
            throw conflict("GHN_CANCEL_ORDER_STATUS","Chỉ hủy vận đơn khi đơn GreenFarm còn chờ giao hoặc giao thất bại");
        if(!ghn.cancel(shipment.getProviderOrderCode()))
            throw conflict("GHN_CANCEL_REJECTED","GHN không cho phép hủy vận đơn ở trạng thái hiện tại");
        shipment.setShippingStatus("cancel"); shipment.setLastError(null); shipments.save(shipment);
        addEvent(shipment,"admin:cancel","cancel","cancel","Admin đã hủy vận đơn GHN");
        return response(shipment);
    }

    @Transactional
    public String printUrl(Long orderId) {
        Shipment shipment=required(orderId);
        if(shipment.getProviderOrderCode()==null) throw conflict("GHN_SHIPMENT_NOT_CREATED","Chưa tạo vận đơn GHN");
        return ghn.printUrl(shipment.getProviderOrderCode());
    }

    @Transactional(readOnly=true)
    public java.util.List<com.agri.ecommerce.dto.response.ShipmentEventResponse> events(Long orderId) {
        Shipment shipment=shipments.findByOrder_Id(orderId)
            .orElseThrow(()->conflict("GHN_SHIPMENT_NOT_FOUND","Đơn này không chọn GHN"));
        return events.findAllByShipment_IdOrderByEventAtAscIdAsc(shipment.getId()).stream()
            .map(item->new com.agri.ecommerce.dto.response.ShipmentEventResponse(
                item.getId(),item.getEventType(),item.getShippingStatus(),item.getEventAt(),
                item.getCreatedAt(),item.getDescription())).toList();
    }

    private void reconcileCod(Shipment shipment,Long orderId) {
        if(shipment.getCodTransferredAt()==null) return;
        Order order=orders.findById(orderId).orElseThrow(this::notFound);
        if(order.getStatus()!=OrderStatus.DELIVERED && order.getStatus()!=OrderStatus.COMPLETED) return;
        Payment payment=payments.findByOrder_Id(orderId).orElse(null);
        if(payment!=null && payment.getPaymentMethod()==PaymentMethod.COD && payment.getStatus()==PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(shipment.getCodTransferredAt());
            payment.setGatewayResponseCode("GHN_COD_TRANSFERRED");
            payments.save(payment);
        }
    }

    private Shipment required(Long orderId) {
        return shipments.findLockedByOrder_Id(orderId).orElseThrow(()->conflict("GHN_SHIPMENT_NOT_FOUND","Đơn này không chọn GHN"));
    }
    private void attach(Shipment shipment,String code,BigDecimal fee,String note) {
        shipment.setProviderOrderCode(code); shipment.setCarrierShippingFee(fee);
        if("not_created".equals(shipment.getShippingStatus())) shipment.setShippingStatus("created");
        shipment.setLastError(null); shipments.save(shipment);
        addEvent(shipment,"created:"+code,"create","created",note);
    }
    private void addEvent(Shipment shipment,String key,String type,String status,String note) {
        if(events.existsByShipment_IdAndEventKey(shipment.getId(),key)) return;
        ShipmentEvent event=new ShipmentEvent(); event.setShipment(shipment); event.setEventKey(key);
        event.setEventType(type); event.setShippingStatus(status); event.setDescription(note); events.save(event);
    }
    private ShipmentResponse response(Shipment item) {
        return new ShipmentResponse(item.getProvider(),item.getProviderOrderCode(),item.getShippingStatus(),
            item.getCustomerShippingFee(),item.getCarrierShippingFee(),item.getCodAmount(),
            item.getEstimatedDeliveryAt(),item.getCodTransferredAt(),item.getCreatedAt(),item.getLastError());
    }
    private ApplicationException notFound(){return new ApplicationException(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND","Không tìm thấy đơn hàng");}
    private ApplicationException conflict(String code,String message){return new ApplicationException(HttpStatus.CONFLICT,code,message);}
}
