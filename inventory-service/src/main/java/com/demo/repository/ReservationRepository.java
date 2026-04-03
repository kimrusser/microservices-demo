package com.demo.repository;

import com.demo.entity.Reservation;
import com.demo.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {

    Optional<Reservation> findByOrderId(String orderId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByProductId(String productId);

    boolean existsByOrderId(String orderId);
}
