package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.*;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.entity.Wallet;
import com.example.CauLongVui.entity.WalletTransaction;
import com.example.CauLongVui.entity.WithdrawalRequest;
import com.example.CauLongVui.exception.BadRequestException;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final MomoService momoService;

    @Transactional
    public Wallet createWalletForUser(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .user(user)
                        .balance(ZERO)
                        .build()));
    }

    @Transactional
    public WalletDTO getWalletByUserId(Long userId) {
        return WalletDTO.fromEntity(getOrCreateWallet(userId));
    }

    @Transactional
    public BigDecimal getBalanceForUser(Long userId) {
        return getOrCreateWallet(userId).getBalance();
    }

    @Transactional
    public PaginationResponse<WalletTransactionDTO> getTransactions(Long userId, int page, int size) {
        Wallet wallet = getOrCreateWallet(userId);

        Page<WalletTransaction> transactionPage = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(
                wallet.getId(),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1))
        );

        return PaginationResponse.<WalletTransactionDTO>builder()
                .items(transactionPage.getContent().stream()
                        .map(WalletTransactionDTO::fromEntity)
                        .collect(Collectors.toList()))
                .pagination(PaginationResponse.Pagination.builder()
                        .page(transactionPage.getNumber())
                        .limit(transactionPage.getSize())
                        .total(transactionPage.getTotalElements())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequestDTO> getAllWithdrawalRequests() {
        return withdrawalRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(WithdrawalRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequestDTO> getWithdrawalRequestsByUserId(Long userId) {
        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WithdrawalRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, String> initiateTopUp(TopUpRequest request) throws Exception {
        if (request.getUserId() == null) {
            throw new BadRequestException("Thieu userId de nap tien");
        }

        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "So tien nap");
        validateIntegerAmount(amount, "So tien nap phai la so nguyen VND");

        Wallet wallet = getOrCreateWallet(request.getUserId());
        String orderId = "TOPUP_" + request.getUserId() + "_" + System.currentTimeMillis();
        WalletTransaction transaction = walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .status(WalletTransaction.TransactionStatus.PENDING)
                .type(WalletTransaction.TransactionType.TOPUP)
                .referenceId(orderId)
                .description("Nap tien vao vi Cau Long Vui")
                .build());

        try {
            MomoPaymentResponse response = momoService.createPayment(
                    amount.longValue(),
                    orderId,
                    "Nap tien vao vi Cau Long Vui"
            );

            if (response.getResultCode() == null || response.getResultCode() != 0 || response.getPayUrl() == null) {
                transaction.setStatus(WalletTransaction.TransactionStatus.FAILED);
                transaction.setDescription("Tao giao dich MoMo that bai: " + response.getMessage());
                walletTransactionRepository.save(transaction);
                throw new BadRequestException("Khong the tao link nap tien MoMo");
            }

            return Map.of(
                    "orderId", orderId,
                    "payUrl", response.getPayUrl()
            );
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            transaction.setStatus(WalletTransaction.TransactionStatus.FAILED);
            transaction.setDescription("Loi MoMo: " + ex.getMessage());
            walletTransactionRepository.save(transaction);
            throw ex;
        }
    }

    @Transactional
    public WalletTransactionDTO completeTopUp(String orderId, String externalTransactionId) {
        WalletTransaction transaction = walletTransactionRepository
                .findFirstByReferenceIdAndTypeOrderByCreatedAtDesc(orderId, WalletTransaction.TransactionType.TOPUP)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay giao dich nap tien"));

        if (transaction.getStatus() == WalletTransaction.TransactionStatus.COMPLETED) {
            return WalletTransactionDTO.fromEntity(transaction);
        }

        Wallet wallet = walletRepository.findByIdForUpdate(transaction.getWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay vi"));

        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        transaction.setStatus(WalletTransaction.TransactionStatus.COMPLETED);
        if (externalTransactionId != null && !externalTransactionId.isBlank()) {
            transaction.setDescription("Nap tien thanh cong. Ma MoMo: " + externalTransactionId);
        }

        walletRepository.save(wallet);
        walletTransactionRepository.save(transaction);
        return WalletTransactionDTO.fromEntity(transaction);
    }

    @Transactional
    public void failTopUp(String orderId, String reason) {
        walletTransactionRepository
                .findFirstByReferenceIdAndTypeOrderByCreatedAtDesc(orderId, WalletTransaction.TransactionType.TOPUP)
                .ifPresent(transaction -> {
                    if (transaction.getStatus() == WalletTransaction.TransactionStatus.PENDING) {
                        transaction.setStatus(WalletTransaction.TransactionStatus.FAILED);
                        if (reason != null && !reason.isBlank()) {
                            transaction.setDescription(reason);
                        }
                        walletTransactionRepository.save(transaction);
                    }
                });
    }

    @Transactional
    public void payBookingWithWallet(Long userId, BigDecimal amount, String referenceId, String description) {
        BigDecimal normalizedAmount = normalizePositiveAmount(amount, "So tien thanh toan");
        Wallet wallet = getOrCreateWalletForUpdate(userId);

        if (wallet.getBalance().compareTo(normalizedAmount) < 0) {
            throw new BadRequestException("So du vi khong du de thanh toan. Can " + normalizedAmount + " VND nhung chi co " + wallet.getBalance().longValue() + " VND.");
        }

        wallet.setBalance(wallet.getBalance().subtract(normalizedAmount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(normalizedAmount.negate())
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .type(WalletTransaction.TransactionType.PAYMENT)
                .referenceId(referenceId)
                .description(description)
                .build());
    }

    @Transactional
    public BookingDTO payBookingWithWallet(Long bookingId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("Thieu userId de thanh toan bang vi");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nguoi dung"));

        if (booking.getPaidAt() != null) {
            throw new BadRequestException("Booking nay da duoc thanh toan");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking da bi huy, khong the thanh toan");
        }
        if (booking.getUser() != null && !booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Booking khong thuoc ve nguoi dung nay");
        }

        BigDecimal amount = normalizePositiveAmount(toBigDecimal(booking.getTotalPrice()), "So tien thanh toan");
        Wallet wallet = getOrCreateWalletForUpdate(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("So du vi khong du de thanh toan");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .type(WalletTransaction.TransactionType.PAYMENT)
                .referenceId("BOOKING-" + booking.getId())
                .description("Thanh toan booking #" + booking.getId() + " bang vi")
                .build());

        booking.setUser(user);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentMethod(Booking.PaymentMethod.WALLET);
        booking.setPaymentReference(transaction.getReferenceId());
        booking.setPaidAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return BookingDTO.fromEntity(booking);
    }

    @Transactional
    public WalletTransactionDTO refundToWallet(Long userId, BigDecimal amount, String referenceId, String description) {
        BigDecimal normalizedAmount = normalizePositiveAmount(amount, "So tien hoan");
        Wallet wallet = getOrCreateWalletForUpdate(userId);
        wallet.setBalance(wallet.getBalance().add(normalizedAmount));
        walletRepository.save(wallet);

        WalletTransaction transaction = walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(normalizedAmount)
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .type(WalletTransaction.TransactionType.REFUND)
                .referenceId(referenceId)
                .description(description)
                .build());

        return WalletTransactionDTO.fromEntity(transaction);
    }

    @Transactional
    public void deductForMembership(Long userId, long amountVnd, String referenceId, String description) {
        if (amountVnd <= 0) return; // Nếu giá hiệu quả = 0 (hoàn tiền đủ hoặc dư), không cần trừ
        BigDecimal amount = BigDecimal.valueOf(amountVnd).setScale(2, java.math.RoundingMode.HALF_UP);
        Wallet wallet = getOrCreateWalletForUpdate(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new com.example.CauLongVui.exception.BadRequestException("So du vi khong du. Can " + amountVnd + " VND nhung chi co " + wallet.getBalance().longValue() + " VND.");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .type(WalletTransaction.TransactionType.PAYMENT)
                .referenceId(referenceId)
                .description(description)
                .build());
    }

    @Transactional
    public WithdrawalRequestDTO createWithdrawal(CreateWithdrawalRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("Thieu userId de rut tien");
        }
        if (isBlank(request.getBankName()) || isBlank(request.getAccountNumber()) || isBlank(request.getAccountHolder())) {
            throw new BadRequestException("Thong tin tai khoan ngan hang khong duoc de trong");
        }

        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "So tien rut");
        Wallet wallet = getOrCreateWalletForUpdate(request.getUserId());
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("So du vi khong du de tao yeu cau rut tien");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WithdrawalRequest withdrawalRequest = withdrawalRequestRepository.save(WithdrawalRequest.builder()
                .user(wallet.getUser())
                .amount(amount)
                .bankName(request.getBankName().trim())
                .accountNumber(request.getAccountNumber().trim())
                .accountHolder(request.getAccountHolder().trim())
                .status(WithdrawalRequest.WithdrawalStatus.PENDING)
                .build());

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .status(WalletTransaction.TransactionStatus.PENDING)
                .type(WalletTransaction.TransactionType.WITHDRAWAL)
                .referenceId("WD-" + withdrawalRequest.getId())
                .description("Yeu cau rut tien #" + withdrawalRequest.getId())
                .build());

        return WithdrawalRequestDTO.fromEntity(withdrawalRequest);
    }

    @Transactional
    public WithdrawalRequestDTO reviewWithdrawal(Long requestId, WithdrawalRequest.WithdrawalStatus status) {
        if (status == null || status == WithdrawalRequest.WithdrawalStatus.PENDING) {
            throw new BadRequestException("Trang thai duyet khong hop le");
        }

        WithdrawalRequest withdrawalRequest = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau rut tien"));

        if (withdrawalRequest.getStatus() != WithdrawalRequest.WithdrawalStatus.PENDING) {
            throw new BadRequestException("Yeu cau rut tien da duoc xu ly");
        }

        WalletTransaction transaction = walletTransactionRepository
                .findFirstByReferenceIdAndTypeOrderByCreatedAtDesc(
                        "WD-" + withdrawalRequest.getId(),
                        WalletTransaction.TransactionType.WITHDRAWAL
                )
                .orElse(null);

        withdrawalRequest.setStatus(status);
        if (status == WithdrawalRequest.WithdrawalStatus.APPROVED) {
            if (transaction != null) {
                transaction.setStatus(WalletTransaction.TransactionStatus.COMPLETED);
                walletTransactionRepository.save(transaction);
            }
        } else {
            if (transaction != null) {
                transaction.setStatus(WalletTransaction.TransactionStatus.FAILED);
                walletTransactionRepository.save(transaction);
            }
            refundToWallet(
                    withdrawalRequest.getUser().getId(),
                    withdrawalRequest.getAmount(),
                    "WD-" + withdrawalRequest.getId(),
                    "Hoan tra yeu cau rut tien #" + withdrawalRequest.getId()
            );
        }

        withdrawalRequestRepository.save(withdrawalRequest);
        return WithdrawalRequestDTO.fromEntity(withdrawalRequest);
    }

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> createWalletForUser(getUser(userId)));
    }

    private Wallet getOrCreateWalletForUpdate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    createWalletForUser(getUser(userId));
                    return walletRepository.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Khong the tao vi cho nguoi dung"));
                });
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nguoi dung"));
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new BadRequestException(fieldName + " khong duoc de trong");
        }

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) <= 0) {
            throw new BadRequestException(fieldName + " phai lon hon 0");
        }
        return normalized;
    }

    private void validateIntegerAmount(BigDecimal amount, String message) {
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException(message);
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return ZERO;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
