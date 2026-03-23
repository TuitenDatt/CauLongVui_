package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.RacketRentalRequest;
import com.example.CauLongVui.dto.RacketRentalResponse;
import com.example.CauLongVui.entity.Racket;
import com.example.CauLongVui.entity.RacketRentalOrder;
import com.example.CauLongVui.entity.RacketRentalOrderDetail;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.repository.RacketRentalOrderRepository;
import com.example.CauLongVui.repository.RacketRepository;
import com.example.CauLongVui.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RacketRentalService {

    private final RacketRentalOrderRepository orderRepository;
    private final RacketRepository racketRepository;
    private final UserRepository userRepository;

    @Transactional
    public RacketRentalResponse createOrder(RacketRentalRequest request) {
        if (request.getCccdImageUrl() == null || request.getCccdImageUrl().trim().isEmpty()) {
            throw new RuntimeException("Hình ảnh CCCD/CMND là bắt buộc để thuê vợt.");
        }

        User user = null;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username != null && !username.equals("anonymousUser")) {
                user = userRepository.findByEmail(username).orElse(null);
            }
        } catch (Exception e) {
            // ignore
        }

        RacketRentalOrder order = new RacketRentalOrder();
        order.setUser(user);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCourtName(request.getCourtName());
        order.setBookingId(request.getBookingId());
        order.setCccdImageUrl(request.getCccdImageUrl());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(RacketRentalOrder.OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        double totalAmount = 0.0;
        List<RacketRentalOrderDetail> details = request.getItems().stream().map(itemReq -> {
            Racket racket = racketRepository.findById(itemReq.getRacketId())
                    .orElseThrow(() -> new RuntimeException("Racket not found: " + itemReq.getRacketId()));

            if (racket.getStockQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Không đủ số lượng cho vợt: " + racket.getName() + " (còn " + racket.getStockQuantity() + ")");
            }

            racket.setStockQuantity(racket.getStockQuantity() - itemReq.getQuantity());
            racketRepository.save(racket);

            RacketRentalOrderDetail detail = new RacketRentalOrderDetail();
            detail.setRentalOrder(order);
            detail.setRacket(racket);
            detail.setQuantity(itemReq.getQuantity());
            detail.setUnitPrice(racket.getRentalPrice());
            return detail;
        }).collect(Collectors.toList());

        for (RacketRentalOrderDetail detail : details) {
            totalAmount += detail.getQuantity() * detail.getUnitPrice();
        }

        order.setDetails(details);
        order.setTotalAmount(totalAmount);

        RacketRentalOrder savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    public List<RacketRentalResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RacketRentalResponse mapToResponse(RacketRentalOrder order) {
        List<RacketRentalResponse.RentalItemResponse> itemResponses = order.getDetails().stream().map(d ->
                RacketRentalResponse.RentalItemResponse.builder()
                        .racketId(d.getRacket().getId())
                        .racketName(d.getRacket().getName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .imageUrl(d.getRacket().getImageUrl())
                        .build()
        ).collect(Collectors.toList());

        return RacketRentalResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .courtName(order.getCourtName())
                .cccdImageUrl(order.getCccdImageUrl())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .orderDate(order.getOrderDate())
                .items(itemResponses)
                .build();
    }
}
