package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.FoodOrderRequest;
import com.example.CauLongVui.dto.FoodOrderResponse;
import com.example.CauLongVui.dto.PaginationResponse;
import com.example.CauLongVui.entity.FoodOrder;
import com.example.CauLongVui.service.FoodOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/food-orders")
@RequiredArgsConstructor
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<FoodOrderResponse>> createOrder(
            @RequestBody FoodOrderRequest request) {
        FoodOrderResponse response = foodOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<FoodOrderResponse>>> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        PaginationResponse<FoodOrderResponse> response = foodOrderService.getOrders(page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getOrderById(@PathVariable Long id) {
        FoodOrderResponse response = foodOrderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        FoodOrder.OrderStatus status = FoodOrder.OrderStatus.valueOf(body.get("status"));
        FoodOrderResponse response = foodOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
