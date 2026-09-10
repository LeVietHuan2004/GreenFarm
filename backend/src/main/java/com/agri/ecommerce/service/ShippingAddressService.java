package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.ShippingAddressRequest;
import com.agri.ecommerce.dto.response.ShippingAddressResponse;
import com.agri.ecommerce.entity.ShippingAddress;
import com.agri.ecommerce.repository.OrderRepository;
import com.agri.ecommerce.repository.ShippingAddressRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingAddressService {
    private final ShippingAddressRepository addresses;
    private final OrderRepository orders;
    private final UserRepository users;

    public ShippingAddressService(ShippingAddressRepository addresses, OrderRepository orders, UserRepository users) {
        this.addresses = addresses; this.orders = orders; this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ShippingAddressResponse> findAll(Long userId) {
        return addresses.findAllByUser_IdOrderByDefaultAddressDescCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShippingAddressResponse create(Long userId, ShippingAddressRequest request) {
        lockUser(userId);
        boolean makeDefault = request.defaultAddress() || addresses.countByUser_Id(userId) == 0;
        if (makeDefault) clearDefaults(userId, null);
        ShippingAddress address = new ShippingAddress();
        address.setUser(users.getReferenceById(userId));
        apply(address, request, makeDefault);
        return toResponse(addresses.save(address));
    }

    @Transactional
    public ShippingAddressResponse update(Long userId, Long addressId, ShippingAddressRequest request) {
        lockUser(userId);
        ShippingAddress address = findEntity(userId, addressId);
        boolean makeDefault = request.defaultAddress() || address.isDefaultAddress();
        if (request.defaultAddress()) clearDefaults(userId, addressId);
        apply(address, request, makeDefault);
        return toResponse(addresses.save(address));
    }

    @Transactional
    public List<ShippingAddressResponse> remove(Long userId, Long addressId) {
        lockUser(userId);
        ShippingAddress address = findEntity(userId, addressId);
        if (orders.existsByShippingAddress_Id(addressId)) {
            throw new ApplicationException(HttpStatus.CONFLICT, "ADDRESS_IN_USE", "Địa chỉ đã được dùng cho đơn hàng và không thể xóa");
        }
        boolean wasDefault = address.isDefaultAddress();
        addresses.delete(address);
        addresses.flush();
        if (wasDefault) {
            addresses.findAllByUser_IdOrderByDefaultAddressDescCreatedAtDesc(userId).stream().findFirst().ifPresent(next -> {
                next.setDefaultAddress(true); addresses.save(next);
            });
        }
        return findAll(userId);
    }

    @Transactional
    public ShippingAddressResponse setDefault(Long userId, Long addressId) {
        lockUser(userId);
        ShippingAddress address = findEntity(userId, addressId);
        clearDefaults(userId, addressId);
        address.setDefaultAddress(true);
        return toResponse(addresses.save(address));
    }

    @Transactional(readOnly = true)
    public ShippingAddress findEntity(Long userId, Long addressId) {
        return addresses.findByIdAndUser_Id(addressId, userId).orElseThrow(() ->
            new ApplicationException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ giao hàng"));
    }

    private void clearDefaults(Long userId, Long exceptId) {
        var defaults = addresses.findAllByUser_IdAndDefaultAddressTrue(userId);
        defaults.stream().filter(item -> exceptId == null || !item.getId().equals(exceptId)).forEach(item -> item.setDefaultAddress(false));
        addresses.saveAll(defaults);
    }
    private void apply(ShippingAddress target, ShippingAddressRequest source, boolean defaultAddress) {
        target.setFullName(source.fullName().trim()); target.setPhone(source.phone().trim());
        target.setAddress(source.address().trim()); target.setCity(source.city().trim()); target.setDefaultAddress(defaultAddress);
    }
    private void lockUser(Long userId) { users.findByIdForCommerceUpdate(userId).orElseThrow(() ->
        new ApplicationException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản")); }
    private ShippingAddressResponse toResponse(ShippingAddress item) { return new ShippingAddressResponse(item.getId(), item.getFullName(), item.getPhone(), item.getAddress(), item.getCity(), item.isDefaultAddress(), item.getCreatedAt(), item.getUpdatedAt()); }
}
