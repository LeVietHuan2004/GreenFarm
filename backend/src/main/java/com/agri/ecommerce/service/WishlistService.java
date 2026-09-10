package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.WishlistItemRequest;
import com.agri.ecommerce.dto.response.WishlistItemResponse;
import com.agri.ecommerce.dto.response.WishlistResponse;
import com.agri.ecommerce.entity.Product;
import com.agri.ecommerce.entity.ProductStatus;
import com.agri.ecommerce.entity.Wishlist;
import com.agri.ecommerce.mapper.CatalogMapper;
import com.agri.ecommerce.repository.ProductRepository;
import com.agri.ecommerce.repository.UserRepository;
import com.agri.ecommerce.repository.WishlistRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
        WishlistRepository wishlistRepository,
        ProductRepository productRepository,
        UserRepository userRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        return toResponse(
            wishlistRepository.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId)
        );
    }

    @Transactional
    public WishlistResponse addItem(Long userId, WishlistItemRequest request) {
        lockCustomer(userId);
        if (wishlistRepository.findByUser_IdAndProduct_Id(userId, request.productId()).isEmpty()) {
            Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ApplicationException(
                    HttpStatus.NOT_FOUND,
                    "PRODUCT_NOT_FOUND",
                    "Khong tim thay san pham"
                ));
            if (product.getStatus() == ProductStatus.HIDDEN) {
                throw new ApplicationException(
                    HttpStatus.CONFLICT,
                    "PRODUCT_UNAVAILABLE",
                    "San pham hien khong kha dung"
                );
            }

            Wishlist wishlist = new Wishlist();
            wishlist.setUser(userRepository.getReferenceById(userId));
            wishlist.setProduct(product);
            wishlistRepository.save(wishlist);
        }
        return getWishlist(userId);
    }

    @Transactional
    public WishlistResponse removeItem(Long userId, Long productId) {
        lockCustomer(userId);
        wishlistRepository.findByUser_IdAndProduct_Id(userId, productId)
            .ifPresent(wishlistRepository::delete);
        return getWishlist(userId);
    }

    private WishlistResponse toResponse(List<Wishlist> entities) {
        List<WishlistItemResponse> items = entities.stream()
            .map(item -> new WishlistItemResponse(
                item.getId(),
                CatalogMapper.toProductResponse(item.getProduct()),
                item.getCreatedAt()
            ))
            .toList();
        return new WishlistResponse(items, items.size());
    }

    private void lockCustomer(Long userId) {
        userRepository.findByIdForCommerceUpdate(userId)
            .orElseThrow(() -> new ApplicationException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản"
            ));
    }
}
