package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.*;
import com.example.CauLongVui.entity.*;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderDetailRepository foodOrderDetailRepository;
    private final ProductRepository productRepository;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public FoodOrderResponse createOrder(FoodOrderRequest request) {
        FoodOrder order = FoodOrder.builder()
                .customerName(request.getCustomerName())
                .status(FoodOrder.OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .totalAmount(0.0)
                .build();

        if (request.getCourtId() != null) {
            Court court = courtRepository.findById(request.getCourtId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Sân cầu lông với ID: " + request.getCourtId()));
            order.setCourt(court);
        }

        if (request.getBookingId() != null) {
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thông tin đặt sân với ID: " + request.getBookingId()));
            order.setBooking(booking);
        }

        if (request.getDeliveryTime() != null && !request.getDeliveryTime().isEmpty()) {
            order.setDeliveryTime(LocalTime.parse(request.getDeliveryTime(), DateTimeFormatter.ofPattern("HH:mm")));
        }

        FoodOrder savedOrder = foodOrderRepository.save(order);

        double totalAmount = 0.0;
        List<FoodOrderDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (OrderItemRequest item : request.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Sản phẩm với ID: " + item.getProductId()));
                
                if (product.getStockQuantity() < item.getQuantity()) {
                    throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' không đủ số lượng tồn kho.");
                }
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);

                double subTotal = product.getPrice() * item.getQuantity();
                totalAmount += subTotal;

                FoodOrderDetail detail = FoodOrderDetail.builder()
                        .foodOrder(savedOrder)
                        .product(product)
                        .quantity(item.getQuantity())
                        .unitPrice(product.getPrice())
                        .build();
                details.add(detail);
            }
        }

        foodOrderDetailRepository.saveAll(details);
        savedOrder.setTotalAmount(totalAmount);
        foodOrderRepository.save(savedOrder);

        return mapToResponse(savedOrder, details);
    }

    @Override
    public PaginationResponse<FoodOrderResponse> getOrders(int page, int limit) {
        Page<FoodOrder> ordersPage = foodOrderRepository.findAll(PageRequest.of(page - 1, limit));
        List<FoodOrderResponse> responses = ordersPage.getContent().stream().map(order -> {
            List<FoodOrderDetail> details = foodOrderDetailRepository.findByFoodOrderId(order.getId());
            return mapToResponse(order, details);
        }).collect(Collectors.toList());

        return PaginationResponse.<FoodOrderResponse>builder()
                .items(responses)
                .pagination(PaginationResponse.Pagination.builder()
                        .page(page)
                        .limit(limit)
                        .total(ordersPage.getTotalElements())
                        .build())
                .build();
    }

    @Override
    public FoodOrderResponse getOrderById(Long id) {
        FoodOrder order = foodOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đơn hàng với ID: " + id));
        List<FoodOrderDetail> details = foodOrderDetailRepository.findByFoodOrderId(id);
        return mapToResponse(order, details);
    }

    @Override
    @Transactional
    public FoodOrderResponse updateOrderStatus(Long id, FoodOrder.OrderStatus status) {
        FoodOrder order = foodOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đơn hàng với ID: " + id));
        order.setStatus(status);
        foodOrderRepository.save(order);
        List<FoodOrderDetail> details = foodOrderDetailRepository.findByFoodOrderId(id);
        return mapToResponse(order, details);
    }

    private FoodOrderResponse mapToResponse(FoodOrder order, List<FoodOrderDetail> details) {
        List<FoodOrderDetailResponse> itemResponses = new ArrayList<>();
        if (details != null) {
            itemResponses = details.stream().map(d -> FoodOrderDetailResponse.builder()
                    .id(d.getId())
                    .productId(d.getProduct().getId())
                    .productName(d.getProduct().getName())
                    .quantity(d.getQuantity())
                    .unitPrice(d.getUnitPrice())
                    .subTotal(d.getUnitPrice() * d.getQuantity())
                    .build()).collect(Collectors.toList());
        }

        return FoodOrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .courtId(order.getCourt() != null ? order.getCourt().getId() : null)
                .courtName(order.getCourt() != null ? order.getCourt().getName() : null)
                .deliveryTime(order.getDeliveryTime())
                .bookingId(order.getBooking() != null ? order.getBooking().getId() : null)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .items(itemResponses)
                .build();
    }
}
