package net.focik.homeoffice.goahead.domain.invoice;

import jakarta.xml.bind.JAXBContext;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.Pozycja;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KsefInvoiceImportMapperTest {

    private static final String NS = "http://crd.gov.pl/wzor/2025/06/25/13775/";

    /** Faktura grupowa wygenerowana przez portal KSeF: nabywca to grupa VAT (GV=1), odbiorca to jej członek (Podmiot3, rola 10). */
    private static final String VAT_GROUP_INVOICE = """
            <?xml version="1.0" encoding="utf-8"?>
            <Faktura xmlns="%s">
              <Naglowek><KodFormularza kodSystemowy="FA (3)" wersjaSchemy="1-0E">FA</KodFormularza><WariantFormularza>3</WariantFormularza></Naglowek>
              <Podmiot1><DaneIdentyfikacyjne><NIP>9720495827</NIP><Nazwa>Go ahead</Nazwa></DaneIdentyfikacyjne>
                <Adres><KodKraju>PL</KodKraju><AdresL1>ul. Szyperska 13D lok. 32 61-754 Poznań</AdresL1></Adres></Podmiot1>
              <Podmiot2>
                <DaneIdentyfikacyjne><NIP>2090007170</NIP><Nazwa>Centrum Rozwoju Szkół Wyższych Grupa VAT</Nazwa></DaneIdentyfikacyjne>
                <Adres><KodKraju>PL</KodKraju><AdresL1>ul. Pastelowa 16, 60-198 Poznań</AdresL1></Adres>
                <DaneKontaktowe><Email>dod@poznan.merito.pl</Email></DaneKontaktowe>
                <JST>2</JST><GV>1</GV>
              </Podmiot2>
              <Podmiot3>
                <DaneIdentyfikacyjne><NIP>7781028941</NIP><Nazwa>UNIWERSYTET WSB MERITO W POZNANIU</Nazwa></DaneIdentyfikacyjne>
                <Adres><KodKraju>PL</KodKraju><AdresL1>ul. Powstańców Wielkopolskich 5, 61-895 Poznań</AdresL1></Adres>
                <DaneKontaktowe><Email>dod@poznan.merito.pl</Email></DaneKontaktowe>
                <Rola>10</Rola>
              </Podmiot3>
              <Fa>
                <KodWaluty>PLN</KodWaluty><P_1>2026-09-02</P_1><P_2>2026/38</P_2><P_6>2026-09-02</P_6>
                <P_13_7>1749.75</P_13_7><P_15>1749.75</P_15>
                <RodzajFaktury>VAT</RodzajFaktury>
                <FaWiersz><NrWierszaFa>1</NrWierszaFa><P_7>Kurs języka angielskiego</P_7><P_8A>l</P_8A><P_8B>6</P_8B><P_9A>75</P_9A><P_11>450</P_11><P_12>zw</P_12></FaWiersz>
                <FaWiersz><NrWierszaFa>2</NrWierszaFa><P_7>MEL</P_7><P_8A>l</P_8A><P_8B>17.33</P_8B><P_9A>75</P_9A><P_11>1299.75</P_11><P_12>zw</P_12></FaWiersz>
                <Platnosc>
                  <TerminPlatnosci><Termin>2099-10-02</Termin></TerminPlatnosci>
                  <FormaPlatnosci>6</FormaPlatnosci>
                </Platnosc>
              </Fa>
            </Faktura>
            """.formatted(NS);

    private KsefInvoiceImportMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KsefInvoiceImportMapper();
    }

    @Test
    @DisplayName("should map the VAT group member from Podmiot3 to the vatGroupRecipient fields")
    void toInvoice_ShouldMapVatGroupRecipient_WhenBuyerIsVatGroup() throws Exception {
        // given
        InvoiceKsefDto dto = parse(VAT_GROUP_INVOICE);

        // when
        Invoice invoice = mapper.toInvoice(dto);

        // then
        assertThat(invoice.getVatGroupRecipientNip()).isEqualTo("7781028941");
        assertThat(invoice.getVatGroupRecipientName()).isEqualTo("UNIWERSYTET WSB MERITO W POZNANIU");
        assertThat(invoice.getVatGroupRecipientStreet()).isEqualTo("Powstańców Wielkopolskich 5");
        assertThat(invoice.getVatGroupRecipientZip()).isEqualTo("61-895");
        assertThat(invoice.getVatGroupRecipientCity()).isEqualTo("Poznań");
        assertThat(invoice.getBuyerContactEmail()).isEqualTo("dod@poznan.merito.pl");
    }

    @Test
    @DisplayName("should map the remaining invoice data of a group invoice")
    void toInvoice_ShouldMapInvoiceData_WhenBuyerIsVatGroup() throws Exception {
        // given
        InvoiceKsefDto dto = parse(VAT_GROUP_INVOICE);

        // when
        Invoice invoice = mapper.toInvoice(dto);

        // then
        assertThat(invoice.getNumber()).isEqualTo("2026/38");
        assertThat(invoice.getInvoiceDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(invoice.getInvoiceItems()).hasSize(2);
        assertThat(invoice.getInvoiceItems()).allSatisfy(item -> assertThat(item.getVat()).isEqualTo(Vat.VAT_ZW));
        assertThat(invoice.getPaymentMethod()).isEqualTo(PaymentMethod.TRANSFER);
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.TO_PAY);
        assertThat(invoice.getPaymentDate()).isEqualTo(LocalDate.of(2099, 10, 2));
    }

    @Test
    @DisplayName("should leave vatGroupRecipient fields empty when GV is not 1")
    void toInvoice_ShouldNotMapVatGroupRecipient_WhenBuyerIsNotVatGroup() throws Exception {
        // given
        InvoiceKsefDto dto = parse(VAT_GROUP_INVOICE.replace("<GV>1</GV>", "<GV>2</GV>"));

        // when
        Invoice invoice = mapper.toInvoice(dto);

        // then
        assertThat(invoice.getVatGroupRecipientNip()).isNull();
        assertThat(invoice.getVatGroupRecipientName()).isNull();
        assertThat(invoice.getVatGroupRecipientStreet()).isNull();
        assertThat(invoice.getBuyerContactEmail()).isEqualTo("dod@poznan.merito.pl");
    }

    @Test
    @DisplayName("should ignore Podmiot3 with a role other than VAT group member and pick the right one among several")
    void toInvoice_ShouldPickVatGroupMemberRole_WhenSeveralPodmiot3() throws Exception {
        // given: przed członkiem grupy VAT dokładamy Podmiot3 z rolą 2 (odbiorca)
        String otherPodmiot3 = """
                <Podmiot3>
                  <DaneIdentyfikacyjne><NIP>1111111111</NIP><Nazwa>Inny odbiorca</Nazwa></DaneIdentyfikacyjne>
                  <Rola>2</Rola>
                </Podmiot3>
                """;
        InvoiceKsefDto dto = parse(VAT_GROUP_INVOICE.replace("<Podmiot3>", otherPodmiot3 + "<Podmiot3>"));

        // when
        Invoice invoice = mapper.toInvoice(dto);

        // then
        assertThat(dto.getPodmiot3()).hasSize(2);
        assertThat(invoice.getVatGroupRecipientNip()).isEqualTo("7781028941");
    }

    @Test
    @DisplayName("should leave vatGroupRecipient fields empty when GV=1 but there is no Podmiot3")
    void toInvoice_ShouldNotFail_WhenVatGroupWithoutPodmiot3() throws Exception {
        // given
        String xml = VAT_GROUP_INVOICE.replaceAll("(?s)<Podmiot3>.*?</Podmiot3>", "");
        InvoiceKsefDto dto = parse(xml);

        // when
        Invoice invoice = mapper.toInvoice(dto);

        // then
        assertThat(invoice.getVatGroupRecipientNip()).isNull();
    }

    private InvoiceKsefDto parse(String xml) throws Exception {
        InvoiceKsefDto dto = (InvoiceKsefDto) JAXBContext.newInstance(InvoiceKsefDto.class)
                .createUnmarshaller()
                .unmarshal(new StringReader(xml));
        // tak samo jak w KsefService: uzupełnienie brakujących kwot brutto/VAT
        for (Pozycja pozycja : dto.getFakturaCtrl().getPozycje()) {
            pozycja.calculateMissingAmounts();
        }
        return dto;
    }
}
