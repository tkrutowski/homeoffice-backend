package net.focik.homeoffice.goahead.domain.invoice;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.utils.MoneyUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.focik.homeoffice.utils.prints.FontUtil.*;

@Slf4j
public class InvoicePdf {

    private final static List<String> ITEMS_HEADERS = Arrays.asList("L.p", "Nazwa usługi", "VAT*", "Jm",
            "Ilość", "Wartość jednostkowa", "Wartość usługi");

    public InvoicePdf() throws DocumentException, IOException {
    }

    public static String createPdf(Invoice invoice) {
        log.debug("Trying to create pdf file for invoice {}",invoice);
        Document document = new Document();
        final String filename = "faktura.pdf";
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            document.add(createNumber(invoice));
            log.debug("Created number");
            document.add(createPayment(invoice));
            log.debug("Created payment");
            document.add(createSeller(invoice));
            log.debug("Created seller");
            document.add(createBuyer(invoice.getCustomer()));
            log.debug("Created buyer");
            document.add(createItemTable(invoice));
            log.debug("Created items table");
            document.add(createItemTableSummary(invoice));
            log.debug("Created table summary");
            document.add(createPaymentSummary(invoice));
            log.debug("Created payment summary");
            document.add(createPaymentSummaryByWord(invoice));
            log.debug("Created payment summary by word");
            document.add(createOtherInfo(invoice.getOtherInfo()));
            log.debug("Created other info");
            document.add(createSignatures());
            log.debug("Created signatures");

            document.close();
            log.debug("Created pdf file for invoice {}",invoice);

        } catch (IOException | com.itextpdf.text.DocumentException e) {
            log.error("Error creating pdf",e);
            return null;
        }
        return filename;
    }

    private static PdfPTable createNumber(Invoice invoice) {
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
        PdfPCell cellNr2 = new PdfPCell(new Phrase(invoice.getInvoiceNumber(), FONT_12_BOLD));
        cellNr2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellNr2.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellNr2.setRowspan(2);
        table.addCell(cellNr2);

        //data wystawienia value
        PdfPCell cellInvDate = new PdfPCell(new Phrase(invoice.getInvoiceDate().toString(), FONT_10_BOLD));
        cellInvDate.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellInvDate);

        //data wykonania value
        PdfPCell cellSellDate = new PdfPCell(new Phrase(invoice.getInvoiceDate().toString(), FONT_10_BOLD));
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

    private static PdfPTable createPayment(Invoice invoice) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //payment
        Phrase pay = new Phrase();
        Chunk pay1 = new Chunk("Forma płatności: ", FONT_10);
        Chunk pay2 = new Chunk(String.format(" %s %d dni", invoice.getPaymentMethod().getViewValue(),
                Period.between(invoice.getInvoiceDate(), invoice.getPaymentDate()).getDays()), FONT_10_BOLD);
        Chunk pay3 = new Chunk("    Termin płatności: ", FONT_10);
        Chunk pay4 = new Chunk(invoice.getPaymentDate().toString(), FONT_10_BOLD);
        pay.add(pay1);
        pay.add(pay2);
        pay.add(pay3);
        pay.add(pay4);
        pay.add(Chunk.NEWLINE);
        pay.add(new Chunk(" ", FONT_EMPTY_SPACE));
        pay.add(Chunk.NEWLINE);
