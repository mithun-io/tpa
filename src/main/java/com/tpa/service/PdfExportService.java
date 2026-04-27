package com.tpa.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.tpa.entity.Claim;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ClaimRepository claimRepository;

    private static final Color PRIMARY_COLOR  = new Color(0, 86, 179);   // #0056b3
    private static final Color SUCCESS_COLOR  = new Color(40, 167, 69);  // #28a745
    private static final Color DANGER_COLOR   = new Color(220, 53, 69);  // #dc3545
    private static final Color WARNING_COLOR  = new Color(255, 193, 7);  // #ffc107
    private static final Color LIGHT_GRAY     = new Color(248, 249, 250);
    private static final Color BORDER_COLOR   = new Color(222, 226, 230);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] exportClaimReport(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("Claim not found: " + claimId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 60, 60);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Header/Footer via page events
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    PdfContentByte cb = w.getDirectContent();
                    // Footer line
                    cb.setColorStroke(BORDER_COLOR);
                    cb.moveTo(d.leftMargin(), d.bottomMargin() - 10);
                    cb.lineTo(d.right(), d.bottomMargin() - 10);
                    cb.stroke();
                    // Footer text
                    Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
                    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                            new Phrase("TPA Insurance Claim Processing System — Confidential | Claim Report #" + claimId, footerFont),
                            d.getPageSize().getWidth() / 2, d.bottomMargin() - 20, 0);
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                            new Phrase("Page " + w.getPageNumber(), footerFont),
                            d.right(), d.bottomMargin() - 20, 0);
                }
            });

            document.open();

            // ── HEADER BANNER ──────────────────────────────────────────
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{2f, 1f});

            Font titleFont   = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(180, 210, 255));

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBackgroundColor(PRIMARY_COLOR);
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(16);
            leftCell.addElement(new Phrase("TPA CLAIM DECISION REPORT", titleFont));
            leftCell.addElement(new Phrase("Insurance Claim Processing System", subtitleFont));

            String statusLabel = claim.getStatus().name();
            Color statusColor = switch (claim.getStatus()) {
                case ADMIN_APPROVED, CARRIER_APPROVED, SETTLED -> SUCCESS_COLOR;
                case REJECTED -> DANGER_COLOR;
                case UNDER_REVIEW -> WARNING_COLOR;
                default -> Color.GRAY;
            };

            Font statusFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
            Font statusSmall = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE);
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBackgroundColor(statusColor);
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPadding(16);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            rightCell.addElement(new Paragraph(statusLabel, statusFont));
            rightCell.addElement(new Paragraph("Claim Status", statusSmall));

            headerTable.addCell(leftCell);
            headerTable.addCell(rightCell);
            document.add(headerTable);
            document.add(Chunk.NEWLINE);

            // ── CLAIM SUMMARY TABLE ─────────────────────────────────────
            addSectionTitle(document, "Claim Summary");
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{1f, 2f});
            summaryTable.setSpacingAfter(14);

            addRow(summaryTable, "Claim ID",        "#" + claim.getId());
            addRow(summaryTable, "Policy Number",   claim.getPolicyNumber());
            addRow(summaryTable, "Policy ID",       claim.getPolicyId() != null ? claim.getPolicyId() : "—");
            addRow(summaryTable, "Policy Name",     claim.getPolicyName() != null ? claim.getPolicyName() : "—");
            addRow(summaryTable, "Carrier Name",    claim.getCarrierName() != null ? claim.getCarrierName() : "—");
            addRow(summaryTable, "Claim Type",      claim.getClaimType() != null ? claim.getClaimType() : "—");
            
            // Format amounts
            String claimedAmt = String.format("$ %,.2f", claim.getAmount() != null ? claim.getAmount() : 0);
            String totalBillAmt = claim.getTotalBillAmount() != null ? String.format("$ %,.2f", claim.getTotalBillAmount()) : "-";
            
            addRow(summaryTable, "Claimed Amount",  claimedAmt);
            addRow(summaryTable, "Total Bill Amount", totalBillAmt);
            addRow(summaryTable, "Current Status",  claim.getStatus().name());
            addRow(summaryTable, "Created Date",    claim.getCreatedDate() != null ? claim.getCreatedDate().format(DATE_FMT) : "—");
            addRow(summaryTable, "Processed Date",  claim.getProcessedDate() != null ? claim.getProcessedDate().format(DATE_FMT) : "Pending");
            document.add(summaryTable);

            // ── PATIENT & HOSPITAL DETAILS ─────────────────────────────
            addSectionTitle(document, "Medical Details");
            PdfPTable medTable = new PdfPTable(2);
            medTable.setWidthPercentage(100);
            medTable.setWidths(new float[]{1f, 2f});
            medTable.setSpacingAfter(14);
            
            addRow(medTable, "Patient Name",    claim.getPatientName() != null ? claim.getPatientName() : "—");
            addRow(medTable, "Hospital Name",   claim.getHospitalName() != null ? claim.getHospitalName() : "—");
            
            DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String admission = claim.getAdmissionDate() != null ? claim.getAdmissionDate().format(DATE_ONLY) : "—";
            String discharge = claim.getDischargeDate() != null ? claim.getDischargeDate().format(DATE_ONLY) : "—";
            
            addRow(medTable, "Admission Date",  admission);
            addRow(medTable, "Discharge Date",  discharge);
            addRow(medTable, "Diagnosis",       claim.getDiagnosis() != null ? claim.getDiagnosis() : "—");
            document.add(medTable);

            // ── BILLING DETAILS ─────────────────────────────────────────
            addSectionTitle(document, "Billing Details");
            PdfPTable billTable = new PdfPTable(2);
            billTable.setWidthPercentage(100);
            billTable.setWidths(new float[]{1f, 2f});
            billTable.setSpacingAfter(14);
            
            addRow(billTable, "Bill Number", claim.getBillNumber() != null ? claim.getBillNumber() : "—");
            String bDate = claim.getBillDate() != null ? claim.getBillDate().format(DATE_ONLY) : "—";
            addRow(billTable, "Bill Date",   bDate);
            addRow(billTable, "Total Bill",  totalBillAmt);
            addRow(billTable, "Claimed Amt", claimedAmt);
            document.add(billTable);


            // ── POLICYHOLDER DETAILS ────────────────────────────────────
            if (claim.getUser() != null) {
                addSectionTitle(document, "Policyholder Details");
                PdfPTable userTable = new PdfPTable(2);
                userTable.setWidthPercentage(100);
                userTable.setWidths(new float[]{1f, 2f});
                userTable.setSpacingAfter(14);

                addRow(userTable, "Name",   claim.getUser().getUsername());
                addRow(userTable, "Email",  claim.getUser().getEmail());
                addRow(userTable, "Mobile", claim.getUser().getMobile() != null ? claim.getUser().getMobile() : "—");
                document.add(userTable);
            }

            // ── RULE ENGINE DECISION ────────────────────────────────────
            addSectionTitle(document, "Rule Engine Decision");

            String reasons = claim.getRejectionReason();
            if (reasons != null && !reasons.isBlank()) {
                Font reasonFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(80, 80, 80));
                String[] items = reasons.split(",\\s*");
                for (int i = 0; i < items.length; i++) {
                    PdfPTable rowTable = new PdfPTable(2);
                    rowTable.setWidthPercentage(100);
                    rowTable.setWidths(new float[]{0.08f, 1f});
                    rowTable.setSpacingAfter(3);

                    PdfPCell bullet = new PdfPCell(new Phrase("•", reasonFont));
                    bullet.setBorder(Rectangle.NO_BORDER);
                    bullet.setPaddingLeft(10);

                    PdfPCell text = new PdfPCell(new Phrase(items[i].trim(), reasonFont));
                    text.setBorder(Rectangle.NO_BORDER);
                    text.setBackgroundColor(i % 2 == 0 ? LIGHT_GRAY : Color.WHITE);
                    text.setPadding(6);

                    rowTable.addCell(bullet);
                    rowTable.addCell(text);
                    document.add(rowTable);
                }
            } else {
                Font greenFont = new Font(Font.HELVETICA, 10, Font.BOLD, SUCCESS_COLOR);
                document.add(new Paragraph("✓ No rule violations detected. Claim passes all automated checks.", greenFont));
            }

            document.add(Chunk.NEWLINE);

            // ── DECLARATION BOX ─────────────────────────────────────────
            PdfPTable declaration = new PdfPTable(1);
            declaration.setWidthPercentage(100);
            declaration.setSpacingBefore(10);
            Font declFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY);
            PdfPCell declCell = new PdfPCell(new Phrase(
                    "This report is a system-generated document from the TPA Insurance Claim Processing System. " +
                    "It reflects the automated rule engine evaluation and/or manual review decision. " +
                    "This document is confidential and intended solely for authorized personnel.", declFont));
            declCell.setBackgroundColor(LIGHT_GRAY);
            declCell.setBorderColor(BORDER_COLOR);
            declCell.setPadding(10);
            declaration.addCell(declCell);
            document.add(declaration);

            document.close();
            log.info("PDF report generated for claim {}", claimId);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF for claim {}: {}", claimId, e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void addSectionTitle(Document doc, String title) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR);
        Paragraph p = new Paragraph(title, sectionFont);
        p.setSpacingBefore(10);
        p.setSpacingAfter(6);
        doc.add(p);

        // Underline via a 1px colored line
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingAfter(8);
        PdfPCell lineCell = new PdfPCell(new Phrase(""));
        lineCell.setFixedHeight(1f);
        lineCell.setBackgroundColor(PRIMARY_COLOR);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        doc.add(line);
    }

    private void addRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(80, 80, 80));
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(LIGHT_GRAY);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(7);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", valueFont));
        valueCell.setBorderColor(BORDER_COLOR);
        valueCell.setPadding(7);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
