package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.RacketRentalRequest;
import com.example.CauLongVui.dto.RacketRentalResponse;
import com.example.CauLongVui.service.RacketRentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/racket-rentals")
@RequiredArgsConstructor
public class RacketRentalController {

    private final RacketRentalService rentalService;

    @PostMapping
    public ResponseEntity<ApiResponse<RacketRentalResponse>> createOrder(
            @RequestBody RacketRentalRequest request) {
        RacketRentalResponse response = rentalService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt thuê vợt thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RacketRentalResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(rentalService.getAllOrders()));
    }
}
