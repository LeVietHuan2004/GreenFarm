package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderLifecycleService {
    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELED),
        OrderStatus.PROCESSING, EnumSet.of(OrderStatus.READY_FOR_DELIVERY, OrderStatus.CANCELED),
        OrderStatus.READY_FOR_DELIVERY, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELED),
        OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED, EnumSet.of(OrderStatus.COMPLETED),
        OrderStatus.DELIVERY_FAILED, EnumSet.of(OrderStatus.READY_FOR_DELIVERY, OrderStatus.CANCELED),
        OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orders;
    private final OrderStatusHistoryRepository histories;
    private final PaymentRepository payments;
    private final ProductRepository products;
    private final CouponEngineService couponEngine;
    private final UserRepository users;
    private final NotificationService notifications;
    private final LoyaltyService loyalty;

    public OrderLifecycleService(
        OrderRepository orders,
        OrderStatusHistoryRepository histories,
        PaymentRepository payments,
        ProductRepository products,
        CouponEngineService couponEngine,
        UserRepository users,
        NotificationService notifications,
        LoyaltyService loyalty
    ) {
        this.orders = orders;
        this.histories = histories;
        this.payments = payments;
        this.products = products;
        this.couponEngine = couponEngine;
        this.users = users;
        this.notifications = notifications;
        this.loyalty = loyalty;
    }

    @Transactional
    public Order updateStatus(Long orderId, String rawStatus, String note) {
        Order order = lockOrder(orderId);
        OrderStatus target = parseStatus(rawStatus);
        OrderStatus previousStatus = order.getStatus();
        if (order.getStatus() == target) {
            return order;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class)).contains(target)) {
            throw conflict("INVALID_ORDER_TRANSITION", "Không thể chuyển đơn từ " + order.getStatus().getValue() + " sang " + target.getValue());
        }

        Payment payment = payments.findByOrder_Id(orderId).orElse(null);
        if (target == OrderStatus.CANCELED) {
            if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
                throw conflict("PAYMENT_REFUND_REQUIRED", "Đơn đã thanh toán cần hoàn tiền trước khi hủy");
            }
            cancel(order, payment, defaultNote(note, "Đơn hàng đã bị hủy"));
            return order;
        }

        if (payment != null && payment.getPaymentMethod() != PaymentMethod.COD && payment.getStatus() != PaymentStatus.COMPLETED) {
            throw conflict("PAYMENT_NOT_COMPLETED", "Đơn thanh toán trực tuyến chưa được xác nhận");
        }

        LocalDateTime now = LocalDateTime.now();
        if (target == OrderStatus.OUT_FOR_DELIVERY) {
            if (order.getDeliveryStaff() == null) {
                throw conflict("DELIVERY_STAFF_NOT_ASSIGNED", "Đơn hàng chưa được phân công nhân viên giao hàng");
            }
            if (order.getDeliveryClaimedAt() == null) {
                throw conflict("DELIVERY_ORDER_NOT_CLAIMED", "Nhân viên giao hàng phải nhận đơn trước khi bắt đầu giao");
            }
        }

        order.setStatus(target);
        if (target == OrderStatus.READY_FOR_DELIVERY && previousStatus == OrderStatus.DELIVERY_FAILED) {
            order.setDeliveryStaff(null);
            order.setDeliveryClaimedAt(null);
            order.setDeliveryFailureReason(null);
        }
        if (target == OrderStatus.OUT_FOR_DELIVERY) {
            order.setDispatchedAt(now);
        }
        if (target == OrderStatus.DELIVERED) {
            order.setDeliveredAt(now);
        }
        if ((target == OrderStatus.DELIVERED || target == OrderStatus.COMPLETED)
            && payment != null && payment.getPaymentMethod() == PaymentMethod.COD
            && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(now);
            payment.setGatewayResponseCode("COD_COLLECTED");
            payments.save(payment);
        }
        addHistory(order, target, defaultNote(note, defaultStatusNote(target)));
        Order saved = orders.save(order);
        if (target == OrderStatus.PROCESSING) couponEngine.markUsed(saved);
        if (target == OrderStatus.DELIVERED || target == OrderStatus.COMPLETED) loyalty.rewardOrder(saved);
        notifications.notifyUser(saved.getUser(), "order", "Đơn hàng #" + saved.getId() + ": " + defaultStatusNote(target), "/orders/" + saved.getId());
        return saved;
    }

    @Transactional
    public Order updateByStaff(Long orderId, String rawStatus, String note) {
        OrderStatus target = parseStatus(rawStatus);
        if (target != OrderStatus.PROCESSING && target != OrderStatus.READY_FOR_DELIVERY && target != OrderStatus.CANCELED) {
            throw conflict("STAFF_STATUS_NOT_ALLOWED", "Nhân viên chỉ có thể xác nhận, chuẩn bị hoặc từ chối đơn hàng");
        }
        return updateStatus(orderId, rawStatus, note);
    }

    @Transactional
    public Order assignDeliveryStaff(Long orderId, Long deliveryStaffId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.READY_FOR_DELIVERY) {
            throw conflict("ORDER_NOT_READY_FOR_DELIVERY", "Đơn hàng chưa sẵn sàng để phân công giao");
        }
        User deliveryStaff = requireActiveDeliveryStaff(deliveryStaffId);
        if (order.getDeliveryStaff() != null && order.getDeliveryStaff().getId().equals(deliveryStaffId)) {
            return order;
        }
        order.setDeliveryStaff(deliveryStaff);
        order.setDeliveryClaimedAt(null);
        addHistory(order, order.getStatus(), "Đã phân công giao hàng cho " + deliveryStaff.getName());
        Order saved = orders.save(order);
        notifications.notifyUser(deliveryStaff, "delivery", "Bạn được phân công giao đơn hàng #" + saved.getId(), "/delivery");
        notifications.notifyUser(saved.getUser(), "order", "Đơn hàng #" + saved.getId() + " đã được phân công giao hàng", "/orders/" + saved.getId());
        return saved;
    }

    @Transactional
    public Order claimForDelivery(Long orderId, Long deliveryStaffId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.READY_FOR_DELIVERY) {
            throw conflict("ORDER_NOT_READY_FOR_DELIVERY", "Đơn hàng chưa sẵn sàng để nhận giao");
        }
        User deliveryStaff = requireActiveDeliveryStaff(deliveryStaffId);
        if (order.getDeliveryStaff() == null) {
            throw conflict("ORDER_NOT_ASSIGNED_TO_DELIVERY", "Đơn hàng chưa được phân công cho bạn");
        }
        if (!order.getDeliveryStaff().getId().equals(deliveryStaffId)) {
            throw conflict("ORDER_ASSIGNED_TO_ANOTHER_DELIVERY", "Đơn hàng đã được phân công cho nhân viên giao khác");
        }
        if (order.getDeliveryClaimedAt() == null) {
            order.setDeliveryClaimedAt(LocalDateTime.now());
            addHistory(order, order.getStatus(), "Nhân viên giao hàng " + deliveryStaff.getName() + " đã nhận đơn");
            notifications.notifyUser(order.getUser(), "delivery", "Nhân viên giao hàng đã nhận đơn #" + order.getId(), "/orders/" + order.getId());
        }
        return orders.save(order);
    }

    @Transactional
    public Order updateByDelivery(Long orderId, Long deliveryStaffId, String rawStatus, String note) {
        Order order = lockOrder(orderId);
        if (order.getDeliveryStaff() == null || !order.getDeliveryStaff().getId().equals(deliveryStaffId)) {
            throw conflict("ORDER_NOT_ASSIGNED_TO_DELIVERY", "Đơn hàng chưa được phân công cho bạn");
        }
        OrderStatus target = parseStatus(rawStatus);
        if (target != OrderStatus.OUT_FOR_DELIVERY && target != OrderStatus.DELIVERED && target != OrderStatus.DELIVERY_FAILED) {
            throw conflict("DELIVERY_STATUS_NOT_ALLOWED", "Trạng thái giao hàng không hợp lệ");
        }
        if (target == OrderStatus.DELIVERY_FAILED) {
            if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
                throw conflict("INVALID_ORDER_TRANSITION", "Chỉ có thể báo giao thất bại khi đơn đang giao");
            }
            order.setStatus(target);
            order.setDeliveryFailureReason(defaultNote(note, "Giao hàng không thành công"));
            addHistory(order, target, order.getDeliveryFailureReason());
            Order saved = orders.save(order);
            notifications.notifyUser(saved.getUser(), "delivery", "Giao đơn #" + saved.getId() + " chưa thành công: " + saved.getDeliveryFailureReason(), "/orders/" + saved.getId());
            return saved;
        }
        return updateStatus(orderId, rawStatus, note);
    }

    @Transactional
    public Order cancelByCustomer(Long userId, Long orderId) {
        Order order = orders.findByIdAndUserIdForUpdate(orderId, userId).orElseThrow(() -> notFound());
        if (order.getStatus() != OrderStatus.PENDING) {
            throw conflict("ORDER_CANNOT_BE_CANCELED", "Chỉ có thể hủy đơn đang chờ xác nhận");
        }
        Payment payment = payments.findByOrder_Id(orderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            throw conflict("PAYMENT_REFUND_REQUIRED", "Đơn đã thanh toán cần được hỗ trợ hoàn tiền");
        }
        cancel(order, payment, "Khách hàng đã hủy đơn");
        return order;
    }

    @Transactional
    public void cancelForFailedPayment(Long orderId, String note) {
        Order order = lockOrder(orderId);
        Payment payment = payments.findByOrder_Id(orderId).orElse(null);
        cancel(order, payment, note);
    }

    @Transactional
    public Order confirmRefundAndCancel(Long orderId, String note) {
        Order order = lockOrder(orderId);
        Payment payment = payments.findByOrder_Id(orderId).orElseThrow(() ->
            conflict("PAYMENT_NOT_FOUND", "Không tìm thấy giao dịch của đơn hàng"));
        if (payment.getStatus() == PaymentStatus.REFUNDED && order.getStatus() == OrderStatus.CANCELED) {
            return order;
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw conflict("PAYMENT_NOT_COMPLETED", "Chỉ có thể ghi nhận hoàn tiền cho giao dịch đã thanh toán");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setGatewayResponseCode("REFUND_CONFIRMED");
        payments.save(payment);
        cancel(order, payment, defaultNote(note, "Đã xác nhận hoàn tiền và hủy đơn hàng"));
        return order;
    }

    private void cancel(Order order, Payment payment, String note) {
        boolean preserveUsedCouponHistory = order.getStatus() == OrderStatus.COMPLETED;
        boolean newlyCanceled = order.getStatus() != OrderStatus.CANCELED;
        if (newlyCanceled) {
            order.setStatus(OrderStatus.CANCELED);
            addHistory(order, OrderStatus.CANCELED, note);
        }
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponseCode("ORDER_CANCELED");
            payments.save(payment);
        }
        releaseInventory(order);
        couponEngine.releaseForCancellation(order, preserveUsedCouponHistory);
        loyalty.restoreRedemption(order);
        Order saved = orders.save(order);
        if (newlyCanceled) notifications.notifyUser(saved.getUser(), "order", "Đơn hàng #" + saved.getId() + " đã bị hủy", "/orders/" + saved.getId());
    }

    private void releaseInventory(Order order) {
        if (order.getInventoryReleasedAt() != null) {
            return;
        }

        var productIds = order.getItems().stream()
            .map(item -> item.getProduct().getId())
            .distinct()
            .sorted()
            .toList();
        var lockedProducts = products.findAllByIdForUpdate(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = lockedProducts.get(item.getProduct().getId());
            if (product == null) {
                throw conflict("PRODUCT_NOT_FOUND", "Không thể hoàn tồn kho cho sản phẩm đã bị xóa");
            }
            product.setStock(product.getStock() + item.getQuantity());
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                product.setStatus(ProductStatus.IN_STOCK);
            }
        }
        products.saveAll(lockedProducts.values());

        order.setInventoryReleasedAt(LocalDateTime.now());
    }

    private void addHistory(Order order, OrderStatus status, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setNote(note);
        histories.save(history);
    }

    private Order lockOrder(Long orderId) {
        return orders.findByIdForUpdate(orderId).orElseThrow(this::notFound);
    }

    private OrderStatus parseStatus(String rawStatus) {
        try {
            return OrderStatus.fromValue(rawStatus == null ? "" : rawStatus.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS", exception.getMessage());
        }
    }

    private String defaultStatusNote(OrderStatus status) {
        return switch (status) {
            case PROCESSING -> "Đơn hàng đang được xử lý";
            case READY_FOR_DELIVERY -> "Đơn hàng đã sẵn sàng để giao";
            case OUT_FOR_DELIVERY -> "Đơn hàng đang được giao";
            case DELIVERED -> "Đơn hàng đã được giao";
            case DELIVERY_FAILED -> "Giao hàng không thành công";
            case COMPLETED -> "Đơn hàng đã hoàn tất";
            case CANCELED -> "Đơn hàng đã bị hủy";
            case PENDING -> "Đơn hàng đang chờ xác nhận";
        };
    }

    private String defaultNote(String note, String fallback) {
        return StringUtils.hasText(note) ? note.trim() : fallback;
    }

    private ApplicationException notFound() {
        return new ApplicationException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng");
    }

    private ApplicationException conflict(String code, String message) {
        return new ApplicationException(HttpStatus.CONFLICT, code, message);
    }

    private User requireActiveDeliveryStaff(Long userId) {
        User user = users.findById(userId).orElseThrow(() ->
            new ApplicationException(HttpStatus.NOT_FOUND, "DELIVERY_STAFF_NOT_FOUND", "Không tìm thấy nhân viên giao hàng"));
        if (user.getRole() == null || !"delivery_staff".equalsIgnoreCase(user.getRole().getName()) || user.getStatus() != UserStatus.ACTIVE) {
            throw conflict("INVALID_DELIVERY_STAFF", "Tài khoản được chọn không phải nhân viên giao hàng đang hoạt động");
        }
        return user;
    }
}
