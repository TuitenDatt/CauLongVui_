package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.BookingBundleDTO;
import com.example.CauLongVui.entity.*;
import com.example.CauLongVui.exception.BadRequestException;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingBundleService {

    private final BookingBundleRepository bundleRepository;
    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    /**
     * Request item cho từng sân trong cụm.
     */
    public record BundleCourtItem(Long courtId) {}

    /**
     * Tạo cụm đặt sân: cùng ngày, cùng giờ, nhiều sân.
     */
    @Transactional
    public BookingBundleDTO createBundle(
            Long userId,
            String bundleName,
            String note,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            List<Long> courtIds,
            Booking.PaymentMethod paymentMethod,
            String customerName,
            String customerPhone) {

        if (courtIds == null || courtIds.isEmpty()) {
            throw new BadRequestException("Phai chon it nhat 1 san.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Gio bat dau phai truoc gio ket thuc.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + userId));

        // Tính discount dựa trên membership
        double discountRate = 1.0;
        if (user.getMembershipTier() != null
                && user.getMembershipExpiry() != null
                && user.getMembershipExpiry().isAfter(LocalDateTime.now())) {
            if (user.getMembershipTier() == MembershipTier.PRO)  discountRate = 0.9;
            if (user.getMembershipTier() == MembershipTier.VIP)  discountRate = 0.8;
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();

        // Tạo Bundle trước (chưa có bookings)
        BookingBundle bundle = BookingBundle.builder()
                .owner(user)
                .bundleName(bundleName != null && !bundleName.isBlank()
                        ? bundleName
                        : customerName + " - " + bookingDate)
                .note(note)
                .status(BookingBundle.BundleStatus.ACTIVE)
                .build();
        bundle = bundleRepository.save(bundle);

        // Tạo từng Booking cho mỗi sân
        List<Booking> bookings = new ArrayList<>();
        double totalPrice = 0;

        for (Long courtId : courtIds) {
            Court court = courtRepository.findById(courtId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san: " + courtId));

            // Kiểm tra trùng lịch
            boolean overlap = bookingRepository.findByCourtIdAndBookingDate(courtId, bookingDate)
                    .stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                    .anyMatch(b -> startTime.isBefore(b.getEndTime()) && endTime.isAfter(b.getStartTime()));
            if (overlap) {
                throw new BadRequestException("San " + court.getName() + " da co nguoi dat trong khung gio nay.");
            }

            double price = (minutes / 60.0) * court.getPricePerHour() * discountRate;
            totalPrice += price;

            Booking booking = Booking.builder()
                    .court(court)
                    .user(user)
                    .bundle(bundle)
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .bookingDate(bookingDate)
                    .startTime(startTime)
                    .endTime(endTime)
                    .totalPrice(price)
                    .status(Booking.BookingStatus.PENDING)
                    .paymentMethod(paymentMethod)
                    .build();
            bookings.add(bookingRepository.save(booking));
        }

        // Cập nhật tổng giá và danh sách
        bundle.setTotalPrice(totalPrice);
        bundle.setBookings(bookings);
        bundle = bundleRepository.save(bundle);

        return BookingBundleDTO.fromEntity(bundle);
    }

    /**
     * Xác nhận thanh toán cho toàn cụm (sau khi thanh toán MoMo/ví thành công).
     */
    @Transactional
    public BookingBundleDTO confirmBundlePayment(Long bundleId, Booking.PaymentMethod method,
                                                  String paymentReference) {
        BookingBundle bundle = getBundle(bundleId);
        for (Booking b : bundle.getBookings()) {
            if (b.getStatus() == Booking.BookingStatus.PENDING) {
                b.setStatus(Booking.BookingStatus.CONFIRMED);
                b.setPaymentMethod(method);
                b.setPaymentReference(paymentReference);
                b.setPaidAt(LocalDateTime.now());
                bookingRepository.save(b);
            }
        }
        return BookingBundleDTO.fromEntity(bundle);
    }

    /**
     * Thanh toán cụm bằng ví (trừ tiền 1 lần).
     */
    @Transactional
    public BookingBundleDTO payBundleWithWallet(Long bundleId, Long userId) {
        BookingBundle bundle = getBundle(bundleId);
        if (!bundle.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Ban khong co quyen thanh toan cum nay.");
        }
        walletService.payBookingWithWallet(
                userId,
                BigDecimal.valueOf(bundle.getTotalPrice()),
                "BUNDLE-" + bundleId,
                "Thanh toan cum dat san #" + bundleId + " (" + bundle.getBundleName() + ")"
        );
        return confirmBundlePayment(bundleId, Booking.PaymentMethod.WALLET, "WALLET-BUNDLE-" + bundleId);
    }

    /**
     * Lấy danh sách cụm của user.
     */
    @Transactional(readOnly = true)
    public List<BookingBundleDTO> getBundlesByUserId(Long userId) {
        return bundleRepository.findByOwnerIdOrderByCreatedAtDesc(userId)
                .stream().map(BookingBundleDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết 1 cụm.
     */
    @Transactional(readOnly = true)
    public BookingBundleDTO getBundleById(Long bundleId) {
        return BookingBundleDTO.fromEntity(getBundle(bundleId));
    }

    /**
     * Đăng bán 1 sân riêng lẻ trong cụm.
     */
    @Transactional
    public BookingBundleDTO sellOneBooking(Long bundleId, Long bookingId, Long userId) {
        BookingBundle bundle = getBundle(bundleId);
        if (!bundle.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Ban khong co quyen thao tac cum nay.");
        }
        Booking booking = bundle.getBookings().stream()
                .filter(b -> b.getId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Booking khong thuoc cum nay."));

        if (Boolean.TRUE.equals(booking.getIsPass())) {
            throw new BadRequestException("San nay da duoc dang ban roi.");
        }
        booking.setIsPass(true);
        booking.setPassPrice(booking.getTotalPrice() != null ? booking.getTotalPrice() * 0.8 : 0.0);
        bookingRepository.save(booking);

        // Nếu tất cả đã bán → cập nhật bundle status
        boolean allSold = bundle.getBookings().stream().allMatch(b -> Boolean.TRUE.equals(b.getIsPass()));
        if (allSold) {
            bundle.setStatus(BookingBundle.BundleStatus.FULLY_SOLD);
            bundleRepository.save(bundle);
        }

        return BookingBundleDTO.fromEntity(getBundle(bundleId));
    }

    /**
     * Đăng bán tất cả sân trong cụm cùng lúc.
     */
    @Transactional
    public BookingBundleDTO sellAllBookings(Long bundleId, Long userId) {
        BookingBundle bundle = getBundle(bundleId);
        if (!bundle.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Ban khong co quyen thao tac cum nay.");
        }
        for (Booking b : bundle.getBookings()) {
            if (!Boolean.TRUE.equals(b.getIsPass())) {
                b.setIsPass(true);
                b.setPassPrice(b.getTotalPrice() != null ? b.getTotalPrice() * 0.8 : 0.0);
                bookingRepository.save(b);
            }
        }
        bundle.setStatus(BookingBundle.BundleStatus.FULLY_SOLD);
        bundleRepository.save(bundle);
        return BookingBundleDTO.fromEntity(bundle);
    }

    /**
     * Hủy cụm và hoàn tiền vào ví (nếu đã thanh toán).
     */
    @Transactional
    public BookingBundleDTO cancelBundle(Long bundleId, Long userId) {
        BookingBundle bundle = getBundle(bundleId);
        if (!bundle.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Ban khong co quyen huy cum nay.");
        }
        if (bundle.getStatus() == BookingBundle.BundleStatus.CANCELLED) {
            throw new BadRequestException("Cum nay da bi huy roi.");
        }

        boolean anyPaid = bundle.getBookings().stream().anyMatch(b -> b.getPaidAt() != null);
        for (Booking b : bundle.getBookings()) {
            if (b.getStatus() != Booking.BookingStatus.CANCELLED) {
                b.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(b);
            }
        }

        // Hoàn tiền 1 lần cho toàn cụm nếu đã thanh toán
        if (anyPaid && bundle.getTotalPrice() != null && bundle.getTotalPrice() > 0) {
            walletService.refundToWallet(
                    userId,
                    BigDecimal.valueOf(bundle.getTotalPrice()),
                    "BUNDLE-" + bundleId,
                    "Hoan tien huy cum dat san #" + bundleId
            );
        }

        bundle.setStatus(BookingBundle.BundleStatus.CANCELLED);
        return BookingBundleDTO.fromEntity(bundleRepository.save(bundle));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private BookingBundle getBundle(Long bundleId) {
        return bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cum dat san: " + bundleId));
    }
}
