package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.CartItemRequest;
import com.agri.ecommerce.dto.request.MergeCartRequest;
import com.agri.ecommerce.dto.request.UpdateCartItemRequest;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartItemRepository carts;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    CartService service;
    Product product;

    @BeforeEach
    void setUp() {
        service = new CartService(carts, products, users);
        product = new Product();
        product.setCategory(new Category());
        product.setStatus(ProductStatus.IN_STOCK);
        product.setStock(10);
        product.setPrice(new BigDecimal("12500.50"));
        lenient().when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(new User()));
    }

    private CartItem item(int quantity) {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void addsNewItemAndCalculatesTotalsFromCurrentPrice() {
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(carts.save(any(CartItem.class))).thenAnswer(call -> {
            CartItem saved = call.getArgument(0);
            when(carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(saved));
            return saved;
        });
        var response = service.addItem(1L, new CartItemRequest(2L, 3));
        assertThat(response.totalItems()).isEqualTo(3);
        assertThat(response.subtotal()).isEqualByComparingTo("37501.50");
        var order = inOrder(users, products, carts);
        order.verify(users).findByIdForCommerceUpdate(1L);
        order.verify(products).findById(2L);
        order.verify(carts).findByUser_IdAndProduct_Id(1L, 2L);
        order.verify(carts).save(any(CartItem.class));
    }

    @Test
    void repeatedAddIncrementsExistingItem() {
        CartItem existing = item(3);
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(carts.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(existing));
        service.addItem(1L, new CartItemRequest(2L, 2));
        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(carts).save(existing);
    }

    @Test
    void checksCombinedQuantityAgainstStock() {
        CartItem existing = item(8);
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(carts.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(2L, 3)))
            .isInstanceOf(ApplicationException.class).extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(existing.getQuantity()).isEqualTo(8);
        verify(carts, never()).save(any());
    }

    @Test
    void rejectsUnavailableProducts() {
        when(products.findById(2L)).thenReturn(Optional.of(product));
        for (ProductStatus status : List.of(ProductStatus.HIDDEN, ProductStatus.OUT_OF_STOCK)) {
            product.setStatus(status);
            assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(2L, 1)))
                .extracting("code").isEqualTo("PRODUCT_UNAVAILABLE");
        }
        product.setStatus(ProductStatus.IN_STOCK);
        product.setStock(0);
        assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(2L, 1)))
            .extracting("code").isEqualTo("PRODUCT_UNAVAILABLE");
        verify(carts, never()).save(any());
    }

    @Test
    void updateRejectsExcessAndAllowsReducingAfterStockChange() {
        CartItem existing = item(8);
        product.setStock(5);
        when(carts.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.updateItem(1L, 10L, new UpdateCartItemRequest(6)))
            .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        service.updateItem(1L, 10L, new UpdateCartItemRequest(5));
        assertThat(existing.getQuantity()).isEqualTo(5);
    }

    @Test
    void cannotUpdateOrRemoveAnotherCustomersItem() {
        assertThatThrownBy(() -> service.updateItem(1L, 20L, new UpdateCartItemRequest(1)))
            .extracting("code").isEqualTo("CART_ITEM_NOT_FOUND");
        assertThatThrownBy(() -> service.removeItem(1L, 20L))
            .extracting("code").isEqualTo("CART_ITEM_NOT_FOUND");
        verify(carts, never()).save(any());
        verify(carts, never()).delete(any());
    }

    @Test
    void canRemoveUnavailableProductAndClearOnlyOwnCart() {
        CartItem existing = item(1);
        product.setStatus(ProductStatus.HIDDEN);
        when(carts.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(existing));
        service.removeItem(1L, 10L);
        verify(carts).delete(existing);
        var response = service.clearCart(1L);
        verify(carts).deleteAllByUser_Id(1L);
        assertThat(response.items()).isEmpty();
        assertThat(response.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void mergeCombinesDuplicateProductsBeforeCheckingStock() {
        when(products.findById(2L)).thenReturn(Optional.of(product));
        CartItem existing = item(1);
        when(carts.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(existing));
        service.mergeCart(1L, new MergeCartRequest(List.of(new CartItemRequest(2L, 2), new CartItemRequest(2L, 3))));
        assertThat(existing.getQuantity()).isEqualTo(6);
        verify(carts, times(1)).save(existing);
    }

    @Test
    void mergeRejectsQuantityOverflow() {
        assertThatThrownBy(() -> service.mergeCart(1L, new MergeCartRequest(List.of(
            new CartItemRequest(2L, Integer.MAX_VALUE), new CartItemRequest(2L, 1)
        )))).extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        verify(carts, never()).save(any());
    }

    @Test
    void emptyCartHasZeroTotals() {
        var response = service.getCart(1L);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalItems()).isZero();
        assertThat(response.subtotal()).isEqualByComparingTo("0");
    }
}
