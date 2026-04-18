package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.BookingBundle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingBundleDTO {

    private Long id;
    private Long ownerId;
    private String ownerName;
    private String bundleName;
    private String note;
    private LocalDateTime createdAt;
    private BookingBundle.BundleStatus status;
    private Double totalPrice;

    /** Ngày & giờ chung của cả cụm (đều giống nhau) */
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private int totalCourts;
    private int soldCourts;

    private List<BookingItem> bookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingItem {
        private Long bookingId;
        private Long courtId;
        private String courtName;
        private Double price;
        private Boolean isPass;
        private Double passPrice;
        private String status;
    }

    public static BookingBundleDTO fromEntity(BookingBundle bundle) {
        List<BookingItem> items = bundle.getBookings().stream()
                .map(b -> BookingItem.builder()
                        .bookingId(b.getId())
                        .courtId(b.getCourt().getId())
                        .courtName(b.getCourt().getName())
                        .price(b.getTotalPrice())
                        .isPass(b.getIsPass())
                        .passPrice(b.getPassPrice())
                        .status(b.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        // Lấy ngày/giờ từ booking đầu tiên (tất cả giống nhau)
        LocalDate date = null;
        LocalTime start = null;
        LocalTime end = null;
        if (!bundle.getBookings().isEmpty()) {
            var first = bundle.getBookings().get(0);
            date = first.getBookingDate();
            start = first.getStartTime();
            end = first.getEndTime();
        }

        long sold = bundle.getBookings().stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsPass())).count();

        return BookingBundleDTO.builder()
                .id(bundle.getId())
                .ownerId(bundle.getOwner().getId())
                .ownerName(bundle.getOwner().getFullName())
                .bundleName(bundle.getBundleName())
                .note(bundle.getNote())
                .createdAt(bundle.getCreatedAt())
                .status(bundle.getStatus())
                .totalPrice(bundle.getTotalPrice())
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .totalCourts(bundle.getBookings().size())
                .soldCourts((int) sold)
                .bookings(items)
                .build();
    }
}
