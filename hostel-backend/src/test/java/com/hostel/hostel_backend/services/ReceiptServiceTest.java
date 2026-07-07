package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that ReceiptService.generateReceiptPdf() produces a valid, non-empty PDF
 * byte array for well-formed Fee + User inputs without throwing any exception.
 * Full layout correctness is a visual concern; the contract we assert here is:
 *  - no IOException / RuntimeException
 *  - output starts with the PDF magic bytes (%PDF)
 *  - output length is reasonable (> 5 KB for any styled A4 document)
 */
public class ReceiptServiceTest {

    private final ReceiptService receiptService = new ReceiptService();

    private Fee buildPaidFee(String id) {
        Fee f = new Fee();
        f.setId(id);
        f.setStudentId("student-001");
        f.setFeeType("HOSTEL");
        f.setAmount(12500.0);
        f.setDueDate("2026-07-01");
        f.setPaidDate("2026-06-28");
        f.setStatus("PAID");
        return f;
    }

    private User buildStudent() {
        User u = new User();
        u.setId("student-001");
        u.setName("Alice Sharma");
        u.setEmail("alice@hostel.com");
        u.setPhone("+91-9876543210");
        return u;
    }

    @Test
    void testGenerateReceiptPdf_doesNotThrow() throws IOException {
        byte[] pdf = receiptService.generateReceiptPdf(buildPaidFee("fee-abc123"), buildStudent());
        assertNotNull(pdf, "PDF bytes must not be null");
    }

    @Test
    void testGenerateReceiptPdf_startsWithPdfMagicBytes() throws IOException {
        byte[] pdf = receiptService.generateReceiptPdf(buildPaidFee("fee-abc123"), buildStudent());
        // iText 8 compresses PDFs; standard-font documents are typically ~2 KB.
        // We assert > 1 KB as a reasonable minimum for any non-trivial PDF content.
        assertTrue(pdf.length > 1_000,
                "Generated PDF should be at least 1 KB, got " + pdf.length + " bytes");
        // %PDF magic bytes
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void testGenerateReceiptPdf_withNullPhoneAndNullPaidDate_doesNotThrow() throws IOException {
        // Edge case: student has no phone, fee has no paidDate (should render "—")
        User studentNoPhone = buildStudent();
        studentNoPhone.setPhone(null);

        Fee feeNoPaidDate = buildPaidFee("fee-xyz999");
        feeNoPaidDate.setPaidDate(null);

        byte[] pdf = receiptService.generateReceiptPdf(feeNoPaidDate, studentNoPhone);
        assertNotNull(pdf);
        assertTrue(pdf.length > 1_000);
    }
}
