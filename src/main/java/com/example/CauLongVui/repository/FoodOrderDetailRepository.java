package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.FoodOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodOrderDetailRepository extends JpaRepository<FoodOrderDetail, Long> {
    List<FoodOrderDetail> findByFoodOrderId(Long foodOrderId);
}
