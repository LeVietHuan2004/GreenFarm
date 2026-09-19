package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.CheckoutRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("30000.00");
    static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000.00");
    private final OrderRepository orders;
    private final OrderStatusHistoryRepository histories;
    private final ShippingAddressService addressService;
    private final CartItemRepository carts;
    private final ProductRepository products;
    private final CouponEngineService couponEngine;
    private final UserRepository users;
    private final PaymentService paymentService;
    private final OrderLifecycleService orderLifecycle;
    private final NotificationService notifications;
    private final LoyaltyService loyalty;

    public OrderService(OrderRepository orders, OrderStatusHistoryRepository histories, ShippingAddressService addressService,
                        CartItemRepository carts, ProductRepository products, CouponEngineService couponEngine, UserRepository users,
                        PaymentService paymentService, OrderLifecycleService orderLifecycle, NotificationService notifications,
                        LoyaltyService loyalty) {
        this.orders=orders; this.histories=histories; this.addressService=addressService; this.carts=carts;
        this.products=products; this.couponEngine=couponEngine; this.users=users; this.paymentService=paymentService; this.orderLifecycle=orderLifecycle;
        this.notifications=notifications;
        this.loyalty=loyalty;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Long userId, CheckoutRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản"));
        addressService.findEntity(userId, request.shippingAddressId());
        var cart = requireCart(userId);
        validateCart(cart);
        BigDecimal subtotal = subtotal(cart);
        BigDecimal shipping = shippingFee(subtotal);
        CouponEngineService.Quote couponQuote = couponEngine.preview(userId, cart, subtotal, shipping, request.couponCode(), request.freeShippingCouponCode());
        return calculate(subtotal, shipping, couponQuote, user, request.loyaltyPoints());
    }

    @Transactional
    public OrderResponse create(Long userId, CheckoutRequest request, String clientIp) {
        User user = users.findByIdForCommerceUpdate(userId).orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản"));
        ShippingAddress address = addressService.findEntity(userId, request.shippingAddressId());
        var cart = requireCart(userId);
        List<Long> productIds = cart.stream().map(item -> item.getProduct().getId()).sorted().toList();
        Map<Long, Product> lockedProducts = products.findAllByIdForUpdate(productIds).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (var item : cart) item.setProduct(lockedProducts.get(item.getProduct().getId()));
        validateCart(cart);
        BigDecimal subtotal = subtotal(cart);
        BigDecimal shipping = shippingFee(subtotal);
        CouponEngineService.Quote couponQuote = couponEngine.quoteForReservation(userId, cart, subtotal, shipping, request.couponCode(), request.freeShippingCouponCode());
        CheckoutPreviewResponse totals = calculate(subtotal, shipping, couponQuote, user, request.loyaltyPoints());

        Order order = new Order();
        order.setUser(user); order.setShippingAddress(address);
        order.setRecipientName(address.getFullName()); order.setRecipientPhone(address.getPhone());
        order.setShippingAddressLine(address.getAddress()); order.setShippingCity(address.getCity());
        order.setSubtotal(subtotal); order.setShippingFee(totals.shippingFee()); order.setDiscountAmount(totals.discountAmount());
        order.setLoyaltyPointsUsed(totals.loyaltyPointsApplied()); order.setLoyaltyDiscountAmount(totals.loyaltyDiscountAmount());
        order.setShippingDiscountAmount(totals.shippingDiscountAmount());
        order.setTotalPrice(totals.total()); order.setStatus(OrderStatus.PENDING);
        if (couponQuote.productCoupon() != null) { order.setCoupon(couponQuote.productCoupon()); order.setCouponCode(couponQuote.productCoupon().getCode()); }
        if (couponQuote.shippingCoupon() != null) { order.setShippingCoupon(couponQuote.shippingCoupon()); order.setShippingCouponCode(couponQuote.shippingCoupon().getCode()); }

        for (var cartItem : cart) {
            Product product = cartItem.getProduct();
            OrderItem item = new OrderItem(); item.setProduct(product); item.setProductName(product.getName());
            item.setProductUnit(product.getUnit()); item.setProductImage(product.getImages().isEmpty()?null:product.getImages().getFirst().getImage());
            item.setQuantity(cartItem.getQuantity()); item.setPrice(product.getPrice()); order.addItem(item);
            product.setStock(product.getStock()-cartItem.getQuantity());
            if (product.getStock()==0) product.setStatus(ProductStatus.OUT_OF_STOCK);
        }
        OrderStatusHistory initial = new OrderStatusHistory(); initial.setStatus(OrderStatus.PENDING); initial.setNote("Đơn hàng đã được tạo"); order.addHistory(initial);
        Order saved = orders.save(order);
        couponEngine.reserve(user, saved, couponQuote);
        loyalty.redeem(user, saved, totals.loyaltyPointsApplied());
        PaymentResponse payment = paymentService.createForOrder(saved, PaymentMethod.fromRequestValue(request.paymentMethod()), clientIp);
        products.saveAll(lockedProducts.values());
        carts.deleteAllByUser_Id(userId);
        notifications.notifyUser(saved.getUser(), "order", "Đơn hàng #" + saved.getId() + " đã được tạo", "/orders/" + saved.getId());
        notifications.notifyRole("admin", "order", "Có đơn hàng mới #" + saved.getId(), "/admin/orders");
        notifications.notifyRole("staff", "order", "Có đơn hàng mới #" + saved.getId(), "/staff");
        return toResponse(saved, saved.getStatusHistory(), payment);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> findAll(Long userId) {
        return orders.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId).stream().map(order -> new OrderSummaryResponse(
            order.getId(), order.getStatus().getValue(), order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
            order.getTotalPrice(), order.getRecipientName(), order.getShippingCity(), order.getCreatedAt(),
            deliveryStaffId(order), deliveryStaffName(order), order.getDeliveryClaimedAt())).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findOne(Long userId, Long orderId) {
        Order order = orders.findByIdAndUser_Id(orderId, userId).orElseThrow(() ->
            new ApplicationException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng"));
        return toResponse(order, histories.findAllByOrder_IdOrderByChangedAtAscIdAsc(orderId), paymentService.findForOrder(orderId));
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = orderLifecycle.cancelByCustomer(userId, orderId);
        return toResponse(order, histories.findAllByOrder_IdOrderByChangedAtAscIdAsc(orderId), paymentService.findForOrder(orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> findAdminOrders(String status, Pageable pageable) {
        var page = status == null || status.isBlank()
            ? orders.findAllByOrderByCreatedAtDescIdDesc(pageable)
            : orders.findAllByStatusOrderByCreatedAtDescIdDesc(parseStatus(status), pageable);
        return PageResponse.from(page, this::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderResponse findAdminOrder(Long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() ->
            new ApplicationException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng"));
        return toResponse(order, histories.findAllByOrder_IdOrderByChangedAtAscIdAsc(orderId), paymentService.findForOrder(orderId));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String status, String note) {
        Order order = orderLifecycle.updateStatus(orderId, status, note);
        return toResponse(order, histories.findAllByOrder_IdOrderByChangedAtAscIdAsc(orderId), paymentService.findForOrder(orderId));
    }

    @Transactional
    public OrderResponse confirmRefundAndCancel(Long orderId, String note) {
        Order order = orderLifecycle.confirmRefundAndCancel(orderId, note);
        return toResponse(order, histories.findAllByOrder_IdOrderByChangedAtAscIdAsc(orderId), paymentService.findForOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findStaffOrders() {
        return orders.findAllByStatusInOrderByCreatedAtDescIdDesc(List.of(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.READY_FOR_DELIVERY, OrderStatus.DELIVERY_FAILED))
            .stream().map(this::toOperationalResponse).toList();
    }

    @Transactional
    public OrderResponse updateStaffStatus(Long orderId, String status, String note) {
        return toOperationalResponse(orderLifecycle.updateByStaff(orderId, status, note));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findDeliveryOrders(Long deliveryStaffId) {
        return orders.findAllByDeliveryStaff_IdOrderByCreatedAtDescIdDesc(deliveryStaffId).stream().map(this::toOperationalResponse).toList();
    }

    @Transactional
    public OrderResponse assignDeliveryStaff(Long orderId, Long deliveryStaffId) {
        return toOperationalResponse(orderLifecycle.assignDeliveryStaff(orderId, deliveryStaffId));
    }

    @Transactional
    public OrderResponse claimForDelivery(Long orderId, Long deliveryStaffId) {
        return toOperationalResponse(orderLifecycle.claimForDelivery(orderId, deliveryStaffId));
    }

    @Transactional
    public OrderResponse updateDeliveryStatus(Long orderId, Long deliveryStaffId, String status, String note) {
        return toOperationalResponse(orderLifecycle.updateByDelivery(orderId, deliveryStaffId, status, note));
    }

    private List<CartItem> requireCart(Long userId) {
        var cart = carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId);
        if (cart.isEmpty()) throw new ApplicationException(HttpStatus.CONFLICT, "EMPTY_CART", "Giỏ hàng đang trống");
        return cart;
    }
    private void validateCart(List<CartItem> cart) {
        for (var item : cart) {
            Product product=item.getProduct();
            if (product==null)
                throw new ApplicationException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", "Một sản phẩm trong giỏ không còn tồn tại");
            if (product.getStatus()!=ProductStatus.IN_STOCK || product.getStock()<=0)
                throw new ApplicationException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", "Sản phẩm “"+product.getName()+"” hiện không còn bán");
            if (item.getQuantity()>product.getStock())
                throw new ApplicationException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Sản phẩm “"+product.getName()+"” chỉ còn "+product.getStock());
        }
    }
    private BigDecimal subtotal(List<CartItem> cart) { return cart.stream().map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP); }
    private CheckoutPreviewResponse calculate(BigDecimal subtotal, BigDecimal shipping, CouponEngineService.Quote couponQuote,
                                              User user, Integer requestedLoyaltyPoints) {
        BigDecimal afterProductCoupon = subtotal.subtract(couponQuote.productDiscount()).setScale(2, RoundingMode.HALF_UP);
        LoyaltyService.Quote loyaltyQuote = loyalty.quote(user, requestedLoyaltyPoints, afterProductCoupon);
        BigDecimal total = afterProductCoupon.subtract(loyaltyQuote.discountAmount())
            .add(shipping).subtract(couponQuote.shippingDiscount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new CheckoutPreviewResponse(subtotal, shipping, couponQuote.productDiscount(), loyaltyQuote.discountAmount(),
            loyaltyQuote.pointsApplied(), couponQuote.shippingDiscount(), total,
            couponQuote.productCoupon() == null ? null : couponQuote.productCoupon().getCode(),
            couponQuote.shippingCoupon() == null ? null : couponQuote.shippingCoupon().getCode(),
            couponQuote.productDescription(), couponQuote.shippingDescription());
    }
    private BigDecimal shippingFee(BigDecimal subtotal) { return subtotal.compareTo(FREE_SHIPPING_THRESHOLD)>=0?BigDecimal.ZERO:STANDARD_SHIPPING_FEE; }
    private OrderStatus parseStatus(String value) {
        try { return OrderStatus.fromValue(value.trim()); }
        catch (IllegalArgumentException exception) { throw new ApplicationException(HttpStatus.BAD_REQUEST,"INVALID_ORDER_STATUS",exception.getMessage()); }
    }
    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getStatus().getValue(), order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
            order.getTotalPrice(), order.getRecipientName(), order.getShippingCity(), order.getCreatedAt(), deliveryStaffId(order), deliveryStaffName(order), order.getDeliveryClaimedAt());
    }
    private OrderResponse toResponse(Order order, List<OrderStatusHistory> history, PaymentResponse payment) {
        var items=order.getItems().stream().map(item -> new OrderItemResponse(item.getId(), item.getProduct().getId(), item.getProduct().getSlug(), item.getProductName(), item.getProductUnit(), item.getProductImage(), item.getQuantity(), item.getPrice(), item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).toList();
        var statusHistory=history.stream().map(item -> new OrderStatusHistoryResponse(item.getId(), item.getStatus().getValue(), item.getNote(), item.getChangedAt())).toList();
        return new OrderResponse(order.getId(),order.getStatus().getValue(),order.getSubtotal(),order.getShippingFee(),order.getDiscountAmount(),order.getTotalPrice(),order.getCouponCode(),order.getRecipientName(),order.getRecipientPhone(),order.getShippingAddressLine(),order.getShippingCity(),items,statusHistory,order.getCreatedAt(),order.getUpdatedAt(),payment,deliveryStaffId(order),deliveryStaffName(order),order.getDeliveryClaimedAt(),order.getDeliveryFailureReason(),order.getLoyaltyPointsUsed(),order.getLoyaltyDiscountAmount(),order.getShippingCouponCode(),order.getShippingDiscountAmount());
    }
    private OrderResponse toOperationalResponse(Order order) { return toResponse(order, histories.findAllByOrder_IdOrderByChangedAtAscIdAsc(order.getId()), paymentService.findForOrder(order.getId())); }
    private Long deliveryStaffId(Order order) { return order.getDeliveryStaff() == null ? null : order.getDeliveryStaff().getId(); }
    private String deliveryStaffName(Order order) { return order.getDeliveryStaff() == null ? null : order.getDeliveryStaff().getName(); }
}