//        pay.setLeading(50f);

        //bank
        Phrase bank = new Phrase();
        Chunk bank1 = new Chunk("Bank: ", FONT_10);
        Chunk bank2 = new Chunk("Santander Bank", FONT_10_BOLD);
        Chunk bank3 = new Chunk("   Nr konta: ", FONT_10);
        Chunk bank4 = new Chunk("93 1910 1048 2273 6485 5801 0001", FONT_10_BOLD);
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

    private static PdfPTable createSeller(Invoice invoice) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //name
        Phrase name = new Phrase();
        Chunk name1 = new Chunk("Sprzedawca: ", FONT_10);
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

    private static PdfPTable createBuyer(Customer c) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //name
        Phrase name = new Phrase();
        Chunk name1 = new Chunk("Nabywca: ", FONT_10);
        Chunk name2 = new Chunk(c.getName(), FONT_10_BOLD);
        name.add(name1);
        name.add(name2);
        name.add(Chunk.NEWLINE);
        name.add(new Chunk(" ", FONT_EMPTY_SPACE));
        name.add(Chunk.NEWLINE);

        //adres
        Phrase address = new Phrase();
        Chunk adr1 = new Chunk("Adres: ", FONT_10);
        Chunk adr2 = new Chunk(c.getAddress().toString(), FONT_10_BOLD);
        address.add(adr1);
        address.add(adr2);
        address.add(Chunk.NEWLINE);
        address.add(new Chunk(" ", FONT_EMPTY_SPACE));
        address.add(Chunk.NEWLINE);

        //tel
        Phrase phone = new Phrase();
        Chunk phone1 = new Chunk("Telefon: ", FONT_10);
        Chunk phone2 = new Chunk(c.getPhone(), FONT_10_BOLD);
        phone.add(phone1);
        phone.add(phone2);
        phone.add(Chunk.NEWLINE);
        phone.add(new Chunk(" ", FONT_EMPTY_SPACE));
        phone.add(Chunk.NEWLINE);

        //mail
        Phrase mail = new Phrase();
        Chunk mail1 = new Chunk("E-mail: ", FONT_10);
        Chunk mail2 = new Chunk(c.getMail(), FONT_10_BOLD);
        mail.add(mail1);
        mail.add(mail2);
        mail.add(Chunk.NEWLINE);

        //nip
        Phrase nip = new Phrase();
        Chunk nip1 = new Chunk("NIP: ", FONT_10);
        Chunk nip2 = new Chunk(Objects.nonNull(c.getNip()) ? c.getNip() : "", FONT_10_BOLD);
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

    private static PdfPTable createItemTable(Invoice invoice) {

        float[] columnWidths = {1, 6, 2, 2, 2, 3, 3};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        addTableHeader(table, ITEMS_HEADERS);
        addRows(table, invoice.getInvoiceItems());
//
//        PdfPCell cell = new PdfPCell(new Phrase("*Ustawa o VAT (DZ. U. z 2004r. nr.54, poz.535) art. 43 pkt 1 ust. 28", smallFont));
//      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
//        cell.setColspan(3);
//        cell.setBorder(0);
//        table.addCell(cell);
//
//        PdfPCell cellStr = new PdfPCell(new Phrase("RAZEM", mainFont));
//        cellStr.setBackgroundColor(GrayColor.GRAYBLACK);
//        cellStr.setHorizontalAlignment(Element.ALIGN_CENTER);
//        table.addCell(cellStr);

        return table;
    }

    private static void addTableHeader(PdfPTable table, java.util.List<String> headers) {
//        Stream.of("L.p", "Nazwa towaru/usługi", "Podstawa prawna zwolnienia*","Jm",
//                        "Ilość","Wartość jednostkowa","Wartość towaru/usługi")
        headers.forEach(columnTitle -> {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(HEADER_COLOR);
//            header.setBorderWidth(2);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setPhrase(new Phrase(columnTitle, FONT_10));
            table.addCell(header);
        });
    }

    private static void addRows(PdfPTable table, List<InvoiceItem> items) {
        int index = 1;
        for (InvoiceItem item : items) {
            //id
            PdfPCell cellId = new PdfPCell(new Phrase(String.valueOf(index++), FONT_10));
            cellId.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellId.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellId);

            //name
            PdfPCell cellName = new PdfPCell(new Phrase(item.getName(), FONT_10));
            cellName.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellName.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellName);

            //zw
            PdfPCell cellZw = new PdfPCell(new Phrase("zw.", FONT_10));
            cellZw.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellZw.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellZw);

            //unit
            PdfPCell cellUnit = new PdfPCell(new Phrase(item.getUnit(), FONT_10));
            cellUnit.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellUnit.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellUnit);

            //quantity
            PdfPCell cellQuantity = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), FONT_10));
            cellQuantity.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellQuantity.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellQuantity);

            //amount per unit
            PdfPCell cellPerUnit = new PdfPCell(new Phrase(String.format("%.2f", item.getAmount().getNumberStripped()), FONT_10));
            cellPerUnit.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellPerUnit.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellPerUnit);

            //amount sum
            PdfPCell cellSum = new PdfPCell(new Phrase(String.format("%.2f", item.getAmount().multiply(item.getQuantity()).getNumberStripped()), FONT_10));
            cellSum.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSum.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cellSum);

//            table.addCell(String.valueOf(index++));//lp
//            table.addCell(item.getName());//name
//            table.addCell("zw");//podstawa prawna zwolnienia
//            table.addCell(item.getUnit());//jm
//            table.addCell(String.valueOf(item.getQuantity()));//ilosc
//            table.addCell(String.format("%.2f", item.getAmount().getNumberStripped()));
//            table.addCell(String.format("%.2f", item.getAmount().multiply(item.getQuantity()).getNumberStripped()));
        }
    }

    private static PdfPTable createItemTableSummary(Invoice invoice) {

        float[] columnWidths = {13, 3, 3};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase("*Ustawa o VAT (DZ. U. z 2004r. nr.54, poz.535) art. 43 pkt 1 ust. 28", FONT_8));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
//        cell.setColspan(3);
        cell.setBorder(0);
        table.addCell(cell);

