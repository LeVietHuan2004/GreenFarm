package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.WishlistItemRequest;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {
    @Mock WishlistRepository wishlists;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    WishlistService service;

    @BeforeEach
    void setUp() {
        service = new WishlistService(wishlists, products, users);
        when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(new User()));
    }

    @Test
    void repeatedAddDoesNotCreateDuplicates() {
        when(wishlists.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(new Wishlist()));
        service.addItem(1L, new WishlistItemRequest(2L));
        verify(wishlists, never()).save(any());
        verifyNoInteractions(products);
    }

    @Test
    void allowsSavingOutOfStockProduct() {
        Product product = new Product();
        product.setCategory(new Category());
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(wishlists.save(any(Wishlist.class))).thenAnswer(call -> {
            Wishlist saved = call.getArgument(0);
            when(wishlists.findAllByUser_IdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(saved));
            return saved;
        });
        var result = service.addItem(1L, new WishlistItemRequest(2L));
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items().getFirst().product().status()).isEqualTo("out_of_stock");
    }

    @Test
    void rejectsHiddenAndMissingProducts() {
        Product product = new Product();
        product.setStatus(ProductStatus.HIDDEN);
        when(products.findById(2L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.addItem(1L, new WishlistItemRequest(2L)))
            .extracting("code").isEqualTo("PRODUCT_UNAVAILABLE");
        assertThatThrownBy(() -> service.addItem(1L, new WishlistItemRequest(3L)))
            .extracting("code").isEqualTo("PRODUCT_NOT_FOUND");
        verify(wishlists, never()).save(any());
    }

    @Test
    void removeIsScopedToCustomerAndSafeToRepeat() {
        Wishlist item = new Wishlist();
        when(wishlists.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(item), Optional.empty());
        service.removeItem(1L, 2L);
        service.removeItem(1L, 2L);
        verify(wishlists, times(1)).delete(item);
    }
}
