package com.hostel.hostel_backend.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class QrCodeService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Encodes the leaveId (plus a random nonce stored in Redis with a TTL matching the
     * leave's toDate) into a QR code image, returned as a base64 PNG string.
     */
    public String generateQrBase64(String leaveId, String toDateStr) {
        try {
            String nonce = UUID.randomUUID().toString();
            String redisKey = "leave:nonce:" + leaveId;
            long ttl = calculateTtlInSeconds(toDateStr);

            redisTemplate.opsForValue().set(redisKey, nonce, ttl, TimeUnit.SECONDS);

            String qrContent = leaveId + ":" + nonce;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR Code", e);
        }
    }

    private long calculateTtlInSeconds(String toDateStr) {
        if (toDateStr == null || toDateStr.trim().isEmpty()) {
            return 7 * 24 * 3600; // default 7 days
        }
        try {
            LocalDateTime toDate;
            if (toDateStr.contains("T")) {
                toDate = LocalDateTime.parse(toDateStr);
            } else {
                toDate = java.time.LocalDate.parse(toDateStr).atTime(23, 59, 59);
            }
            long duration = Duration.between(LocalDateTime.now(), toDate).toSeconds();
            return duration > 0 ? duration : 3600; // minimum 1 hour if target is in the past
        } catch (Exception e) {
            return 7 * 24 * 3600; // default 7 days
        }
    }
}