//        PdfPCell cellEmpty = new PdfPCell(new Phrase("RAZEM", mainFont));
//        cellEmpty.setBackgroundColor(BaseColor.LIGHT_GRAY);
//        cellEmpty.setHorizontalAlignment(Element.ALIGN_CENTER);
//        table.addCell(cellEmpty);

        PdfPCell cellStr = new PdfPCell(new Phrase("RAZEM", FONT_10));
        cellStr.setBackgroundColor(HEADER_COLOR);
        cellStr.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellStr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cellStr);


        PdfPCell cellSum = new PdfPCell(new Phrase(String.format("%.2f", invoice.getAmount().getNumberStripped()), FONT_10));
        cellSum.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellSum.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cellSum);

        table.setSpacingAfter(15f);
        return table;
    }


    private static PdfPTable createPaymentSummary(Invoice invoice) {

        float[] columnWidths = {1, 1, 1};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        //to pay
        Phrase toPay = new Phrase();
        Chunk toPay1 = new Chunk("Razem do zapłaty: ", FONT_10);
        Chunk toPay2 =  new Chunk(MoneyUtils.mapMoneyToString(invoice.getAmount()), FONT_10_BOLD);
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

    private static PdfPTable createPaymentSummaryByWord(Invoice invoice) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        //to pay by word
        Phrase toPayByWord = new Phrase();
        Chunk toPayByWord1 = new Chunk("Słownie: ", FONT_10);
//        Chunk toPayByWord2 = new Chunk("Test", mainFontBold);
        Chunk toPayByWord2 = new Chunk(MoneyUtils.amountByWords(invoice.getAmount()), FONT_10_BOLD);
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
//        header.setBorderWidth(2);
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
//        headerBuy.setBorderWidth(2);
        headerBuy.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerBuy.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerBuy.setPhrase(new Phrase("Fakturę odebrał", FONT_8));
        table.addCell(headerBuy);

        PdfPCell cellEmpty1 = new PdfPCell(new Phrase("", FONT_10));
        cellEmpty1.setBorderWidth(0);
        table.addCell(cellEmpty1);

        PdfPCell headerSell = new PdfPCell();
        headerSell.setBackgroundColor(HEADER_COLOR);
//        headerSell.setBorderWidth(2);
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
//        cellEmptySec2.setBorderWidth(0);
        cellEmptySec2.setFixedHeight(36f);
        table.addCell(cellEmptySec2);
        PdfPCell cellEmptySec3 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec3.setBorderWidth(0);
        table.addCell(cellEmptySec3);
        PdfPCell cellEmptySec4 = new PdfPCell(new Phrase("Agnieszka Krutowska", FONT_10));
        cellEmptySec4.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellEmptySec4.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellEmptySec4.setFixedHeight(36f);
//        cellEmptySec4.setBorderWidth(0);
        table.addCell(cellEmptySec4);
        PdfPCell cellEmptySec5 = new PdfPCell(new Phrase("", FONT_10));
        cellEmptySec5.setBorderWidth(0);
        table.addCell(cellEmptySec5);


        return table;
    }

    private static void addCustomRows(PdfPTable table)
            throws URISyntaxException, BadElementException, IOException {
//        Path path = Paths.get(ClassLoader.getSystemResource("Java_logo.png").toURI());
//        Image img = Image.getInstance(path.toAbsolutePath().toString());
//        img.scalePercent(10);

//        PdfPCell imageCell = new PdfPCell(img);
//        table.addCell(imageCell);

        PdfPCell horizontalAlignCell = new PdfPCell(new Phrase("row 2, col 2"));
        horizontalAlignCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(horizontalAlignCell);

        PdfPCell verticalAlignCell = new PdfPCell(new Phrase("row 2, col 3"));
        verticalAlignCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        table.addCell(verticalAlignCell);
    }

    public void createPdf(String dest) throws IOException, DocumentException, com.itextpdf.text.DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(dest));
        document.open();
        float[] columnWidths = {1, 5, 5};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.getDefaultCell().setUseAscender(true);
        table.getDefaultCell().setUseDescender(true);
        Font f = new Font(Font.FontFamily.HELVETICA, 13, Font.NORMAL, GrayColor.GRAYWHITE);
        PdfPCell cell = new PdfPCell(new Phrase("This is a header", f));
        cell.setBackgroundColor(GrayColor.GRAYBLACK);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setColspan(3);
        table.addCell(cell);
        table.getDefaultCell().setBackgroundColor(new GrayColor(0.75f));
        for (int i = 0; i < 2; i++) {
            table.addCell("#");
            table.addCell("Key");
            table.addCell("Value");
        }
        table.setHeaderRows(3);
        table.setFooterRows(1);
        table.getDefaultCell().setBackgroundColor(GrayColor.GRAYWHITE);
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        for (int counter = 1; counter < 101; counter++) {
            table.addCell(String.valueOf(counter));
            table.addCell("key " + counter);
            table.addCell("value " + counter);
        }
        document.add(table);
        document.close();
    }
}
