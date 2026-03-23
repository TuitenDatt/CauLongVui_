package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.RacketRentalOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RacketRentalOrderRepository extends JpaRepository<RacketRentalOrder, Long> {
    Page<RacketRentalOrder> findByCustomerPhone(String customerPhone, Pageable pageable);
    Page<RacketRentalOrder> findByUserId(Long userId, Pageable pageable);
    java.util.List<RacketRentalOrder> findByStatusIn(java.util.List<RacketRentalOrder.OrderStatus> statuses);
}
