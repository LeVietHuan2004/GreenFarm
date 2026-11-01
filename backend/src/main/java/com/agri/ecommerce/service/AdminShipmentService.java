package com.agri.ecommerce.service;

import com.agri.ecommerce.dto.response.AdminShipmentResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.entity.Shipment;
import com.agri.ecommerce.repository.ShipmentRepository;
import java.util.ArrayList;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminShipmentService {
    private final ShipmentRepository shipments;

    public AdminShipmentService(ShipmentRepository shipments) { this.shipments = shipments; }

    @Transactional(readOnly = true)
    public PageResponse<AdminShipmentResponse> findAll(String search, String status, int page, int size) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String state = status == null ? "" : status.trim();
        Specification<Shipment> filter = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("provider"), "ghn"));
            if (!state.isEmpty()) predicates.add(cb.equal(root.get("shippingStatus"), state));
            if (!term.isEmpty()) {
                String like = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                var order = root.join("order");
                var matches = new ArrayList<Predicate>();
                matches.add(cb.like(cb.lower(root.get("providerOrderCode")), like, '\\'));
                matches.add(cb.like(cb.lower(order.get("recipientName")), like, '\\'));
                matches.add(cb.like(order.get("recipientPhone"), like, '\\'));
                try { matches.add(cb.equal(order.get("id"), Long.parseLong(term.replaceFirst("^#", "")))); }
                catch (NumberFormatException ignored) { }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(shipments.findAll(filter, pageable), shipment -> {
            var order = shipment.getOrder();
            return new AdminShipmentResponse(order.getId(), order.getStatus().getValue(), order.getRecipientName(),
                order.getRecipientPhone(), order.getShippingAddressLine(), order.getShippingCity(),
                shipment.getProviderOrderCode(), shipment.getShippingStatus(), shipment.getCarrierShippingFee(),
                shipment.getCustomerShippingFee(), shipment.getCodAmount(), shipment.getCreatedAt());
        });
    }
}
