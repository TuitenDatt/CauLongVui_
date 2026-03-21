package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.FoodOrderRequest;
import com.example.CauLongVui.dto.FoodOrderResponse;
import com.example.CauLongVui.dto.PaginationResponse;
import com.example.CauLongVui.entity.FoodOrder;

public interface FoodOrderService {
    FoodOrderResponse createOrder(FoodOrderRequest request);
    PaginationResponse<FoodOrderResponse> getOrders(int page, int limit);
    FoodOrderResponse getOrderById(Long id);
    FoodOrderResponse updateOrderStatus(Long id, FoodOrder.OrderStatus status);
}
