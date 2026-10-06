package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.*;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.service.GuestCommerceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/guest")
public class GuestCommercePublicController {
    private final GuestCommerceService service;
    public GuestCommercePublicController(GuestCommerceService service){this.service=service;}
    @PostMapping("/sessions") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<GuestSessionResponse> createSession(){return ApiResponse.success("Đã tạo phiên mua hàng",service.createSession());}
    @GetMapping("/payment-methods") public ApiResponse<PaymentMethodOptionsResponse> paymentMethods(){return ApiResponse.success("Lấy phương thức thanh toán",service.paymentOptions());}
    @GetMapping("/cart") public ApiResponse<CartResponse> cart(@RequestHeader("X-Guest-Token") String token){return ApiResponse.success("Lấy giỏ hàng thành công",service.getCart(token));}
    @PostMapping("/cart/items") public ApiResponse<CartResponse> add(@RequestHeader("X-Guest-Token") String token,@Valid @RequestBody CartItemRequest request){return ApiResponse.success("Đã thêm vào giỏ hàng",service.addItem(token,request));}
    @PatchMapping("/cart/items/{id}") public ApiResponse<CartResponse> update(@RequestHeader("X-Guest-Token") String token,@PathVariable Long id,@Valid @RequestBody UpdateCartItemRequest request){return ApiResponse.success("Đã cập nhật giỏ hàng",service.updateItem(token,id,request));}
    @DeleteMapping("/cart/items/{id}") public ApiResponse<CartResponse> remove(@RequestHeader("X-Guest-Token") String token,@PathVariable Long id){return ApiResponse.success("Đã xóa sản phẩm",service.removeItem(token,id));}
    @DeleteMapping("/cart") public ApiResponse<CartResponse> clear(@RequestHeader("X-Guest-Token") String token){return ApiResponse.success("Đã xóa giỏ hàng",service.clearCart(token));}
    @PostMapping("/checkout/preview") public ApiResponse<CheckoutPreviewResponse> preview(@RequestHeader("X-Guest-Token") String token,@Valid @RequestBody GuestCheckoutRequest request){return ApiResponse.success("Tính đơn hàng thành công",service.preview(token,request));}
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<GuestOrderCreatedResponse> checkout(@RequestHeader("X-Guest-Token") String token,@Valid @RequestBody GuestCheckoutRequest request,HttpServletRequest servletRequest){return ApiResponse.success("Đặt hàng thành công",service.checkout(token,request,clientIp(servletRequest)));}
    @GetMapping("/orders/recovery") public ApiResponse<GuestOrderCreatedResponse> recover(@RequestHeader("X-Guest-Token") String token,@RequestParam String key,HttpServletRequest servletRequest){return ApiResponse.success("Khôi phục kết quả đặt hàng",service.recoverCheckout(token,key,clientIp(servletRequest)));}
    @PostMapping("/orders/lookup") public ApiResponse<OrderResponse> lookup(@Valid @RequestBody GuestOrderLookupRequest request){return ApiResponse.success("Xác minh đơn hàng thành công",service.lookup(request));}
    private String clientIp(HttpServletRequest request){String forwarded=request.getHeader("X-Forwarded-For");return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim();}
}
