package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.*;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.mapper.CatalogMapper;
import com.agri.ecommerce.repository.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestCommerceService {
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("30000.00");
    private static final BigDecimal EXPRESS_SHIPPING_FEE = new BigDecimal("50000.00");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000.00");
    private final GuestSessionRepository sessions;
    private final GuestCartItemRepository guestCarts;
    private final CartItemRepository carts;
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;
    private final CouponEngineService coupons;
    private final PaymentService payments;
    private final NotificationService notifications;
    private final OrderService orderService;
    private final InventoryService inventory;
    private final int sessionDays;
    private final SecureRandom random = new SecureRandom();

    public GuestCommerceService(GuestSessionRepository sessions, GuestCartItemRepository guestCarts,
            CartItemRepository carts, ProductRepository products, UserRepository users, OrderRepository orders,
            CouponEngineService coupons, PaymentService payments, NotificationService notifications,
            OrderService orderService, InventoryService inventory, @Value("${app.guest.session-days:30}") int sessionDays) {
        this.sessions=sessions; this.guestCarts=guestCarts; this.carts=carts; this.products=products; this.users=users;
        this.orders=orders; this.coupons=coupons; this.payments=payments; this.notifications=notifications;
        this.orderService=orderService; this.inventory=inventory; this.sessionDays=Math.max(1,sessionDays);
    }

    @Transactional
    public GuestSessionResponse createSession() {
        String token = randomToken();
        GuestSession session = new GuestSession();
        session.setTokenHash(hash(token));
        session.setExpiresAt(LocalDateTime.now().plusDays(sessionDays));
        sessions.save(session);
        return new GuestSessionResponse(token, session.getExpiresAt());
    }

    public PaymentMethodOptionsResponse paymentOptions(){return payments.options();}

    @Transactional(readOnly=true)
    public CartResponse getCart(String token) { return cartResponse(guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(requireActive(token,false).getId())); }

    @Transactional
    public CartResponse addItem(String token, CartItemRequest request) {
        GuestSession session=requireActive(token,true); Product product=requireProduct(request.productId());
        GuestCartItem item=guestCarts.findByGuestSession_IdAndProduct_Id(session.getId(),product.getId()).orElseGet(()->{
            GuestCartItem value=new GuestCartItem(); value.setGuestSession(session); value.setProduct(product); return value;
        });
        long quantity=(long)item.getQuantity()+request.quantity(); validateProduct(product,quantity);
        item.setQuantity((int)quantity); guestCarts.save(item); touch(session); return cartResponse(session.getId());
    }

    @Transactional
    public CartResponse updateItem(String token,Long itemId,UpdateCartItemRequest request) {
        GuestSession session=requireActive(token,true);
        GuestCartItem item=guestCarts.findByIdAndGuestSession_Id(itemId,session.getId()).orElseThrow(this::cartItemNotFound);
        validateProduct(item.getProduct(),request.quantity()); item.setQuantity(request.quantity()); guestCarts.save(item); touch(session);
        return cartResponse(session.getId());
    }

    @Transactional
    public CartResponse removeItem(String token,Long itemId) {
        GuestSession session=requireActive(token,true);
        GuestCartItem item=guestCarts.findByIdAndGuestSession_Id(itemId,session.getId()).orElseThrow(this::cartItemNotFound);
        guestCarts.delete(item); touch(session); return cartResponse(session.getId());
    }

    @Transactional
    public CartResponse clearCart(String token) {
        GuestSession session=requireActive(token,true); guestCarts.deleteAllByGuestSession_Id(session.getId()); touch(session);
        return new CartResponse(List.of(),0,BigDecimal.ZERO);
    }

    @Transactional(readOnly=true)
    public CheckoutPreviewResponse preview(String token,GuestCheckoutRequest request) {
        GuestSession session=requireActive(token,false); List<CartItem> cart=cartAdapters(session.getId()); validateCart(cart);
        BigDecimal subtotal=subtotal(cart); BigDecimal shipping=shippingFee(subtotal,shippingMethod(request.shippingMethod()));
        CouponEngineService.Quote quote=coupons.previewGuest(session.getId(),cart,subtotal,shipping,request.couponCode(),request.freeShippingCouponCode());
        return totals(subtotal,shipping,quote);
    }

    @Transactional
    public GuestOrderCreatedResponse checkout(String rawToken,GuestCheckoutRequest request,String clientIp) {
        GuestSession session=requireSession(rawToken,true);
        Optional<Order> replay=orders.findByGuestSession_IdAndGuestCheckoutKey(session.getId(),request.idempotencyKey());
        if(replay.isPresent()) return existingOrderResponse(rawToken,request.idempotencyKey(),replay.get(),clientIp);
        if(session.getConsumedAt()!=null) throw conflict("GUEST_SESSION_CONSUMED","Phiên khách đã được sử dụng. Vui lòng bắt đầu giỏ hàng mới");

        List<GuestCartItem> source=guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(session.getId());
        if(source.isEmpty()) throw conflict("EMPTY_CART","Giỏ hàng đang trống");
        List<Long> ids=source.stream().map(i->i.getProduct().getId()).distinct().sorted().toList();
        Map<Long,Product> locked=products.findAllByIdForUpdate(ids).stream().collect(Collectors.toMap(Product::getId,Function.identity()));
        List<CartItem> cart=new ArrayList<>();
        for(GuestCartItem sourceItem:source){ Product product=locked.get(sourceItem.getProduct().getId()); CartItem item=new CartItem(); item.setProduct(product); item.setQuantity(sourceItem.getQuantity()); cart.add(item); }
        validateCart(cart);
        BigDecimal subtotal=subtotal(cart); String shippingMethod=shippingMethod(request.shippingMethod());
        BigDecimal shipping=shippingFee(subtotal,shippingMethod);
        CouponEngineService.Quote quote=coupons.quoteGuestForReservation(session.getId(),cart,subtotal,shipping,request.couponCode(),request.freeShippingCouponCode());
        CheckoutPreviewResponse total=totals(subtotal,shipping,quote);

        Order order=new Order(); order.setGuestSession(session); order.setGuestEmail(request.email().trim().toLowerCase(Locale.ROOT));
        order.setGuestCheckoutKey(request.idempotencyKey()); order.setRecipientName(request.name().trim()); order.setRecipientPhone(request.phone().trim());
        order.setShippingAddressLine(request.shippingAddress().trim()); order.setShippingCity(request.shippingCity().trim()); order.setShippingMethod(shippingMethod);
        order.setSubtotal(subtotal); order.setShippingFee(total.shippingFee()); order.setDiscountAmount(total.discountAmount());
        order.setLoyaltyPointsUsed(0); order.setLoyaltyDiscountAmount(BigDecimal.ZERO); order.setShippingDiscountAmount(total.shippingDiscountAmount());
        order.setTotalPrice(total.total()); order.setStatus(OrderStatus.PENDING);
        if(quote.productCoupon()!=null){order.setCoupon(quote.productCoupon());order.setCouponCode(quote.productCoupon().getCode());}
        if(quote.shippingCoupon()!=null){order.setShippingCoupon(quote.shippingCoupon());order.setShippingCouponCode(quote.shippingCoupon().getCode());}
        for(CartItem cartItem:cart){Product product=cartItem.getProduct(); OrderItem item=new OrderItem();item.setProduct(product);item.setProductName(product.getName());item.setProductUnit(product.getUnit());item.setProductImage(product.getImages().isEmpty()?null:product.getImages().getFirst().getImage());item.setQuantity(cartItem.getQuantity());item.setPrice(product.getPrice());order.addItem(item);}
        OrderStatusHistory history=new OrderStatusHistory();history.setStatus(OrderStatus.PENDING);history.setNote("Đơn hàng khách vãng lai đã được tạo");order.addHistory(history);
        Order saved=orders.save(order); inventory.reserveOrder(saved,locked); String lookupToken=lookupToken(rawToken,saved.getId(),request.idempotencyKey()); saved.setGuestLookupTokenHash(hash(lookupToken)); orders.save(saved);
        coupons.reserveGuest(session,saved,quote);
        PaymentResponse payment=payments.createForOrder(saved,PaymentMethod.fromRequestValue(request.paymentMethod()),clientIp);
        guestCarts.deleteAllByGuestSession_Id(session.getId()); session.setConsumedAt(LocalDateTime.now()); sessions.save(session);
        notifications.notifyRole("admin","order","Có đơn hàng khách mới #"+saved.getId(),"/admin/orders");
        notifications.notifyRole("staff","order","Có đơn hàng khách mới #"+saved.getId(),"/staff");
        return new GuestOrderCreatedResponse(orderService.findAdminOrder(saved.getId()),lookupToken,false,payment.paymentUrl());
    }

    @Transactional
    public GuestOrderCreatedResponse recoverCheckout(String rawToken,String key,String clientIp) {
        if(key==null || !key.matches("^[A-Za-z0-9_-]{16,64}$")) throw orderNotFound();
        GuestSession session=requireSession(rawToken,true);
        Order existing=orders.findByGuestSession_IdAndGuestCheckoutKey(session.getId(),key).orElseThrow(this::orderNotFound);
        return existingOrderResponse(rawToken,key,existing,clientIp);
    }

    private GuestOrderCreatedResponse existingOrderResponse(String rawToken,String key,Order existing,String clientIp) {
        return new GuestOrderCreatedResponse(orderService.findAdminOrder(existing.getId()),
            lookupToken(rawToken,existing.getId(),key),true,payments.pendingVnpayUrl(existing.getId(),clientIp));
    }

    @Transactional(readOnly=true)
    public OrderResponse lookup(GuestOrderLookupRequest request) {
        Order order=orders.findByIdAndGuestEmailIgnoreCase(request.orderId(),request.email().trim()).orElseThrow(this::orderNotFound);
        if(order.getGuestLookupTokenHash()==null || !secureEquals(order.getGuestLookupTokenHash(),hash(request.token()))) throw orderNotFound();
        return orderService.findAdminOrder(order.getId());
    }

    @Transactional
    public CartResponse mergeIntoCustomer(String token,Long userId) {
        User user=users.findByIdForCommerceUpdate(userId).orElseThrow(()->new ApplicationException(HttpStatus.NOT_FOUND,"USER_NOT_FOUND","Không tìm thấy tài khoản"));
        GuestSession session=requireActive(token,true); List<GuestCartItem> guestItems=guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(session.getId());
        List<CartItem> customerItems=carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId);
        if(guestItems.isEmpty() && customerItems.isEmpty()){consumeMerged(session,user);return cartResponseForUser(userId);}
        List<Long> ids=java.util.stream.Stream.concat(guestItems.stream().map(i->i.getProduct().getId()),
            customerItems.stream().map(i->i.getProduct().getId())).distinct().sorted().toList();
        Map<Long,Product> locked=products.findAllByIdForUpdate(ids).stream().collect(Collectors.toMap(Product::getId,Function.identity()));
        for(CartItem item:customerItems) validateProduct(locked.get(item.getProduct().getId()),item.getQuantity());
        for(GuestCartItem guest:guestItems){
            Product product=locked.get(guest.getProduct().getId()); CartItem existing=customerItems.stream()
                .filter(item->item.getProduct().getId().equals(guest.getProduct().getId())).findFirst().orElse(null);
            long merged=(existing==null?0L:existing.getQuantity())+guest.getQuantity(); validateProduct(product,merged);
        }
        for(GuestCartItem guest:guestItems){Product product=locked.get(guest.getProduct().getId());CartItem item=carts.findByUser_IdAndProduct_Id(userId,product.getId()).orElseGet(()->{CartItem value=new CartItem();value.setUser(user);value.setProduct(product);return value;});item.setQuantity(item.getQuantity()+guest.getQuantity());carts.save(item);}
        guestCarts.deleteAllByGuestSession_Id(session.getId()); consumeMerged(session,user); return cartResponseForUser(userId);
    }

    private void consumeMerged(GuestSession session,User user){session.setConsumedAt(LocalDateTime.now());session.setMergedUser(user);sessions.save(session);}
    private GuestSession requireActive(String token,boolean lock){GuestSession s=requireSession(token,lock);if(s.getConsumedAt()!=null)throw conflict("GUEST_SESSION_CONSUMED","Phiên khách không còn hiệu lực");return s;}
    private GuestSession requireSession(String token,boolean lock){if(token==null||token.isBlank())throw unauthorized();GuestSession s=(lock?sessions.findByTokenHashForUpdate(hash(token)):sessions.findByTokenHash(hash(token))).orElseThrow(this::unauthorized);if(!s.getExpiresAt().isAfter(LocalDateTime.now()))throw new ApplicationException(HttpStatus.UNAUTHORIZED,"GUEST_SESSION_EXPIRED","Phiên khách đã hết hạn");return s;}
    private void touch(GuestSession s){s.setExpiresAt(LocalDateTime.now().plusDays(sessionDays));sessions.save(s);}
    private Product requireProduct(Long id){Product p=products.findById(id).orElseThrow(()->new ApplicationException(HttpStatus.NOT_FOUND,"PRODUCT_NOT_FOUND","Không tìm thấy sản phẩm"));validateProduct(p,1);return p;}
    private void validateProduct(Product p,long quantity){if(p==null||p.getStatus()!=ProductStatus.IN_STOCK||p.getStock()<=0)throw conflict("PRODUCT_UNAVAILABLE","Sản phẩm hiện không còn bán");if(quantity<1||quantity>Integer.MAX_VALUE||quantity>p.getStock())throw conflict("INSUFFICIENT_STOCK","Số lượng yêu cầu vượt quá tồn kho hiện tại");}
    private void validateCart(List<CartItem> cart){if(cart.isEmpty())throw conflict("EMPTY_CART","Giỏ hàng đang trống");for(CartItem i:cart)validateProduct(i.getProduct(),i.getQuantity());}
    private List<CartItem> cartAdapters(Long sessionId){List<CartItem> result=new ArrayList<>();for(GuestCartItem source:guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(sessionId)){CartItem item=new CartItem();item.setProduct(source.getProduct());item.setQuantity(source.getQuantity());result.add(item);}return result;}
    private BigDecimal subtotal(List<CartItem> cart){return cart.stream().map(i->i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);}
    private String shippingMethod(String value){return value==null||value.isBlank()?"standard":value.trim().toLowerCase(Locale.ROOT);}
    private BigDecimal shippingFee(BigDecimal subtotal,String method){if("standard".equals(method)&&subtotal.compareTo(FREE_SHIPPING_THRESHOLD)>=0)return BigDecimal.ZERO;return "express".equals(method)?EXPRESS_SHIPPING_FEE:STANDARD_SHIPPING_FEE;}
    private CheckoutPreviewResponse totals(BigDecimal subtotal,BigDecimal shipping,CouponEngineService.Quote quote){BigDecimal total=subtotal.subtract(quote.productDiscount()).add(shipping).subtract(quote.shippingDiscount()).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);return new CheckoutPreviewResponse(subtotal,shipping,quote.productDiscount(),BigDecimal.ZERO,0,quote.shippingDiscount(),total,quote.productCoupon()==null?null:quote.productCoupon().getCode(),quote.shippingCoupon()==null?null:quote.shippingCoupon().getCode(),quote.productDescription(),quote.shippingDescription());}
    private CartResponse cartResponse(Long id){return cartResponse(guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(id));}
    private CartResponse cartResponse(List<GuestCartItem> entities){List<CartItemResponse> items=entities.stream().map(i->new CartItemResponse(i.getId(),i.getQuantity(),i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())),CatalogMapper.toProductResponse(i.getProduct()),i.getCreatedAt(),i.getUpdatedAt())).toList();return new CartResponse(items,items.stream().mapToLong(CartItemResponse::quantity).sum(),items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add));}
    private CartResponse cartResponseForUser(Long id){List<CartItemResponse> items=carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(id).stream().map(i->new CartItemResponse(i.getId(),i.getQuantity(),i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())),CatalogMapper.toProductResponse(i.getProduct()),i.getCreatedAt(),i.getUpdatedAt())).toList();return new CartResponse(items,items.stream().mapToLong(CartItemResponse::quantity).sum(),items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add));}
    private String randomToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private String lookupToken(String guestToken,Long orderId,String key){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(guestToken.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((orderId+":"+key).getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    private String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(bytes);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private boolean secureEquals(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII),b.getBytes(StandardCharsets.US_ASCII));}
    private ApplicationException unauthorized(){return new ApplicationException(HttpStatus.UNAUTHORIZED,"INVALID_GUEST_SESSION","Phiên khách không hợp lệ");}
    private ApplicationException cartItemNotFound(){return new ApplicationException(HttpStatus.NOT_FOUND,"CART_ITEM_NOT_FOUND","Không tìm thấy sản phẩm trong giỏ hàng");}
    private ApplicationException orderNotFound(){return new ApplicationException(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND","Không tìm thấy đơn hàng hoặc thông tin xác minh không đúng");}
    private ApplicationException conflict(String code,String message){return new ApplicationException(HttpStatus.CONFLICT,code,message);}
}
