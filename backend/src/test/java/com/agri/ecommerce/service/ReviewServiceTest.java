package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.ReviewRequest;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock ReviewRepository reviews; @Mock ProductRepository products; @Mock UserRepository users; @Mock OrderRepository orders;
    ReviewService service;
    @BeforeEach void setUp(){service=new ReviewService(reviews,products,users,orders);}

    @Test void rejectsReviewWhenProductWasNotDelivered(){
        when(orders.hasPurchasedProduct(eq(1L),eq(2L),anyList())).thenReturn(false);
        assertThatThrownBy(()->service.create(1L,new ReviewRequest(2L,5,"Tươi ngon")))
            .extracting("code").isEqualTo("REVIEW_PURCHASE_REQUIRED");
        verify(reviews,never()).save(any());
    }

    @Test void createsOneReviewForDeliveredProduct(){
        User user=mock(User.class); when(user.getId()).thenReturn(1L); when(user.getName()).thenReturn("Khách hàng");
        Product product=mock(Product.class); when(product.getId()).thenReturn(2L);
        when(orders.hasPurchasedProduct(eq(1L),eq(2L),anyList())).thenReturn(true);
        when(reviews.findByUser_IdAndProduct_Id(1L,2L)).thenReturn(Optional.empty());
        when(users.getReferenceById(1L)).thenReturn(user); when(products.findById(2L)).thenReturn(Optional.of(product));
        when(reviews.save(any(Review.class))).thenAnswer(call->call.getArgument(0));
        var result=service.create(1L,new ReviewRequest(2L,5,"  Tươi ngon  "));
        assertThat(result.rating()).isEqualTo(5); assertThat(result.comment()).isEqualTo("Tươi ngon");
    }
}
