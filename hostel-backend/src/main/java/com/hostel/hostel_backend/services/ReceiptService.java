package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates a PDF payment receipt for a hostel fee using iText 8.
 *
 * The PDF contains:
 *  - Hostel branding header
 *  - Receipt number (= fee ID)
 *  - Student name & ID
 *  - Fee type, amount, due date, paid date
 *  - Auto-generated print timestamp
 */
@Service
public class ReceiptService {

    private static final String HOSTEL_NAME = "Smart Hostel Management System";
    private static final String HOSTEL_SUBTITLE = "Official Fee Payment Receipt";

    // Brand colour: deep teal #1A6B72
    private static final DeviceRgb BRAND_COLOR  = new DeviceRgb(26,  107, 114);
    private static final DeviceRgb HEADER_BG    = new DeviceRgb(26,  107, 114);
    private static final DeviceRgb ROW_ALT_BG   = new DeviceRgb(240, 248, 249);
    private static final DeviceRgb LABEL_COLOR  = new DeviceRgb(80,  80,  80);
    private static final DeviceRgb VALUE_COLOR  = new DeviceRgb(20,  20,  20);

    public byte[] generateReceiptPdf(Fee fee, User student) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter  writer  = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document   doc     = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── Header banner ─────────────────────────────────────────────────────
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();

        Cell headerCell = new Cell()
                .setBackgroundColor(HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setPadding(20)
                .setTextAlignment(TextAlignment.CENTER);

        headerCell.add(new Paragraph(HOSTEL_NAME)
                .setFont(bold)
                .setFontSize(20)
                .setFontColor(ColorConstants.WHITE)
                .setMarginBottom(4));

        headerCell.add(new Paragraph(HOSTEL_SUBTITLE)
                .setFont(regular)
                .setFontSize(11)
                .setFontColor(new DeviceRgb(200, 235, 238))
                .setMarginBottom(0));

        headerTable.addCell(headerCell);
        doc.add(headerTable);
        doc.add(new Paragraph(" "));   // spacer

        // ── Receipt number & date ─────────────────────────────────────────────
        String printedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        metaTable.addCell(labelCell("Receipt No.", regular)
                .setTextAlignment(TextAlignment.LEFT));
        metaTable.addCell(labelCell("Printed On", regular)
                .setTextAlignment(TextAlignment.RIGHT));

        metaTable.addCell(valueCell("REC-" + fee.getId().toUpperCase(), bold)
                .setTextAlignment(TextAlignment.LEFT));
        metaTable.addCell(valueCell(printedAt, bold)
                .setTextAlignment(TextAlignment.RIGHT));

        doc.add(metaTable);

        // ── Divider ───────────────────────────────────────────────────────────
        doc.add(new Paragraph()
                .setBorderBottom(new SolidBorder(BRAND_COLOR, 1.5f))
                .setMarginBottom(16));

        // ── Student details section ────────────────────────────────────────────
        doc.add(new Paragraph("Student Details")
                .setFont(bold)
                .setFontSize(12)
                .setFontColor(BRAND_COLOR)
                .setMarginBottom(6));

        Table studentTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(16);

        addDetailRow(studentTable, "Student Name",  student.getName(),  regular, bold, false);
        addDetailRow(studentTable, "Student ID",    student.getId(),    regular, bold, true);
        addDetailRow(studentTable, "Email",         student.getEmail(), regular, bold, false);
        addDetailRow(studentTable, "Phone",
                student.getPhone() != null ? student.getPhone() : "—", regular, bold, true);

        doc.add(studentTable);

        // ── Fee details section ────────────────────────────────────────────────
        doc.add(new Paragraph("Fee Details")
                .setFont(bold)
                .setFontSize(12)
                .setFontColor(BRAND_COLOR)
                .setMarginBottom(6));

        Table feeTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addDetailRow(feeTable, "Fee Type",  fee.getFeeType(),               regular, bold, false);
        addDetailRow(feeTable, "Due Date",  fee.getDueDate(),               regular, bold, true);
        addDetailRow(feeTable, "Paid Date", fee.getPaidDate(),              regular, bold, false);
        addDetailRow(feeTable, "Status",    fee.getStatus(),                regular, bold, true);

        doc.add(feeTable);

        // ── Amount box ────────────────────────────────────────────────────────
        Table amountTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth()
                .setMarginBottom(24);

        Cell amountCell = new Cell()
                .setBackgroundColor(HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setPadding(16)
                .setTextAlignment(TextAlignment.CENTER);

        amountCell.add(new Paragraph("Amount Paid")
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(new DeviceRgb(200, 235, 238))
                .setMarginBottom(4));

        amountCell.add(new Paragraph("₹ " + String.format("%.2f", fee.getAmount()))
                .setFont(bold)
                .setFontSize(26)
                .setFontColor(ColorConstants.WHITE)
                .setMarginBottom(0));

        amountTable.addCell(amountCell);
        doc.add(amountTable);

        // ── Footer note ───────────────────────────────────────────────────────
        doc.add(new Paragraph(
                "This is a system-generated receipt and does not require a physical signature. " +
                "For disputes, contact the hostel administration with receipt number REC-" +
                fee.getId().toUpperCase() + ".")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(LABEL_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(new SolidBorder(new DeviceRgb(220, 220, 220), 0.5f))
                .setPaddingTop(10));

        doc.close();
        return baos.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addDetailRow(Table table, String label, String value,
                              PdfFont regular, PdfFont bold, boolean altRow) {
        DeviceRgb bg = altRow ? ROW_ALT_BG : new DeviceRgb(255, 255, 255);

        table.addCell(new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(230, 230, 230), 0.5f))
                .setPadding(8)
                .add(new Paragraph(label)
                        .setFont(regular)
                        .setFontSize(9)
                        .setFontColor(LABEL_COLOR)));

        table.addCell(new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(230, 230, 230), 0.5f))
                .setPadding(8)
                .add(new Paragraph(value != null ? value : "—")
                        .setFont(bold)
                        .setFontSize(10)
                        .setFontColor(VALUE_COLOR)));
    }

    private Cell labelCell(String text, PdfFont font) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(LABEL_COLOR));
    }

    private Cell valueCell(String text, PdfFont font) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(11)
                        .setFontColor(VALUE_COLOR));
    }
}
