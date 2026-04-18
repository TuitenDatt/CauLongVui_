package com.example.CauLongVui.service;

import com.example.CauLongVui.config.VNPayConfig;
import com.example.CauLongVui.entity.MembershipPlan;
import com.example.CauLongVui.entity.MembershipSubscription;
import com.example.CauLongVui.entity.MembershipTier;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.repository.MembershipPlanRepository;
import com.example.CauLongVui.repository.MembershipSubscriptionRepository;
import com.example.CauLongVui.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final MembershipPlanRepository planRepository;
    private final MembershipSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final VNPayService vnPayService;
    private final MomoService momoService;
    private final WalletService walletService;

    public List<MembershipPlan> getAllPlans() {
        return planRepository.findAll();
    }

    /**
     * Tính số tiền thực tế user phải trả khi mua gói mới,
     * sau khi đã khấu trừ giá trị còn lại của gói hiện tại.
     */
    public long calculateEffectivePrice(User user, MembershipPlan newPlan) {
        if (user.getMembershipTier() == MembershipTier.NORMAL
                || user.getMembershipExpiry() == null
                || !user.getMembershipExpiry().isAfter(LocalDateTime.now())) {
            return newPlan.getPrice();
        }

        // Tìm gói hiện tại dựa vào tier
        MembershipPlan currentPlan = planRepository.findByTier(user.getMembershipTier()).orElse(null);
        if (currentPlan == null) return newPlan.getPrice();

        // Số giây còn lại
        LocalDateTime now = LocalDateTime.now();
        long totalSeconds = (long) currentPlan.getDurationInDays() * 86400L;
        long remainingSeconds = java.time.Duration.between(now, user.getMembershipExpiry()).getSeconds();
        if (remainingSeconds <= 0) return newPlan.getPrice();

        // Giá trị hoàn = giá gói cũ × (giây còn lại / tổng giây gói cũ)
        long refund = (long) (currentPlan.getPrice() * ((double) remainingSeconds / totalSeconds));
        long effectivePrice = newPlan.getPrice() - refund;
        return Math.max(effectivePrice, 0);
    }

    @Transactional
    public String initiatePurchase(Long planId, Long userId, String method, HttpServletRequest request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        // --- Ràng buộc: không cho hạ cấp ---
        int currentOrdinal = user.getMembershipTier().ordinal(); // NORMAL=0, PRO=1, VIP=2
        int newOrdinal = plan.getTier().ordinal();
        if (newOrdinal < currentOrdinal) {
            throw new IllegalArgumentException("Không thể mua gói thấp hơn gói hiện tại của bạn.");
        }
        if (newOrdinal == currentOrdinal
                && user.getMembershipExpiry() != null
                && user.getMembershipExpiry().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Bạn đang sử dụng gói này rồi. Vui lòng chờ hết hạn.");
        }

        // --- Tính giá thực tế (có thể được khấu trừ khi nâng cấp) ---
        long effectivePrice = calculateEffectivePrice(user, plan);

        // Tạo subscription đang chờ xử lý
        MembershipSubscription subscription = MembershipSubscription.builder()
                .user(user)
                .plan(plan)
                .purchaseDate(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusDays(plan.getDurationInDays()))
                .status(MembershipSubscription.SubscriptionStatus.PENDING)
                .build();
        subscription = subscriptionRepository.save(subscription);

        String orderId = "MB-" + subscription.getId();
        String orderInfo = "Membership plan " + plan.getName() + " for user " + user.getEmail();

        if ("wallet".equalsIgnoreCase(method)) {
            // --- Thanh toán bằng ví nội bộ: trừ tiền và kích hoạt ngay ---
            walletService.deductForMembership(userId, effectivePrice, orderId,
                    "Mua goi " + plan.getName());
            completePurchase(subscription.getId());
            return "WALLET_SUCCESS";
        } else if ("momo".equalsIgnoreCase(method)) {
            var momoRes = momoService.createPayment(effectivePrice, orderId, orderInfo);
            String paymentUrl = momoRes.getPayUrl();
            if (paymentUrl == null) {
                throw new Exception("MoMo payment failed: " + momoRes.getMessage() + " - Code: " + momoRes.getResultCode());
            }
            subscription.setVnpayTxnRef(orderId);
            subscriptionRepository.save(subscription);
            return paymentUrl;
        } else {
            String ipAddress = VNPayConfig.getIpAddress(request);
            String paymentUrl = vnPayService.createPaymentUrl(effectivePrice, orderInfo, orderId, ipAddress);
            subscription.setVnpayTxnRef(orderId);
            subscriptionRepository.save(subscription);
            return paymentUrl;
        }
    }

    @Transactional
    public void completePurchase(Long subscriptionId) {
        MembershipSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (subscription.getStatus() == MembershipSubscription.SubscriptionStatus.COMPLETED) {
            return;
        }

        subscription.setStatus(MembershipSubscription.SubscriptionStatus.COMPLETED);
        subscriptionRepository.save(subscription);

        User user = subscription.getUser();
        user.setMembershipTier(subscription.getPlan().getTier());

        LocalDateTime newExpiry;
        if (user.getMembershipExpiry() != null && user.getMembershipExpiry().isAfter(LocalDateTime.now())) {
            newExpiry = user.getMembershipExpiry().plusDays(subscription.getPlan().getDurationInDays());
        } else {
            newExpiry = LocalDateTime.now().plusDays(subscription.getPlan().getDurationInDays());
        }
        user.setMembershipExpiry(newExpiry);
        userRepository.save(user);

        log.info("Membership updated for user {}: Tier {}, Expiry {}", user.getEmail(), user.getMembershipTier(), user.getMembershipExpiry());
    }

    @Transactional
    public void cancelPurchase(Long subscriptionId) {
        MembershipSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        subscription.setStatus(MembershipSubscription.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }
}
