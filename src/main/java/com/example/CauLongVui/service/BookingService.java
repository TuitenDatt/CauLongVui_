package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.BookingDTO;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.entity.Court;
import com.example.CauLongVui.exception.BadRequestException;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.BookingRepository;
import com.example.CauLongVui.repository.CourtRepository;
import com.example.CauLongVui.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final BookingHoldService bookingHoldService;
    private final WalletService walletService;

    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay dat san voi ID: " + id));
        return BookingDTO.fromEntity(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByCourtId(Long courtId) {
        return bookingRepository.findByCourtId(courtId).stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByPhone(String phone) {
        return bookingRepository.findByCustomerPhoneOrderByBookingDateDesc(phone).stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserIdOrderByBookingDateDesc(userId).stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getPassBookings() {
        return bookingRepository.findByIsPassTrueOrderByBookingDateDesc().stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getBookedSlots(Long courtId, LocalDate date) {
        return bookingRepository.findByCourtIdAndBookingDate(courtId, date).stream()
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .map(b -> Map.of(
                        "startTime", b.getStartTime().toString(),
                        "endTime", b.getEndTime().toString()
                ))
                .collect(Collectors.toList());
    }

    public BookingDTO createBooking(BookingDTO bookingDTO, String holdId) {
        Court court = courtRepository.findById(bookingDTO.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san voi ID: " + bookingDTO.getCourtId()));

        if (!bookingDTO.getStartTime().isBefore(bookingDTO.getEndTime())) {
            throw new BadRequestException("Gio bat dau phai truoc gio ket thuc");
        }

        if (holdId != null && !holdId.isBlank()) {
            boolean holdValid = bookingHoldService.confirmHold(holdId);
            if (!holdValid) {
                throw new BadRequestException("Giu cho da het han hoac khong hop le. Vui long giu cho lai.");
            }
        } else {
            List<Booking> dayBookings = bookingRepository.findByCourtIdAndBookingDate(
                    bookingDTO.getCourtId(),
                    bookingDTO.getBookingDate()
            );
            boolean hasOverlap = dayBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                    .anyMatch(b -> bookingDTO.getStartTime().isBefore(b.getEndTime())
                            && bookingDTO.getEndTime().isAfter(b.getStartTime()));

            if (hasOverlap) {
                throw new BadRequestException("San da duoc dat trong khung gio nay. Vui long chon khung gio khac.");
            }
        }

        long minutes = Duration.between(bookingDTO.getStartTime(), bookingDTO.getEndTime()).toMinutes();
        double totalPrice = (minutes / 60.0) * court.getPricePerHour();

        com.example.CauLongVui.entity.User user = null;
        if (bookingDTO.getUserId() != null) {
            user = userRepository.findById(bookingDTO.getUserId()).orElse(null);
            if (user != null && user.getMembershipTier() != null
                    && user.getMembershipExpiry() != null
                    && user.getMembershipExpiry().isAfter(LocalDateTime.now())) {
                if (user.getMembershipTier() == com.example.CauLongVui.entity.MembershipTier.PRO) {
                    totalPrice *= 0.9;
                } else if (user.getMembershipTier() == com.example.CauLongVui.entity.MembershipTier.VIP) {
                    totalPrice *= 0.8;
                }
            }
        }

        // ── Book-now-pay-later: chỉ PRO/VIP mới được phép ──
        boolean isBookNowPayLater = bookingDTO.getPaymentMethod() == Booking.PaymentMethod.BOOK_NOW_PAY_LATER;
        if (isBookNowPayLater) {
            if (user == null) throw new BadRequestException("Phai dang nhap de dung tinh nang dat truoc.");
            var tier = user.getMembershipTier();
            boolean eligible = (tier == com.example.CauLongVui.entity.MembershipTier.PRO
                    || tier == com.example.CauLongVui.entity.MembershipTier.VIP)
                    && user.getMembershipExpiry() != null
                    && user.getMembershipExpiry().isAfter(LocalDateTime.now());
            if (!eligible) throw new BadRequestException("Chi thanh vien PRO/VIP moi duoc dat truoc - thanh toan sau.");
        }

        // Tính paymentDeadline nếu đặt trước
        LocalDateTime paymentDeadline = null;
        if (isBookNowPayLater) {
            LocalDateTime matchStart = bookingDTO.getBookingDate().atTime(bookingDTO.getStartTime());
            var tier = user.getMembershipTier();
            if (tier == com.example.CauLongVui.entity.MembershipTier.VIP) {
                paymentDeadline = matchStart.minusHours(2);  // VIP: phải trả trước 2 tiếng
            } else {
                paymentDeadline = matchStart.minusHours(24); // PRO: phải trả trước 24 tiếng
            }
            // Deadline không được trong quá khứ
            if (paymentDeadline.isBefore(LocalDateTime.now())) {
                paymentDeadline = LocalDateTime.now().plusMinutes(30);
            }
        }

        Booking booking = Booking.builder()
                .court(court)
                .user(user)
                .customerName(bookingDTO.getCustomerName())
                .customerPhone(bookingDTO.getCustomerPhone())
                .bookingDate(bookingDTO.getBookingDate())
                .startTime(bookingDTO.getStartTime())
                .endTime(bookingDTO.getEndTime())
                .totalPrice(totalPrice)
                .status(isBookNowPayLater ? Booking.BookingStatus.CONFIRMED : Booking.BookingStatus.PENDING)
                .paymentMethod(bookingDTO.getPaymentMethod())
                .paymentDeadline(paymentDeadline)
                .build();

        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    public BookingDTO createBooking(BookingDTO bookingDTO) {
        return createBooking(bookingDTO, null);
    }

    /**
     * PRO/VIP thanh toán sau khi đã đặt trước.
     * Hỗ trợ method: WALLET hoặc MOMO (redirect).
     */
    @Transactional
    public BookingDTO payDeposit(Long bookingId, Long userId, Booking.PaymentMethod method,
                                  String paymentReference) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking: " + bookingId));
        if (booking.getPaidAt() != null) {
            throw new BadRequestException("Booking nay da duoc thanh toan.");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking da bi huy, khong the thanh toan.");
        }
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Ban khong co quyen thanh toan booking nay.");
        }

        if (method == Booking.PaymentMethod.WALLET) {
            walletService.payBookingWithWallet(
                    userId, BigDecimal.valueOf(booking.getTotalPrice()),
                    "BOOKING-" + bookingId, "Thanh toan dat san #" + bookingId);
        }

        booking.setPaymentMethod(method);
        booking.setPaymentReference(paymentReference);
        booking.setPaidAt(LocalDateTime.now());
        booking.setPaymentDeadline(null); // clear deadline
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    public BookingDTO updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay dat san voi ID: " + id));

        if (status == Booking.BookingStatus.CANCELLED
                && booking.getStatus() != Booking.BookingStatus.CANCELLED
                && booking.getPaidAt() != null
                && booking.getRefundedAt() == null
                && booking.getUser() != null
                && booking.getTotalPrice() != null
                && booking.getTotalPrice() > 0) {
            walletService.refundToWallet(
                    booking.getUser().getId(),
                    BigDecimal.valueOf(booking.getTotalPrice()),
                    "BOOKING-" + booking.getId(),
                    "Hoan tien booking #" + booking.getId()
            );
            booking.setRefundedAt(LocalDateTime.now());
        }

        booking.setStatus(status);
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    public BookingDTO markBookingPaid(Long id, Booking.PaymentMethod paymentMethod, String paymentReference) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking voi ID: " + id));

        if (booking.getPaidAt() != null) {
            return BookingDTO.fromEntity(booking);
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking da bi huy, khong the xac nhan thanh toan");
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentReference(paymentReference);
        booking.setPaidAt(LocalDateTime.now());
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    public BookingDTO markBookingPaymentFailed(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking voi ID: " + id));

        if (booking.getPaidAt() == null && booking.getStatus() != Booking.BookingStatus.CANCELLED) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            booking = bookingRepository.save(booking);
        }
        return BookingDTO.fromEntity(booking);
    }

    @Transactional
    public BookingDTO updateBookingToPass(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay dat san voi ID: " + id));
        booking.setIsPass(true);
        booking.setPassPrice(booking.getTotalPrice() != null ? booking.getTotalPrice() * 0.8 : 0.0);
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDTO buyPassBooking(Long id, Long newUserId, String newCustomerName, String newCustomerPhone) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay dat san voi ID: " + id));
        if (!Boolean.TRUE.equals(booking.getIsPass())) {
            throw new BadRequestException("San nay khong duoc dang ban lai.");
        }

        booking.setIsPass(false);
        if (booking.getPassPrice() != null) {
            booking.setTotalPrice(booking.getPassPrice());
        }
        booking.setPassPrice(null);

        if (newUserId != null) {
            booking.setUser(userRepository.findById(newUserId).orElse(null));
        }
        booking.setCustomerName(newCustomerName);
        booking.setCustomerPhone(newCustomerPhone);

        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getUnpaidBookings(Long userId) {
        return bookingRepository
                .findByUserIdAndPaidAtIsNullAndPaymentDeadlineIsNotNullAndStatusNotOrderByBookingDateAsc(
                        userId, Booking.BookingStatus.CANCELLED)
                .stream().map(BookingDTO::fromEntity).collect(Collectors.toList());
    }
}
