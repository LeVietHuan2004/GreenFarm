package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface BusinessReportRepository extends Repository<Order, Long> {
    @Query(value = "SELECT COALESCE(SUM(p.amount),0), COUNT(*) FROM payments p WHERE p.status='completed' AND p.paid_at >= :fromDate AND p.paid_at < :toDate", nativeQuery = true)
    Object[] totals(@Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);

    @Query(value = "SELECT DATE(p.paid_at), COALESCE(SUM(p.amount),0), COUNT(*) FROM payments p WHERE p.status='completed' AND p.paid_at >= :fromDate AND p.paid_at < :toDate GROUP BY DATE(p.paid_at) ORDER BY DATE(p.paid_at)", nativeQuery = true)
    List<Object[]> daily(@Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);

    @Query(value = "SELECT oi.product_id, oi.product_name, SUM(oi.quantity), SUM(oi.price * oi.quantity) FROM order_items oi JOIN payments p ON p.order_id=oi.order_id WHERE p.status='completed' AND p.paid_at >= :fromDate AND p.paid_at < :toDate GROUP BY oi.product_id, oi.product_name ORDER BY SUM(oi.quantity) DESC LIMIT 10", nativeQuery = true)
    List<Object[]> topProducts(@Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);

    @Query(value = "SELECT o.status, COUNT(*) FROM orders o WHERE o.created_at >= :fromDate AND o.created_at < :toDate GROUP BY o.status", nativeQuery = true)
    List<Object[]> orderStatuses(@Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM users u JOIN roles r ON r.id=u.role_id WHERE LOWER(r.name)='customer' AND u.created_at >= :fromDate AND u.created_at < :toDate", nativeQuery = true)
    long newCustomers(@Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM products WHERE stock > 0 AND stock <= :threshold AND status <> 'hidden'", nativeQuery = true)
    long lowStockCount(@Param("threshold") int threshold);

    @Query(value = "SELECT id, name, stock FROM products WHERE stock > 0 AND stock <= :threshold AND status <> 'hidden' ORDER BY stock ASC, id ASC LIMIT 10", nativeQuery = true)
    List<Object[]> lowStockProducts(@Param("threshold") int threshold);

    @Query(value = "SELECT c.id, c.code, c.coupon_type, COUNT(*), COALESCE(SUM(cu.discount_amount),0) FROM coupon_usages cu JOIN coupons c ON c.id=cu.coupon_id JOIN payments p ON p.order_id=cu.order_id WHERE cu.status IN ('RESERVED','USED') AND p.status='completed' AND p.paid_at >= :fromDate AND p.paid_at < :toDate GROUP BY c.id, c.code, c.coupon_type ORDER BY COUNT(*) DESC, SUM(cu.discount_amount) DESC", nativeQuery = true)
    List<Object[]> couponPerformance(@Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);
}
