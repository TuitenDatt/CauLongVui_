package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhóm nhiều Booking thành 1 cụm (booking bundle).
 * Dùng cho trường hợp đặt nhiều sân cùng ngày cùng giờ (VD: công ty).
 */
@Entity
@Table(name = "booking_bundles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /** Tên cụm do user đặt, VD: "Công ty ABC - Thứ 5" */
    @Column(nullable = false, length = 200)
    private String bundleName;

    @Column(length = 500)
    private String note;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BundleStatus status = BundleStatus.ACTIVE;

    /** Tổng giá toàn cụm */
    @Column
    private Double totalPrice;

    @OneToMany(mappedBy = "bundle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    public enum BundleStatus {
        ACTIVE,       // Đang hoạt động
        FULLY_SOLD,   // Đã bán hết tất cả sân
        CANCELLED     // Đã hủy
    }
}
