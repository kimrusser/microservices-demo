package com.demo.repository;

import com.demo.entity.Order;
import com.demo.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(OrderStatus status);
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    Stream<Order> streamByCustomerId(String customerId);
    boolean existsByIdAndCustomerId(String id, String customerId);
}
