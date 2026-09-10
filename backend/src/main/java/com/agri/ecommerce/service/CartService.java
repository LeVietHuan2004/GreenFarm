package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.CartItemRequest;
import com.agri.ecommerce.dto.request.MergeCartRequest;
import com.agri.ecommerce.dto.request.UpdateCartItemRequest;
import com.agri.ecommerce.dto.response.CartItemResponse;
import com.agri.ecommerce.dto.response.CartResponse;
import com.agri.ecommerce.entity.CartItem;
import com.agri.ecommerce.entity.Product;
import com.agri.ecommerce.entity.ProductStatus;
import com.agri.ecommerce.mapper.CatalogMapper;
import com.agri.ecommerce.repository.CartItemRepository;
import com.agri.ecommerce.repository.ProductRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
        CartItemRepository cartItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository
    ) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return toResponse(cartItemRepository.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        lockCustomer(userId);
        addOrIncrement(userId, request.productId(), request.quantity());
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateItem(
        Long userId,
        Long itemId,
        UpdateCartItemRequest request
    ) {
        lockCustomer(userId);
        CartItem item = cartItemRepository.findByIdAndUser_Id(itemId, userId)
            .orElseThrow(this::cartItemNotFound);
        ensurePurchasable(item.getProduct());
        ensureStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        lockCustomer(userId);
        CartItem item = cartItemRepository.findByIdAndUser_Id(itemId, userId)
            .orElseThrow(this::cartItemNotFound);
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        lockCustomer(userId);
        cartItemRepository.deleteAllByUser_Id(userId);
        return new CartResponse(List.of(), 0, BigDecimal.ZERO);
    }

    @Transactional
    public CartResponse mergeCart(Long userId, MergeCartRequest request) {
        lockCustomer(userId);
        Map<Long, Long> quantitiesByProduct = new LinkedHashMap<>();
        for (CartItemRequest item : request.items()) {
            quantitiesByProduct.merge(
                item.productId(),
                item.quantity().longValue(),
                Long::sum
            );
        }

        quantitiesByProduct.forEach((productId, quantity) -> {
            if (quantity > Integer.MAX_VALUE) {
                throw insufficientStock();
            }
            addOrIncrement(userId, productId, quantity.intValue());
        });
        return getCart(userId);
    }

    private void addOrIncrement(Long userId, Long productId, int addedQuantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ApplicationException(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "Khong tim thay san pham"
            ));
        ensurePurchasable(product);

        CartItem item = cartItemRepository.findByUser_IdAndProduct_Id(userId, productId)
            .orElseGet(() -> {
                CartItem created = new CartItem();
                created.setUser(userRepository.getReferenceById(userId));
                created.setProduct(product);
                created.setQuantity(0);
                return created;
            });

        long finalQuantity = (long) item.getQuantity() + addedQuantity;
        if (finalQuantity > Integer.MAX_VALUE) {
            throw insufficientStock();
        }
        ensureStock(product, (int) finalQuantity);
        item.setQuantity((int) finalQuantity);
        cartItemRepository.save(item);
    }

    private void lockCustomer(Long userId) {
        userRepository.findByIdForCommerceUpdate(userId)
            .orElseThrow(() -> new ApplicationException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản"
            ));
    }

    private void ensurePurchasable(Product product) {
        if (product.getStatus() != ProductStatus.IN_STOCK || product.getStock() <= 0) {
            throw new ApplicationException(
                HttpStatus.CONFLICT,
                "PRODUCT_UNAVAILABLE",
                "Sản phẩm hiện không thể thêm vào giỏ hàng"
            );
        }
    }

    private void ensureStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw insufficientStock();
        }
    }

    private ApplicationException insufficientStock() {
        return new ApplicationException(
            HttpStatus.CONFLICT,
            "INSUFFICIENT_STOCK",
            "Số lượng yêu cầu vượt quá tồn kho hiện tại"
        );
    }

    private ApplicationException cartItemNotFound() {
        return new ApplicationException(
            HttpStatus.NOT_FOUND,
            "CART_ITEM_NOT_FOUND",
            "Không tìm thấy sản phẩm trong giỏ hàng"
        );
    }

    private CartResponse toResponse(List<CartItem> entities) {
        List<CartItemResponse> items = entities.stream()
            .map(item -> new CartItemResponse(
                item.getId(),
                item.getQuantity(),
                item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                CatalogMapper.toProductResponse(item.getProduct()),
                item.getCreatedAt(),
                item.getUpdatedAt()
            ))
            .toList();
        long totalItems = items.stream().mapToLong(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
            .map(CartItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, totalItems, subtotal);
    }
}
