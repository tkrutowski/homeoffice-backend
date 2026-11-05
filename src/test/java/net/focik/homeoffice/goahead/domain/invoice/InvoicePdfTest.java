package net.focik.homeoffice.goahead.domain.invoice;

import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.Vat;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfTest {

    private Invoice invoice(Customer customer) {
        InvoiceItem item = new InvoiceItem();
        item.setName("Kurs języka angielskiego");
        item.setUnit("h");
        item.setQuantity(7f);
        item.setAmount(Money.of(100, "PLN"));
        item.setVat(Vat.VAT_ZW);

        Invoice invoice = new Invoice();
        invoice.setNumber("2026/47");
        invoice.setPaymentMethod(PaymentMethod.TRANSFER);
        invoice.setInvoiceDate(LocalDate.of(2026, 11, 2));
        invoice.setSellDate(LocalDate.of(2026, 11, 2));
        invoice.setPaymentDate(LocalDate.of(2026, 11, 16));
        invoice.setInvoiceItems(List.of(item));
        invoice.setCustomer(customer);
        return invoice;
    }

    /** Klient założony importem z KSeF bez danych kontaktowych: telefon, mail i adres = null. */
    private Customer customerWithoutContacts() {
        Customer customer = new Customer();
        customer.setName("Centrum Rozwoju Szkół Wyższych Grupa VAT");
        customer.setNip("2090007170");
        customer.setCustomerType(CustomerType.COMPANY);
        return customer;
    }

    private String pdfText(Invoice invoice) throws Exception {
        String path = InvoicePdf.createPdf(invoice, null);
        assertThat(path).isNotNull();
        File file = new File(path);
        try (var doc = Loader.loadPDF(file)) {
            return new PDFTextStripper().getText(doc);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    @DisplayName("PDF generuje się dla klienta z null w telefonie, mailu i adresie")
    void createPdf_customerWithNullContacts() throws Exception {
        String text = pdfText(invoice(customerWithoutContacts()));

        assertThat(text).contains("Centrum Rozwoju").contains("2090007170");
    }

    @Test
    @DisplayName("Faktura grupowa: PDF zawiera odbiorcę - członka grupy VAT")
    void createPdf_vatGroupInvoice_containsRecipient() throws Exception {
        Invoice invoice = invoice(customerWithoutContacts());
        invoice.setVatGroupRecipientNip("7781028941");
        invoice.setVatGroupRecipientName("UNIWERSYTET WSB MERITO W POZNANIU");
        invoice.setVatGroupRecipientStreet("ul. Powstańców Wielkopolskich 5");
        invoice.setVatGroupRecipientZip("61-895");
        invoice.setVatGroupRecipientCity("Poznań");

        String text = pdfText(invoice);

        assertThat(text)
                .contains("Odbiorca")
                .contains("UNIWERSYTET WSB MERITO")
                .contains("7781028941")
                .contains("61-895");
    }

    @Test
    @DisplayName("Faktura zwykła: PDF nie zawiera bloku odbiorcy")
    void createPdf_regularInvoice_hasNoRecipientBlock() throws Exception {
        String text = pdfText(invoice(customerWithoutContacts()));

        assertThat(text).doesNotContain("Odbiorca");
    }
}
