package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.config.GhnProperties;
import com.agri.ecommerce.entity.CartItem;
import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderItem;
import com.agri.ecommerce.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GhnShippingServiceTest {
    private final GhnClient client = mock(GhnClient.class);
    private final GhnProperties properties = mock(GhnProperties.class);
    private final ObjectMapper json = new ObjectMapper();
    private GhnShippingService service;

    @BeforeEach void setUp() { service = new GhnShippingService(client, properties); }

    @Test void previewUsesGhnFeeAndFinalCodWithoutCreatingOrder() throws Exception {
        when(properties.shippingPaymentType()).thenReturn(1);
        when(properties.requiredNote()).thenReturn("CHOXEMHANGKHONGTHU");
        when(client.post(eq("/shiip/public-api/v2/shipping-order/preview"), anyMap()))
            .thenReturn(json.readTree("{\"order_code\":\"\",\"total_fee\":20900}"));
        var quote = service.previewCart(List.of(item(true)), recipient(), new BigDecimal("100000"), new BigDecimal("120900"));
        assertThat(quote.fee()).isEqualByComparingTo("20900");
        ArgumentCaptor<Map<String,Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq("/shiip/public-api/v2/shipping-order/preview"), payload.capture());
        assertThat(payload.getValue()).containsEntry("cod_amount", 120900).containsEntry("payment_type_id", 1)
            .containsEntry("is_new_to_address", true).containsEntry("to_ward_name", "Phường Sài Gòn");
        verify(client, never()).post(eq("/shiip/public-api/v2/shipping-order/create"), anyMap());
    }

    @Test void unverifiedFreshProductCannotBeQuoted() {
        assertThatThrownBy(() -> service.previewCart(List.of(item(false)), recipient(), BigDecimal.TEN, BigDecimal.ZERO))
            .hasMessageContaining("GHN");
        verifyNoInteractions(client);
    }

    @Test void oversizedPackageCannotReachGhn() {
        CartItem line = item(true);
        line.setQuantity(100);
        assertThatThrownBy(() -> service.previewCart(List.of(line), recipient(), new BigDecimal("100000"), BigDecimal.ZERO))
            .hasMessageContaining("GHN");
        verifyNoInteractions(client);
    }

    @Test void fractionalCodCannotBeSilentlyTruncated() {
        assertThatThrownBy(() -> service.previewCart(List.of(item(true)), recipient(), new BigDecimal("100000"), new BigDecimal("100000.50")))
            .hasMessageContaining("VND");
        verifyNoInteractions(client);
    }

    @Test void missingTwoLevelAddressCannotBeQuoted() {
        var incomplete = new GhnShippingService.Recipient("Khách hàng", "0901234567", "1 Đường X", "Hồ Chí Minh", null);
        assertThatThrownBy(() -> service.previewCart(List.of(item(true)), incomplete,
            new BigDecimal("100000"), BigDecimal.ZERO)).hasMessageContaining("GHN");
        verifyNoInteractions(client);
    }

    @Test void prepaidOrderCreatesShipmentWithZeroCod() throws Exception {
        when(properties.shippingPaymentType()).thenReturn(1);
        when(properties.requiredNote()).thenReturn("CHOXEMHANGKHONGTHU");
        when(client.post(eq("/shiip/public-api/v2/shipping-order/create"), anyMap()))
            .thenReturn(json.readTree("{\"order_code\":\"LADFYR\",\"total_fee\":20900}"));
        Order order = new Order(); order.setSubtotal(new BigDecimal("100000"));
        order.setRecipientName("Khách hàng"); order.setRecipientPhone("0901234567");
        order.setShippingAddressLine("1 Đường X"); order.setShippingProvinceName("Hồ Chí Minh");
        order.setShippingWardName("Phường Sài Gòn");
        OrderItem item = new OrderItem(); item.setProductName("Sản phẩm thử nghiệm");
        Product product = new Product(); product.setGhnEligible(true); item.setProduct(product);
        item.setPrice(new BigDecimal("100000")); item.setQuantity(1);
        item.setPackageWeightG(600); item.setPackageLengthCm(20);
        item.setPackageWidthCm(15); item.setPackageHeightCm(10); order.addItem(item);
        assertThat(service.create(order,"GF-31",BigDecimal.ZERO).orderCode()).isEqualTo("LADFYR");
        ArgumentCaptor<Map<String,Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq("/shiip/public-api/v2/shipping-order/create"),payload.capture());
        assertThat(payload.getValue()).containsEntry("client_order_code","GF-31").containsEntry("cod_amount",0);
    }

    private CartItem item(boolean eligible) {
        Product product = new Product();
        product.setName("Sản phẩm thử nghiệm"); product.setPrice(new BigDecimal("100000"));
        product.setGhnEligible(eligible); product.setPackageWeightG(600);
        product.setPackageLengthCm(20); product.setPackageWidthCm(15); product.setPackageHeightCm(10);
        CartItem line = new CartItem(); line.setProduct(product); line.setQuantity(1);
        return line;
    }

    private GhnShippingService.Recipient recipient() {
        return new GhnShippingService.Recipient("Khách hàng", "0901234567", "1 Đường X",
            "Hồ Chí Minh", "Phường Sài Gòn");
    }
}
