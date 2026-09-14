package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.agri.ecommerce.dto.request.*;
import jakarta.validation.Validation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommerceValidationTest {
    @Test
    void rejectsInvalidQuantityIdsAndMalformedMergeItems() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Integer quantity : Arrays.asList(null, 0, -1)) {
                assertThat(validator.validate(new CartItemRequest(1L, quantity))).isNotEmpty();
                assertThat(validator.validate(new UpdateCartItemRequest(quantity))).isNotEmpty();
            }
            assertThat(validator.validate(new CartItemRequest(-1L, 1))).isNotEmpty();
            assertThat(validator.validate(new WishlistItemRequest(null))).isNotEmpty();
            assertThat(validator.validate(new MergeCartRequest(null))).isNotEmpty();
            assertThat(validator.validate(new MergeCartRequest(Arrays.asList((CartItemRequest) null)))).isNotEmpty();
            assertThat(validator.validate(new MergeCartRequest(List.of(new CartItemRequest(1L, 0))))).isNotEmpty();
            assertThat(validator.validate(new MergeCartRequest(Collections.nCopies(101, new CartItemRequest(1L, 1))))).isNotEmpty();
            assertThat(validator.validate(new MergeCartRequest(List.of(new CartItemRequest(1L, 1))))).isEmpty();
        }
    }

    @Test
    void rejectsContactMessagesShorterThanTenCharacters() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new ContactRequest("Khách hàng", null, null, "quá ngắn"))).isNotEmpty();
            assertThat(validator.validate(new ContactRequest("Khách hàng", null, null, "Nội dung đủ dài"))).isEmpty();
        }
    }
}
