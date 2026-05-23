package net.focik.homeoffice.goahead.domain.cost;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.utils.MoneyUtils;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Period;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static net.focik.homeoffice.utils.prints.FontUtil.*;

@Slf4j
public class CostPdf {

    private final static List<String> ITEMS_HEADERS = Arrays.asList("L.p", "Nazwa usługi", "VAT*", "Jm",
            "Ilość", "Cena netto", "Wartość netto", "Stawka VAT", "Wartość VAT", "Wartość brutto");

    public CostPdf() {
    }

    public static String createPdf(Cost cost, byte[] qrCode) {
        log.debug("Trying to create pdf file for cost {}",cost);
        Document document = new Document();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cost-" + cost.getNumber().replace('/', '_') + "-", ".pdf");
            PdfWriter.getInstance(document, new FileOutputStream(tempFile));
            document.open();

            document.add(createNumber(cost));
            log.debug("Created number");
            document.add(createPayment(cost));
            log.debug("Created payment");
            document.add(createSupplier(cost.getSupplier()));
            log.debug("Created supplier");
            document.add(createBuyer());
            log.debug("Created buyer");
            document.add(createItemTable(cost));
            log.debug("Created items table");
            document.add(createItemTableSummary(cost));
            log.debug("Created table summary");
            document.add(createPaymentSummary(cost));
            log.debug("Created payment summary");
            document.add(createPaymentSummaryByWord(cost));
            log.debug("Created payment summary by word");

            if (qrCode != null) {
                Image img = Image.getInstance(qrCode);
                img.scalePercent(40);
                document.add(img);
                log.debug("Created qr code");
            }

            document.add(createOtherInfo(cost.getOtherInfo()));
            log.debug("Created other info");
            document.add(createSignatures());
            log.debug("Created signatures");

            document.close();
            log.debug("Created pdf file for cost {}",cost);

        } catch (IOException | DocumentException e) {
            log.error("Error creating pdf",e);
            if (tempFile != null) {
                tempFile.delete();
            }
            return null;
        }
        return tempFile.getAbsolutePath();
    }

    private static PdfPTable createNumber(Cost cost) {
        float[] columnWidths = {1, 1, 1, 1};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        //nr faktury
        PdfPCell cellNr = new PdfPCell(new Phrase("FAKTURA NR", FONT_12));
        cellNr.setBackgroundColor(HEADER_COLOR);
        cellNr.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellNr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellNr.setRowspan(2);
        table.addCell(cellNr);
        PdfPCell cellNr2 = new PdfPCell(new Phrase(cost.getNumber(), FONT_12_BOLD));
        cellNr2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellNr2.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellNr2.setRowspan(2);
        table.addCell(cellNr2);

        //data wystawienia value
        PdfPCell cellInvDate = new PdfPCell(new Phrase(cost.getInvoiceDate().toString(), FONT_10_BOLD));
        cellInvDate.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellInvDate);

        //data wykonania value
        PdfPCell cellSellDate = new PdfPCell(new Phrase(cost.getInvoiceDate().toString(), FONT_10_BOLD));
        cellSellDate.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellSellDate);

        //data wystawienia string
        PdfPCell cellInvDate2 = new PdfPCell(new Phrase("data wykonania", FONT_8));
        cellInvDate2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellInvDate2.setBackgroundColor(HEADER_COLOR);
        table.addCell(cellInvDate2);

        //data wykonania string
        PdfPCell cellSellDate2 = new PdfPCell(new Phrase("data wystawienia", FONT_8));
        cellSellDate2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellSellDate2.setBackgroundColor(HEADER_COLOR);
        table.addCell(cellSellDate2);

        table.setSpacingAfter(5f);
        return table;
    }

    private static PdfPTable createPayment( Cost cost) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //payment
        Phrase pay = new Phrase();
        Chunk pay1 = new Chunk("Forma płatności: ", FONT_10);
        Chunk pay2 = new Chunk(String.format(" %s %d dni", cost.getPaymentMethod().getTranslate(),
                Period.between(cost.getInvoiceDate(), cost.getPaymentDate()).getDays()), FONT_10_BOLD);
        Chunk pay3 = new Chunk("    Termin płatności: ", FONT_10);
        Chunk pay4 = new Chunk(cost.getPaymentDate().toString(), FONT_10_BOLD);
        pay.add(pay1);
        pay.add(pay2);
        pay.add(pay3);
        pay.add(pay4);
        pay.add(Chunk.NEWLINE);
        pay.add(new Chunk(" ", FONT_EMPTY_SPACE));
        pay.add(Chunk.NEWLINE);

        //bank
        Phrase bank = new Phrase();
        Chunk bank1 = new Chunk("Bank: ", FONT_10);
        Chunk bank2 = new Chunk(cost.getSupplier().getBankName() == null ? "" : cost.getSupplier().getBankName(), FONT_10_BOLD);
        Chunk bank3 = new Chunk("   Nr konta: ", FONT_10);
        Chunk bank4 = new Chunk(cost.getSupplier().getAccountNumber() == null ? "" : cost.getSupplier().getAccountNumber(), FONT_10_BOLD);
        bank.add(bank1);
        bank.add(bank2);
        bank.add(bank3);
        bank.add(bank4);
        bank.add(Chunk.NEWLINE);
        bank.add(new Chunk(" ", FONT_EMPTY_SPACE));
        bank.add(Chunk.NEWLINE);


        Paragraph p = new Paragraph();
        p.add(pay);
        p.setLeading(20f);
        p.add(bank);

        p.setSpacingAfter(20f);

        table.addCell(p);
        table.setSpacingAfter(5f);
        return table;
    }

    private static PdfPTable createBuyer() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //name
        Phrase name = new Phrase();
        Chunk name1 = new Chunk("Nabywca: ", FONT_10);
        Chunk name2 = new Chunk("GO AHEAD usługi językowe Agnieszka Krutowska", FONT_10_BOLD);
        name.add(name1);
        name.add(name2);
        name.add(Chunk.NEWLINE);
        name.add(new Chunk(" ", FONT_EMPTY_SPACE));
        name.add(Chunk.NEWLINE);

        //adres
        Phrase address = new Phrase();
        Chunk adr1 = new Chunk("Adres: ", FONT_10);
        Chunk adr2 = new Chunk("ul. Szyperska 13D/32, 61-754 Poznań", FONT_10_BOLD);
        address.add(adr1);
        address.add(adr2);
        address.add(Chunk.NEWLINE);
        address.add(new Chunk(" ", FONT_EMPTY_SPACE));
        address.add(Chunk.NEWLINE);

        //tel
        Phrase phone = new Phrase();
        Chunk phone1 = new Chunk("Telefon: ", FONT_10);
        Chunk phone2 = new Chunk("+48 501 528 532", FONT_10_BOLD);
        phone.add(phone1);
        phone.add(phone2);
        phone.add(Chunk.NEWLINE);
        phone.add(new Chunk(" ", FONT_EMPTY_SPACE));
        phone.add(Chunk.NEWLINE);

        //mail
        Phrase mail = new Phrase();
        Chunk mail1 = new Chunk("E-mail: ", FONT_10);
        Chunk mail2 = new Chunk("akrutowska@gmail.com", FONT_10_BOLD);
        mail.add(mail1);
        mail.add(mail2);
        mail.add(Chunk.NEWLINE);
        mail.add(Chunk.NEWLINE);

        //nip
        Phrase nip = new Phrase();
        Chunk nip1 = new Chunk("NIP: ", FONT_10);
        Chunk nip2 = new Chunk("972-049-58-27", FONT_10_BOLD);
        nip.add(nip1);
        nip.add(nip2);
        nip.add(Chunk.NEWLINE);
        nip.add(new Chunk(" ", FONT_EMPTY_SPACE));
        nip.add(Chunk.NEWLINE);

        Paragraph p = new Paragraph();
        p.add(name);
        p.add(address);
        p.add(phone);
        p.add(mail);
        p.add(nip);

        table.addCell(p);
        table.setSpacingAfter(5f);
        return table;
    }

    private static PdfPTable createSupplier(Supplier supplier) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //name
        Phrase name = new Phrase();
        Chunk name1 = new Chunk("Sprzedawca: ", FONT_10);
        Chunk name2 = new Chunk(supplier.getName(), FONT_10_BOLD);
        name.add(name1);
        name.add(name2);
        name.add(Chunk.NEWLINE);
        name.add(new Chunk(" ", FONT_EMPTY_SPACE));
        name.add(Chunk.NEWLINE);

        //adres
        Phrase address = new Phrase();
        Chunk adr1 = new Chunk("Adres: ", FONT_10);
        Chunk adr2 = new Chunk(supplier.getAddress().toString(), FONT_10_BOLD);
        address.add(adr1);
        address.add(adr2);
        address.add(Chunk.NEWLINE);
        address.add(new Chunk(" ", FONT_EMPTY_SPACE));
        address.add(Chunk.NEWLINE);

        //tel
        Phrase phone = new Phrase();
        Chunk phone1 = new Chunk("Telefon: ", FONT_10);
        Chunk phone2 = new Chunk(supplier.getPhone(), FONT_10_BOLD);
        phone.add(phone1);
        phone.add(phone2);
        phone.add(Chunk.NEWLINE);
        phone.add(new Chunk(" ", FONT_EMPTY_SPACE));
        phone.add(Chunk.NEWLINE);

        //mail
        Phrase mail = new Phrase();
        Chunk mail1 = new Chunk("E-mail: ", FONT_10);
        Chunk mail2 = new Chunk(supplier.getMail(), FONT_10_BOLD);
        mail.add(mail1);
        mail.add(mail2);
        mail.add(Chunk.NEWLINE);

        //nip
        Phrase nip = new Phrase();
        Chunk nip1 = new Chunk("NIP: ", FONT_10);
        Chunk nip2 = new Chunk(Objects.nonNull(supplier.getNip()) ? supplier.getNip() : "", FONT_10_BOLD);
        nip.add(nip1);
        nip.add(nip2);
        nip.add(Chunk.NEWLINE);
        nip.add(new Chunk(" ", FONT_EMPTY_SPACE));
        nip.add(Chunk.NEWLINE);

        Paragraph p = new Paragraph();
        p.add(name);
        p.add(address);
        p.add(phone);
        p.add(mail);
        p.add(Chunk.NEWLINE);
        p.add(nip);

        table.addCell(p);

        table.setSpacingAfter(5f);
        return table;
    }

    private static PdfPTable createItemTable( Cost cost) {

        float[] columnWidths = {1.1f, 5.8f, 1.4f, 1.2f, 1.4f, 2.2f, 2.4f, 2.0f, 2.2f, 2.4f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        addTableHeader(table);
        addRows(table, cost.getCostItems());
        table.setSpacingAfter(5f);
        return table;
    }

    private static void addTableHeader(PdfPTable table) {
        CostPdf.ITEMS_HEADERS.forEach(columnTitle -> {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(HEADER_COLOR);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setPhrase(new Phrase(columnTitle, FONT_10));
            table.addCell(header);
        });
    }

    private static void addRows(PdfPTable table, List<CostItem> items) {
        int index = 1;
        for (CostItem item : items) {
            addCell(table, String.valueOf(index++), Element.ALIGN_CENTER);
            addCell(table, item.getName(), Element.ALIGN_LEFT);
            addCell(table, item.getVat().getViewValue(), Element.ALIGN_CENTER);
            addCell(table, item.getUnit(), Element.ALIGN_CENTER);
            addCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER);
            addCell(table, formatMoney(item.getAmountNet()), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(item.getAmountNet().multiply(item.getQuantity())), Element.ALIGN_RIGHT);
            addCell(table, item.getVat().getViewValue(), Element.ALIGN_CENTER);
            addCell(table, formatMoney(item.getAmountVat().multiply(item.getQuantity())), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(item.getAmountGross().multiply(item.getQuantity())), Element.ALIGN_RIGHT);
        }
    }

    private static PdfPTable createItemTableSummary( Cost cost) {

        float[] columnWidths = {2.2f, 2.4f, 2.0f, 2.2f, 2.4f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        Map<Vat, List<CostItem>> itemsByVat = cost.getCostItems().stream()
                .collect(Collectors.groupingBy(CostItem::getVat));

        Money totalNet = Money.of(0, "PLN");
        Money totalVat = Money.of(0, "PLN");
        Money totalGross = Money.of(0, "PLN");

        // Jedna iteracja — zbieramy sumy i zapisujemy wyniki per VAT
        record VatSums(Vat vat, Money net, Money vat_, Money gross) {}

        List<VatSums> vatSumsList = itemsByVat.entrySet().stream()
                .map(entry -> {
                    Money net = entry.getValue().stream()
                            .map(item -> item.getAmountNet().multiply(item.getQuantity()))
                            .reduce(Money.of(0, "PLN"), Money::add);
                    Money vat = entry.getValue().stream()
                            .map(item -> item.getAmountVat().multiply(item.getQuantity()))
                            .reduce(Money.of(0, "PLN"), Money::add);
                    Money gross = entry.getValue().stream()
                            .map(item -> item.getAmountGross().multiply(item.getQuantity()))
                            .reduce(Money.of(0, "PLN"), Money::add);
                    return new VatSums(entry.getKey(), net, vat, gross);
                })
                .sorted(Comparator.comparing(vs -> vs.vat().getNumberValue(), Comparator.reverseOrder()))
                .toList();

        for (VatSums vs : vatSumsList) {
            totalNet = totalNet.add(vs.net());
            totalVat = totalVat.add(vs.vat_());
            totalGross = totalGross.add(vs.gross());
        }

        addCellWithBackground(table, "RAZEM");
        addCell(table, formatMoney(totalNet), Element.ALIGN_RIGHT);
        addCell(table, "X", Element.ALIGN_CENTER);
        addCell(table, formatMoney(totalVat), Element.ALIGN_RIGHT);
        addCell(table, formatMoney(totalGross), Element.ALIGN_RIGHT);

        for (VatSums vs : vatSumsList) {
            addCell(table, "W tym", Element.ALIGN_CENTER);
            addCell(table, formatMoney(vs.net()), Element.ALIGN_RIGHT);
            addCell(table, vs.vat().getViewValue(), Element.ALIGN_CENTER);
            addCell(table, formatMoney(vs.vat_()), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(vs.gross()), Element.ALIGN_RIGHT);
        }

        PdfPTable outerTable = new PdfPTable(new float[]{10.9f, 11.2f});
        outerTable.setWidthPercentage(100);
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(0);
        outerTable.addCell(emptyCell);

        PdfPCell innerTableCell = new PdfPCell(table);
        innerTableCell.setBorder(0);
        innerTableCell.setPadding(0);
        outerTable.addCell(innerTableCell);

        outerTable.setSpacingAfter(15f);
        return outerTable;
    }

    private static void addCell(PdfPTable table, String value, int horizontalAlignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FONT_10));
        cell.setHorizontalAlignment(horizontalAlignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addCellWithBackground(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FONT_10));
        cell.setBackgroundColor(HEADER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static String formatMoney(Money money) {
        return String.format("%.2f", money.getNumberStripped());
    }

    private static PdfPTable createPaymentSummary( Cost cost) {

        float[] columnWidths = {1, 1, 1};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        //to pay
        Phrase toPay = new Phrase();
        Chunk toPay1 = new Chunk("Razem do zapłaty: ", FONT_10);
        Chunk toPay2 =  new Chunk(MoneyUtils.mapMoneyToString(cost.getAmountSum()), FONT_10_BOLD);
        toPay.add(toPay1);
        toPay.add(toPay2);
        toPay.add(Chunk.NEWLINE);
        toPay.add(new Chunk(" ", FONT_EMPTY_SPACE));
        toPay.add(Chunk.NEWLINE);

        Paragraph parToPay = new Paragraph();
        parToPay.add(toPay);
        table.addCell(parToPay);

        PdfPCell cellEmpty = new PdfPCell(new Phrase("", FONT_8));
        cellEmpty.setColspan(2);
        cellEmpty.setBorder(0);
        table.addCell(cellEmpty);

        table.setSpacingAfter(5f);
        return table;
    }

    private static PdfPTable createPaymentSummaryByWord( Cost cost) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //to pay by word
        Phrase toPayByWord = new Phrase();
        Chunk toPayByWord1 = new Chunk("Słownie: ", FONT_10);
        Chunk toPayByWord2 = new Chunk(MoneyUtils.amountByWords(cost.getAmountSum()), FONT_10_BOLD);
        toPayByWord.add(toPayByWord1);
        toPayByWord.add(toPayByWord2);
        toPayByWord.add(Chunk.NEWLINE);
        toPayByWord.add(new Chunk(" ", FONT_EMPTY_SPACE));
        toPayByWord.add(Chunk.NEWLINE);

        Paragraph parToPayByWord = new Paragraph();
        parToPayByWord.add(toPayByWord);
        PdfPCell cell = new PdfPCell(parToPayByWord);
        table.addCell(cell);

        table.setSpacingAfter(15f);
        return table;
    }

    private static PdfPTable createOtherInfo(String message) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(HEADER_COLOR);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.setPhrase(new Phrase("UWAGI", FONT_10));
        table.addCell(header);

        PdfPCell cell = new PdfPCell(new Phrase(message, FONT_10));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setMinimumHeight(36f);
        table.addCell(cell);

        table.setSpacingAfter(10f);
        return table;
    }

    private static PdfPTable createSignatures() {

        float[] columnWidths = {1, 5, 1, 5, 1};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        PdfPCell cellEmpty = new PdfPCell(new Phrase("", FONT_10));
        cellEmpty.setBorderWidth(0);
        table.addCell(cellEmpty);

        PdfPCell headerBuy = new PdfPCell();
        headerBuy.setBackgroundColor(HEADER_COLOR);
        headerBuy.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerBuy.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerBuy.setPhrase(new Phrase("Fakturę odebrał", FONT_8));
        table.addCell(headerBuy);

        PdfPCell cellEmpty1 = new PdfPCell(new Phrase("", FONT_10));
        cellEmpty1.setBorderWidth(0);
        table.addCell(cellEmpty1);

        PdfPCell headerSell = new PdfPCell();
        headerSell.setBackgroundColor(HEADER_COLOR);
        headerSell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerSell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerSell.setPhrase(new Phrase("Fakturę wystawił", FONT_8));
        table.addCell(headerSell);

        PdfPCell cellEmpty2 = new PdfPCell(new Phrase("", FONT_10));
        cellEmpty2.setBorderWidth(0);
        table.addCell(cellEmpty2);

        PdfPCell cellEmptySec1 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec1.setBorderWidth(0);
        table.addCell(cellEmptySec1);
        PdfPCell cellEmptySec2 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec2.setFixedHeight(36f);
        table.addCell(cellEmptySec2);
        PdfPCell cellEmptySec3 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec3.setBorderWidth(0);
        table.addCell(cellEmptySec3);
        PdfPCell cellEmptySec4 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec4.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellEmptySec4.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellEmptySec4.setFixedHeight(36f);
        table.addCell(cellEmptySec4);
        PdfPCell cellEmptySec5 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec5.setBorderWidth(0);
        table.addCell(cellEmptySec5);

        return table;
    }

}