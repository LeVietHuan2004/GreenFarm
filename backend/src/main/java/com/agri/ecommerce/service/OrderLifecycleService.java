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
        OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orders;
    private final OrderStatusHistoryRepository histories;
    private final PaymentRepository payments;
    private final ProductRepository products;
    private final CouponRepository coupons;

    public OrderLifecycleService(
        OrderRepository orders,
        OrderStatusHistoryRepository histories,
        PaymentRepository payments,
        ProductRepository products,
        CouponRepository coupons
    ) {
        this.orders = orders;
        this.histories = histories;
        this.payments = payments;
        this.products = products;
        this.coupons = coupons;
    }

    @Transactional
    public Order updateStatus(Long orderId, String rawStatus, String note) {
        Order order = lockOrder(orderId);
        OrderStatus target = parseStatus(rawStatus);
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
        order.setStatus(target);
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
        return orders.save(order);
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

    private void cancel(Order order, Payment payment, String note) {
        if (order.getStatus() != OrderStatus.CANCELED) {
            order.setStatus(OrderStatus.CANCELED);
            addHistory(order, OrderStatus.CANCELED, note);
        }
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponseCode("ORDER_CANCELED");
            payments.save(payment);
        }
        releaseInventoryAndCoupon(order);
        orders.save(order);
    }

    private void releaseInventoryAndCoupon(Order order) {
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

        if (order.getCoupon() != null) {
            Coupon coupon = coupons.findByIdForUpdate(order.getCoupon().getId()).orElse(null);
            if (coupon != null && coupon.getTimesUsed() > 0) {
                coupon.setTimesUsed(coupon.getTimesUsed() - 1);
                coupons.save(coupon);
            }
        }
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
}
