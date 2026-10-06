package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.RefundRequestResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundRequestService {
    private final RefundRequestRepository requests;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final OrderLifecycleService lifecycle;
    private final VnpayRefundService vnpay;
    private final NotificationService notifications;

    public RefundRequestService(RefundRequestRepository requests, OrderRepository orders, PaymentRepository payments,
                                OrderLifecycleService lifecycle, VnpayRefundService vnpay, NotificationService notifications) {
        this.requests=requests; this.orders=orders; this.payments=payments; this.lifecycle=lifecycle; this.vnpay=vnpay; this.notifications=notifications;
    }

    @Transactional
    public RefundRequestResponse create(Long userId, Long orderId, String reason, String details) {
        Order order=orders.findByIdAndUserIdForUpdate(orderId,userId).orElseThrow(this::notFound);
        Payment payment=payments.findByOrder_Id(orderId).orElseThrow(() -> conflict("PAYMENT_NOT_FOUND", "Không tìm thấy giao dịch thanh toán"));
        if (order.getStatus()==OrderStatus.CANCELED || payment.getStatus()!=PaymentStatus.COMPLETED)
            throw conflict("REFUND_REQUEST_NOT_ALLOWED", "Chỉ có thể yêu cầu hoàn tiền cho đơn đã thanh toán và chưa hủy");
        if (requests.existsByOrder_IdAndStatusIn(orderId, List.of(RefundRequestStatus.PENDING, RefundRequestStatus.PROCESSING)))
            throw conflict("REFUND_REQUEST_EXISTS", "Đơn hàng đã có yêu cầu hoàn tiền đang được xử lý");
        RefundRequest request=new RefundRequest(); request.setOrder(order); request.setReason(reason.trim()); request.setDetails(blankToNull(details)); request.setStatus(RefundRequestStatus.PENDING);
        RefundRequest saved=requests.save(request);
        notifications.notifyRole("admin", "refund", "Có yêu cầu hoàn tiền mới cho đơn #"+orderId, "/admin/orders");
        notifications.notifyUser(order.getUser(), "refund", "Yêu cầu hoàn tiền cho đơn #"+orderId+" đã được ghi nhận", "/orders/"+orderId);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public RefundRequestResponse latestForCustomer(Long userId, Long orderId) {
        Order order=orders.findByIdAndUser_Id(orderId,userId).orElseThrow(this::notFound);
        return requests.findFirstByOrder_IdOrderByIdDesc(order.getId()).map(this::response).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundRequestResponse> findAll(String status, Pageable pageable) {
        var page=status==null||status.isBlank()?requests.findAllByOrderByCreatedAtDesc(pageable):requests.findAllByStatusOrderByCreatedAtDesc(parseStatus(status),pageable);
        return PageResponse.from(page, this::response);
    }

    @Transactional
    public RefundRequestResponse approve(Long requestId, String note, String actor, String ip) {
        RefundRequest request=requests.findByIdForUpdate(requestId).orElseThrow(this::notFound);
        if (request.getStatus()!=RefundRequestStatus.PENDING) throw conflict("REFUND_REQUEST_NOT_PENDING", "Yêu cầu hoàn tiền này đã được xử lý");
        request.setStatus(RefundRequestStatus.PROCESSING); request.setAdminNote(blankToNull(note)); request.setReviewedBy(actor); request.setReviewedAt(LocalDateTime.now()); requests.save(request);
        VnpayRefundService.RefundOutcome outcome=vnpay.refund(request.getOrder().getId(), null, actor, ip);
        if (outcome.processing()) return response(request);
        if (outcome.fullSuccess()) lifecycle.confirmRefundAndCancel(request.getOrder().getId(), note);
        request.setStatus(RefundRequestStatus.APPROVED); requests.save(request);
        notifications.notifyUser(request.getOrder().getUser(), "refund", "Yêu cầu hoàn tiền cho đơn #"+request.getOrder().getId()+" đã được duyệt", "/orders/"+request.getOrder().getId());
        return response(request);
    }

    @Transactional
    public RefundRequestResponse reject(Long requestId, String note, String actor) {
        RefundRequest request=requests.findByIdForUpdate(requestId).orElseThrow(this::notFound);
        if (request.getStatus()!=RefundRequestStatus.PENDING) throw conflict("REFUND_REQUEST_NOT_PENDING", "Yêu cầu hoàn tiền này đã được xử lý");
        request.setStatus(RefundRequestStatus.REJECTED); request.setAdminNote(blankToNull(note)); request.setReviewedBy(actor); request.setReviewedAt(LocalDateTime.now()); requests.save(request);
        notifications.notifyUser(request.getOrder().getUser(), "refund", "Yêu cầu hoàn tiền cho đơn #"+request.getOrder().getId()+" đã bị từ chối", "/orders/"+request.getOrder().getId());
        return response(request);
    }

    private RefundRequestResponse response(RefundRequest request) { Order o=request.getOrder(); return new RefundRequestResponse(request.getId(),o.getId(),o.getStatus().getValue(),o.getUser()==null?o.getRecipientName():o.getUser().getName(),request.getReason(),request.getDetails(),request.getStatus().name().toLowerCase(),request.getAdminNote(),request.getReviewedBy(),request.getReviewedAt(),request.getCreatedAt()); }
    private RefundRequestStatus parseStatus(String value){try{return RefundRequestStatus.valueOf(value.trim().toUpperCase());}catch(IllegalArgumentException e){throw new ApplicationException(HttpStatus.BAD_REQUEST,"INVALID_REFUND_REQUEST_STATUS","Trạng thái yêu cầu hoàn tiền không hợp lệ");}}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private ApplicationException notFound(){return new ApplicationException(HttpStatus.NOT_FOUND,"REFUND_REQUEST_NOT_FOUND","Không tìm thấy yêu cầu hoàn tiền");}
    private ApplicationException conflict(String code,String message){return new ApplicationException(HttpStatus.CONFLICT,code,message);}
}
