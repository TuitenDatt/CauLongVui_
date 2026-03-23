package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {
    private Long id;
    private Long userId;
    private Long courtId;
    private String courtName;
    private String customerName;
    private String customerPhone;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Booking.BookingStatus status;
    private Double totalPrice;
    private Boolean isPass;
    private Double passPrice;

    public static BookingDTO fromEntity(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .courtId(booking.getCourt().getId())
                .courtName(booking.getCourt().getName())
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .isPass(booking.getIsPass())
                .passPrice(booking.getPassPrice())
                .build();
    }
}
