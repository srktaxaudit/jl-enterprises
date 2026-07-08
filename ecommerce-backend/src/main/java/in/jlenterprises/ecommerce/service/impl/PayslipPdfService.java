package in.jlenterprises.ecommerce.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import in.jlenterprises.ecommerce.dto.hr.PayLine;
import in.jlenterprises.ecommerce.entity.AppSetting;
import in.jlenterprises.ecommerce.entity.hr.Employee;
import in.jlenterprises.ecommerce.entity.hr.Payslip;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.repository.AppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

/** Renders a branded salary-slip PDF with OpenPDF. */
@Service
public class PayslipPdfService {

    private static final Logger log = LoggerFactory.getLogger(PayslipPdfService.class);
    private static final Color NAVY = new Color(11, 36, 71);
    private static final Color GREY = new Color(100, 116, 139);
    private static final String[] MONTHS = {"", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private final AppSettingRepository settings;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    public PayslipPdfService(AppSettingRepository settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    public byte[] build(Payslip p, Employee e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            String sellerName = setting("seller_name", "JL Enterprises");
            byte[] logo = fetch(setting("site_logo_url", null));

            // Header: logo | company + title
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 3f});
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(0);
            if (logo != null) {
                try {
                    Image img = Image.getInstance(logo);
                    img.scaleToFit(110, 55);
                    logoCell.addElement(img);
                } catch (Exception ignore) { /* text header only */ }
            }
            header.addCell(logoCell);
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(0);
            titleCell.addElement(right(new Paragraph(sellerName, font(14, Font.BOLD, NAVY))));
            titleCell.addElement(right(new Paragraph("SALARY SLIP", font(11, Font.BOLD, GREY))));
            titleCell.addElement(right(new Paragraph(MONTHS[p.getPeriodMonth()] + " " + p.getPeriodYear(), font(10, Font.NORMAL, GREY))));
            header.addCell(titleCell);
            doc.add(header);
            doc.add(spacer(12));

            // Employee block
            PdfPTable emp = new PdfPTable(4);
            emp.setWidthPercentage(100);
            emp.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
            kv(emp, "Employee", e.getFirstName() + " " + opt(e.getLastName()));
            kv(emp, "Code", e.getEmployeeCode());
            kv(emp, "Designation", opt(e.getDesignation()));
            kv(emp, "Department", opt(e.getDepartment()));
            kv(emp, "Employment", e.getEmploymentType().name().replace('_', ' '));
            kv(emp, "Paid days", str(p.getDaysPayable()));
            doc.add(emp);
            doc.add(spacer(12));

            // Earnings & deductions
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{3f, 1.4f});
            th(t, "Earnings & Deductions");
            th(t, "Amount");
            for (PayLine l : parse(p.getBreakdownJson())) {
                boolean ded = "DEDUCTION".equalsIgnoreCase(l.type());
                td(t, (ded ? "(-) " : "") + l.label());
                tdRight(t, (ded ? "- " : "") + money(l.amount()));
            }
            tdBold(t, "Gross earnings");
            tdRightBold(t, money(p.getGrossEarnings()));
            tdBold(t, "Total deductions");
            tdRightBold(t, "- " + money(p.getTotalDeductions()));
            doc.add(t);
            doc.add(spacer(10));

            // Net pay
            PdfPTable net = new PdfPTable(2);
            net.setWidthPercentage(100);
            net.setWidths(new float[]{3f, 1.4f});
            PdfPCell nl = new PdfPCell(new Phrase("NET PAY", font(12, Font.BOLD, Color.WHITE)));
            nl.setBackgroundColor(NAVY);
            nl.setPadding(8);
            nl.setBorder(0);
            PdfPCell nv = new PdfPCell(new Phrase(money(p.getNetPay()), font(12, Font.BOLD, Color.WHITE)));
            nv.setBackgroundColor(NAVY);
            nv.setPadding(8);
            nv.setBorder(0);
            nv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            net.addCell(nl);
            net.addCell(nv);
            doc.add(net);

            doc.add(spacer(16));
            doc.add(new Paragraph("Generated on " + LocalDate.now() + " · Computer-generated salary slip; no signature required.",
                    font(8, Font.ITALIC, GREY)));

            doc.close();
        } catch (Exception ex) {
            log.warn("Salary slip PDF failed: {}", ex.getMessage());
            throw new BusinessException("Could not generate the salary slip PDF.");
        }
        return baos.toByteArray();
    }

    // ── helpers ──
    private String setting(String key, String def) {
        return settings.findById(key).map(AppSetting::getValue)
                .filter(v -> v != null && !v.isBlank()).orElse(def);
    }

    private byte[] fetch(String url) {
        if (url == null || url.isBlank() || url.toLowerCase().endsWith(".svg")) return null; // OpenPDF can't rasterise SVG
        try {
            HttpResponse<byte[]> res = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            return res.statusCode() / 100 == 2 ? res.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<PayLine> parse(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<PayLine>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static Font font(float size, int style, Color c) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, c);
    }

    private static Paragraph right(Paragraph p) {
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static Paragraph spacer(float h) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(h);
        return p;
    }

    private static void kv(PdfPTable t, String k, String v) {
        PdfPCell kc = new PdfPCell(new Phrase(k, font(9, Font.BOLD, GREY)));
        kc.setBorder(0);
        kc.setPaddingBottom(4);
        PdfPCell vc = new PdfPCell(new Phrase(v == null ? "-" : v, font(9, Font.NORMAL, NAVY)));
        vc.setBorder(0);
        vc.setPaddingBottom(4);
        t.addCell(kc);
        t.addCell(vc);
    }

    private static void th(PdfPTable t, String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, font(10, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(NAVY);
        c.setPadding(6);
        t.addCell(c);
    }

    private static void td(PdfPTable t, String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, font(10, Font.NORMAL, NAVY)));
        c.setPadding(6);
        t.addCell(c);
    }

    private static void tdRight(PdfPTable t, String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, font(10, Font.NORMAL, NAVY)));
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private static void tdBold(PdfPTable t, String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, font(10, Font.BOLD, NAVY)));
        c.setPadding(6);
        c.setBackgroundColor(new Color(238, 241, 246));
        t.addCell(c);
    }

    private static void tdRightBold(PdfPTable t, String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, font(10, Font.BOLD, NAVY)));
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBackgroundColor(new Color(238, 241, 246));
        t.addCell(c);
    }

    /** "Rs. 12,500" — avoids the ₹ glyph which the base PDF fonts don't include. */
    private static String money(BigDecimal v) {
        BigDecimal x = (v == null ? BigDecimal.ZERO : v).setScale(0, RoundingMode.HALF_UP);
        return "Rs. " + String.format("%,d", x.longValueExact());
    }

    private static String str(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private static String opt(String s) {
        return s == null ? "" : s;
    }
}
