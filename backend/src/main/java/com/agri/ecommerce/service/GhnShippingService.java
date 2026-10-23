package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.config.GhnProperties;
import com.agri.ecommerce.entity.CartItem;
import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderItem;
import com.agri.ecommerce.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GhnShippingService {
    public record Recipient(String name, String phone, String address, String province, String ward) {}
    public record Quote(BigDecimal fee, int serviceType, LocalDateTime estimatedDeliveryAt) {}
    public record Created(String orderCode, BigDecimal fee, LocalDateTime estimatedDeliveryAt) {}
    private record PackageItem(String name, int quantity, int weight, int length, int width, int height, int price) {}
    private final GhnClient client;
    private final GhnProperties properties;

    public GhnShippingService(GhnClient client, GhnProperties properties) {
        this.client = client; this.properties = properties;
    }

    public Quote previewCart(List<CartItem> cart, Recipient recipient, BigDecimal subtotal, BigDecimal codAmount) {
        List<PackageItem> items = new ArrayList<>();
        for (CartItem line : cart) {
            Product product = line.getProduct();
            if (!product.isGhnEligible() || product.getPackageWeightG() == null
                || product.getPackageLengthCm() == null || product.getPackageWidthCm() == null
                || product.getPackageHeightCm() == null)
                throw new ApplicationException(HttpStatus.CONFLICT, "GHN_PRODUCT_NOT_ELIGIBLE",
                    "Giỏ hàng có sản phẩm chưa được xác nhận đủ điều kiện gửi GHN");
            items.add(new PackageItem(product.getName(), line.getQuantity(), product.getPackageWeightG(),
                product.getPackageLengthCm(), product.getPackageWidthCm(), product.getPackageHeightCm(),
                vnd(product.getPrice(), "GHN_PRICE_INVALID")));
        }
        return preview(items, recipient, subtotal, codAmount);
    }

    public Quote previewOrder(Order order, BigDecimal codAmount) {
        List<PackageItem> items = order.getItems().stream().map(item -> packageItem(item)).toList();
        return preview(items, recipient(order), order.getSubtotal(), codAmount);
    }

    public Created create(Order order, String clientOrderCode, BigDecimal codAmount) {
        List<PackageItem> items = order.getItems().stream().map(this::packageItem).toList();
        Map<String, Object> body = payload(items, recipient(order), order.getSubtotal(), codAmount);
        body.put("client_order_code", clientOrderCode);
        JsonNode data = client.post("/shiip/public-api/v2/shipping-order/create", body);
        String code = data.path("order_code").asText();
        if (code.isBlank()) throw new ApplicationException(HttpStatus.BAD_GATEWAY, "GHN_ORDER_CODE_MISSING", "GHN không trả mã vận đơn");
        return new Created(code, money(data.path("total_fee")), estimated(data.path("expected_delivery_time")));
    }

    public JsonNode detailByClientCode(String code) {
        return client.get("/shiip/public-api/v2/shipping-order/detail-by-client-code?client_order_code=" + code);
    }

    public JsonNode detail(String code) {
        return client.get("/shiip/public-api/v2/shipping-order/detail?order_code=" + code);
    }

    public boolean cancel(String code) {
        JsonNode data = client.post("/shiip/public-api/v2/switch-status/cancel",
            Map.of("order_codes", List.of(code), "reason_code", "GHN-CANCEL-OTHER", "reason", "GreenFarm hủy vận đơn"));
        return data.isArray() && data.size() == 1 && code.equals(data.get(0).path("order_code").asText())
            && data.get(0).path("result").asBoolean();
    }

    public String printUrl(String code) {
        JsonNode data = client.post("/shiip/public-api/v2/a5/gen-token", Map.of("order_codes", List.of(code)));
        String token = data.path("token").asText();
        if (!token.matches("[A-Za-z0-9-]{10,100}"))
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "GHN_PRINT_TOKEN_INVALID", "GHN không trả token in hợp lệ");
        return properties.baseUrl() + "/a5/public-api/printA5?token=" + token;
    }

    private Quote preview(List<PackageItem> items, Recipient recipient, BigDecimal subtotal, BigDecimal codAmount) {
        JsonNode data = client.post("/shiip/public-api/v2/shipping-order/preview", payload(items, recipient, subtotal, codAmount));
        if (!data.path("order_code").asText().isBlank())
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "GHN_PREVIEW_CREATED_ORDER", "GHN xem trước trả dữ liệu bất thường");
        long weight = items.stream().mapToLong(item -> (long)item.weight() * item.quantity()).sum();
        return new Quote(money(data.path("total_fee")), weight >= 20000 ? 5 : 2,
            estimated(data.path("expected_delivery_time")));
    }

    private Map<String, Object> payload(List<PackageItem> items, Recipient recipient,
                                        BigDecimal subtotal, BigDecimal codAmount) {
        properties.requireEnabled();
        if (recipient.province() == null || recipient.province().isBlank()
            || recipient.ward() == null || recipient.ward().isBlank())
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "GHN_ADDRESS_REQUIRED",
                "Cần chọn tỉnh/thành và phường/xã GHN cho địa chỉ giao hàng");
        if (items.isEmpty()) throw new ApplicationException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "Giỏ hàng đang trống");
        long weight = 0;
        long height = 0;
        int length = 0, width = 0;
        List<Map<String, Object>> lines = new ArrayList<>();
        for (PackageItem item : items) {
            if (item.quantity() <= 0 || item.weight() <= 0 || item.length() <= 0 || item.width() <= 0 || item.height() <= 0)
                throw new ApplicationException(HttpStatus.CONFLICT, "GHN_PACKAGE_REQUIRED", "Thiếu thông số đóng gói GHN");
            weight += (long)item.weight() * item.quantity();
            height += (long)item.height() * item.quantity();
            length = Math.max(length, item.length()); width = Math.max(width, item.width());
            lines.add(Map.of("name", item.name(), "quantity", item.quantity(), "price", item.price(),
                "weight", item.weight(), "length", item.length(), "width", item.width(), "height", item.height()));
        }
        if (weight > 50000 || length > 200 || width > 200 || height > 200)
            throw new ApplicationException(HttpStatus.CONFLICT, "GHN_PACKAGE_TOO_LARGE", "Kiện hàng vượt giới hạn GHN");
        int serviceType = weight >= 20000 ? 5 : 2;
        Map<String, Object> body = new HashMap<>();
        body.put("to_name", recipient.name()); body.put("to_phone", recipient.phone());
        body.put("to_address", recipient.address()); body.put("to_province_name", recipient.province());
        body.put("to_ward_name", recipient.ward()); body.put("is_new_to_address", true);
        body.put("weight", (int)weight); body.put("length", length); body.put("width", width); body.put("height", (int)height);
        body.put("service_type_id", serviceType); body.put("payment_type_id", properties.shippingPaymentType());
        body.put("required_note", properties.requiredNote());
        int value = vnd(subtotal, "GHN_VALUE_INVALID");
        int cod = vnd(codAmount, "GHN_COD_INVALID");
        if (cod > 50_000_000) throw new ApplicationException(HttpStatus.CONFLICT, "GHN_COD_TOO_LARGE", "Số tiền thu hộ vượt giới hạn GHN");
        body.put("insurance_value", Math.min(5_000_000, value));
        body.put("order_value", value);
        body.put("cod_amount", cod);
        body.put("items", lines);
        return body;
    }

    private PackageItem packageItem(OrderItem item) {
        if (item.getProduct() == null || !item.getProduct().isGhnEligible())
            throw new ApplicationException(HttpStatus.CONFLICT, "GHN_PRODUCT_NOT_ELIGIBLE",
                "Sản phẩm trong đơn hiện không còn đủ điều kiện gửi GHN");
        if (item.getPackageWeightG() == null || item.getPackageLengthCm() == null
            || item.getPackageWidthCm() == null || item.getPackageHeightCm() == null)
            throw new ApplicationException(HttpStatus.CONFLICT, "GHN_PACKAGE_REQUIRED", "Đơn thiếu thông số kiện hàng GHN");
        return new PackageItem(item.getProductName(), item.getQuantity(), item.getPackageWeightG(),
            item.getPackageLengthCm(), item.getPackageWidthCm(), item.getPackageHeightCm(),
            vnd(item.getPrice(), "GHN_PRICE_INVALID"));
    }
    private Recipient recipient(Order order) {
        return new Recipient(order.getRecipientName(), order.getRecipientPhone(), order.getShippingAddressLine(),
            order.getShippingProvinceName(), order.getShippingWardName());
    }
    private BigDecimal money(JsonNode node) {
        if (!node.isNumber() || node.decimalValue().signum() < 0)
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "GHN_FEE_INVALID", "GHN không trả phí hợp lệ");
        return node.decimalValue();
    }
    private int vnd(BigDecimal amount, String code) {
        try {
            if (amount == null || amount.signum() < 0) throw new ArithmeticException();
            return amount.intValueExact();
        } catch (ArithmeticException exception) {
            throw new ApplicationException(HttpStatus.CONFLICT, code, "Giá trị GHN cần là số VND nguyên trong giới hạn hỗ trợ");
        }
    }
    private LocalDateTime estimated(JsonNode node) {
        try {
            return node.isTextual() && !node.asText().isBlank()
                ? OffsetDateTime.parse(node.asText()).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime()
                : null;
        } catch (RuntimeException ignored) { return null; }
    }
}
