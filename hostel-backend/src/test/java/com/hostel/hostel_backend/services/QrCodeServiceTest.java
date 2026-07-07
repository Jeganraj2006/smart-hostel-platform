package com.hostel.hostel_backend.services;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class QrCodeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private QrCodeService qrCodeService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testGenerateAndDecodeQrCode() throws Exception {
        String leaveId = "test-leave-123";
        String toDateStr = LocalDateTime.now().plusDays(2).toString();

        // Generate QR code base64 string
        String base64Image = qrCodeService.generateQrBase64(leaveId, toDateStr);
        assertNotNull(base64Image);
        assertFalse(base64Image.isEmpty());

        // Verify Redis interaction
        verify(valueOperations, times(1)).set(eq("leave:nonce:" + leaveId), anyString(), anyLong(), any());

        // Decode Base64 string back to byte array
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // Convert byte array back to BufferedImage
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(bufferedImage);

        // Decode QR Code using ZXing
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result decodedResult = new MultiFormatReader().decode(bitmap);

        assertNotNull(decodedResult);
        String decodedText = decodedResult.getText();
        assertNotNull(decodedText);
        assertTrue(decodedText.startsWith(leaveId + ":"));
    }
}
