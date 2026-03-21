package com.example.CauLongVui.service;

import com.example.CauLongVui.config.VNPayConfig;
import com.example.CauLongVui.entity.Booking;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.CauLongVui.service.BookingService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;
    private final BookingService bookingService;

    @Override
    public String createPaymentUrl(long amount, String orderInfo, String orderId, String ipAddress) {
        String vnp_Version = vnPayConfig.getVnp_Version();
        String vnp_Command = vnPayConfig.getVnp_Command();
        String vnp_TxnRef = orderId;
        String vnp_IpAddr = ipAddress;
        String vnp_TmnCode = vnPayConfig.getVnp_TmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");

        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
        
        return paymentUrl;
    }

    @Override
    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        
        String signValue = hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {
                return 1; // Success
            } else {
                return 0; // Failed
            }
        } else {
            return -1; // Invalid signature
        }
    }

    @Override
    public String ipnProcess(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        
        String signValue = hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            boolean checkOrderId = true; // TODO: Should check if OrderId exists in DB
            boolean checkAmount = true; // TODO: Should check if Amount matches DB
            boolean checkOrderStatus = true; // TODO: Should check if Order is not already paid

            if (checkOrderId) {
                if (checkAmount) {
                    if (checkOrderStatus) {
                        if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                            // Update status to Success
                            try {
                                String orderId = request.getParameter("vnp_TxnRef");
                                if (orderId != null && orderId.startsWith("CLV-")) {
                                    String[] parts = orderId.split("-");
                                    if (parts.length >= 2) {
                                        Long bookingId = Long.parseLong(parts[1]);
                                        bookingService.updateBookingStatus(bookingId, Booking.BookingStatus.CONFIRMED);
                                    }
                                }
                                return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
                            } catch (Exception e) {
                                log.error("Error updating booking status", e);
                                return "{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}";
                            }
                        } else {
                            // Update status to Failed
                             try {
                                String orderId = request.getParameter("vnp_TxnRef");
                                if (orderId != null && orderId.startsWith("CLV-")) {
                                    String[] parts = orderId.split("-");
                                    if (parts.length >= 2) {
                                        Long bookingId = Long.parseLong(parts[1]);
                                        bookingService.updateBookingStatus(bookingId, Booking.BookingStatus.CANCELLED);
                                    }
                                }
                                return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
                            } catch (Exception e) {
                                log.error("Error updating booking status", e);
                                return "{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}";
                            }
                        }
                    } else {
                        return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
                    }
                } else {
                    return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
                }
            } else {
                return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
            }
        } else {
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }
    }
    
    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
    }
}
