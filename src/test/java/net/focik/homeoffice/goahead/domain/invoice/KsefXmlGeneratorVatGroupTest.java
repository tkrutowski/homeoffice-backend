package net.focik.homeoffice.goahead.domain.invoice;

import jakarta.xml.bind.JAXBContext;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.Pozycja;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Wysyłka faktury grupowej: Invoice -> XML dla KSeF (oraz powrót XML -> Invoice). */
class KsefXmlGeneratorVatGroupTest {

    private KsefXmlGenerator generator;
    private Company company;

    @BeforeEach
    void setUp() {
        generator = new KsefXmlGenerator(new KsefInvoiceMapper());
        company = Company.builder()
                .fullName("Go ahead")
                .nip("972-049-58-27")
                .accountNo("93 1910 1048 2273 6485 5801 0001")
                .address(Address.builder().city("Poznań").street("Szyperska 13D").zip("61-754").build())
                .build();
    }

    private Invoice.InvoiceBuilder groupInvoice() {
        Customer groupVat = Customer.builder()
                .customerType(CustomerType.COMPANY)
                .name("Centrum Rozwoju Szkół Wyższych Grupa VAT")
                .nip("2090007170")
                .address(Address.builder().city("Poznań").street("Pastelowa 16").zip("60-198").build())
                .build();

        return Invoice.builder()
                .number("2026/38")
                .invoiceDate(LocalDate.of(2026, 9, 2))
                .sellDate(LocalDate.of(2026, 9, 2))
                .paymentMethod(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.TO_PAY)
                .paymentDate(LocalDate.of(2026, 10, 2))
                .customer(groupVat)
                .invoiceItems(List.of(InvoiceItem.builder()
                        .name("Kurs języka angielskiego")
                        .quantity(6f)
                        .unit("l")
                        .amount(Money.of(75, "PLN"))
                        .vat(Vat.VAT_ZW)
                        .build()))
                .buyerContactEmail("dod@poznan.merito.pl");
    }

    @Test
    @DisplayName("should generate GV=1 and Podmiot3 with role 10 for a VAT group invoice")
    void generateInvoiceXml_ShouldContainVatGroupMember_WhenRecipientProvided() throws Exception {
        // given
        Invoice invoice = groupInvoice()
                .vatGroupRecipientNip("7781028941")
                .vatGroupRecipientName("UNIWERSYTET WSB MERITO W POZNANIU")
                .vatGroupRecipientStreet("Powstańców Wielkopolskich 5")
                .vatGroupRecipientZip("61-895")
                .vatGroupRecipientCity("Poznań")
                .build();

        // when
        String xml = generator.generateInvoiceXml(invoice, company);

        // then
        assertThat(xml).contains("<GV>1</GV>");
        assertThat(xml).contains("<Podmiot3>");
        assertThat(xml).contains("<NIP>7781028941</NIP>");
        assertThat(xml).contains("<Rola>10</Rola>");
        // nabywcą (Podmiot2) pozostaje grupa VAT, a nie jej członek
        assertThat(xml.indexOf("<NIP>2090007170</NIP>")).isBetween(xml.indexOf("<Podmiot2>"), xml.indexOf("</Podmiot2>"));
        assertThat(xml.indexOf("<NIP>7781028941</NIP>")).isBetween(xml.indexOf("<Podmiot3>"), xml.indexOf("</Podmiot3>"));
        // kolejność zgodna ze schematem: Podmiot2 -> Podmiot3 -> Fa
        assertThat(xml.indexOf("<Podmiot3>")).isGreaterThan(xml.indexOf("</Podmiot2>"));
        assertThat(xml.indexOf("<Fa>")).isGreaterThan(xml.indexOf("</Podmiot3>"));
    }

    @Test
    @DisplayName("should generate GV=2 and no Podmiot3 for a regular invoice")
    void generateInvoiceXml_ShouldNotContainPodmiot3_WhenNoRecipient() throws Exception {
        // when
        String xml = generator.generateInvoiceXml(groupInvoice().build(), company);

        // then
        assertThat(xml).contains("<GV>2</GV>");
        assertThat(xml).doesNotContain("<Podmiot3>");
    }

    @Test
    @DisplayName("should give the same VAT group data after generating XML and importing it back")
    void generatedXml_ShouldRoundTripThroughImportMapper() throws Exception {
        // given
        Invoice invoice = groupInvoice()
                .vatGroupRecipientNip("7781028941")
                .vatGroupRecipientName("UNIWERSYTET WSB MERITO W POZNANIU")
                .vatGroupRecipientStreet("Powstańców Wielkopolskich 5")
                .vatGroupRecipientZip("61-895")
                .vatGroupRecipientCity("Poznań")
                .build();
        String xml = generator.generateInvoiceXml(invoice, company);

        // when
        InvoiceKsefDto parsed = (InvoiceKsefDto) JAXBContext.newInstance(InvoiceKsefDto.class)
                .createUnmarshaller().unmarshal(new StringReader(xml));
        for (Pozycja p : parsed.getFakturaCtrl().getPozycje()) {
            p.calculateMissingAmounts();
        }
        Invoice imported = new KsefInvoiceImportMapper().toInvoice(parsed);

        // then
        assertThat(imported.getVatGroupRecipientNip()).isEqualTo("7781028941");
        assertThat(imported.getVatGroupRecipientName()).isEqualTo("UNIWERSYTET WSB MERITO W POZNANIU");
        assertThat(imported.getVatGroupRecipientStreet()).isEqualTo("Powstańców Wielkopolskich 5");
        assertThat(imported.getVatGroupRecipientZip()).isEqualTo("61-895");
        assertThat(imported.getVatGroupRecipientCity()).isEqualTo("Poznań");
        assertThat(imported.getBuyerContactEmail()).isEqualTo("dod@poznan.merito.pl");
        assertThat(imported.getNumber()).isEqualTo("2026/38");
    }
}
