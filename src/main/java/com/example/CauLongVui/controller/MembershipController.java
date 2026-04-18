package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.entity.MembershipPlan;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.repository.MembershipPlanRepository;
import com.example.CauLongVui.repository.UserRepository;
import com.example.CauLongVui.service.MembershipService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;
    private final MembershipPlanRepository planRepository;
    private final UserRepository userRepository;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<MembershipPlan>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getAllPlans()));
    }

    /**
     * Trả về giá thực tế phải trả sau khi khấu trừ giá trị còn lại của gói cũ.
     */
    @GetMapping("/upgrade-cost")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUpgradeCost(
            @RequestParam Long planId,
            @RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        long effectivePrice = membershipService.calculateEffectivePrice(user, plan);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "originalPrice", plan.getPrice(),
                "effectivePrice", effectivePrice,
                "discount", plan.getPrice() - effectivePrice
        )));
    }

    @PostMapping("/purchase/{planId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purchasePlan(
            @PathVariable Long planId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "momo") String method,
            HttpServletRequest request) throws Exception {
        String result = membershipService.initiatePurchase(planId, userId, method, request);
        if ("WALLET_SUCCESS".equals(result)) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("success", true, "message", "Thanh toan thanh cong!")));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("paymentUrl", result)));
    }

    @PostMapping("/momo-callback")
    public ResponseEntity<Void> momoCallback(@RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("orderId");
        Integer resultCode = (Integer) payload.get("resultCode");

        if (orderId != null && orderId.startsWith("MB-")) {
            String[] parts = orderId.split("-");
            if (parts.length >= 2) {
                Long subId = Long.parseLong(parts[1]);
                if (resultCode != null && resultCode == 0) {
                    membershipService.completePurchase(subId);
                } else {
                    membershipService.cancelPurchase(subId);
                }
            }
        }
        return ResponseEntity.ok().build();
    }
}
