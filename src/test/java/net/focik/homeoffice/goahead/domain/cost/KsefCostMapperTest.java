package net.focik.homeoffice.goahead.domain.cost;

import net.focik.homeoffice.goahead.domain.invoice.ksef.model.*;
import net.focik.homeoffice.utils.share.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KsefCostMapperTest {

    private KsefCostMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KsefCostMapper();
    }

    @Test
    void toCost_ShouldParseAddressCorrectly_WhenAdresL1IsStreetAndAdresL2IsZipAndCity() {
        // given
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress("Testowa 1", "00-001 Warszawa");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier()).isNotNull();
        assertThat(cost.getSupplier().getAddress()).isNotNull();
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 1");
        assertThat(cost.getSupplier().getAddress().getZip()).isEqualTo("00-001");
        assertThat(cost.getSupplier().getAddress().getCity()).isEqualTo("Warszawa");
    }

    @Test
    void toCost_ShouldParseAddressCorrectly_WhenAdresL2ContainsMultipleWords() {
        // given
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress("Długa 2", "30-001 Kraków Wielkie Miasto");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Długa 2");
        assertThat(cost.getSupplier().getAddress().getZip()).isEqualTo("30-001");
        assertThat(cost.getSupplier().getAddress().getCity()).isEqualTo("Kraków Wielkie Miasto");
    }

    @Test
    void toCost_ShouldSetStreetOnly_WhenAdresL2DoesNotContainZipCode() {
        // given
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress("Testowa 5", "");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 5");
        assertThat(cost.getSupplier().getAddress().getZip()).isNull();
        assertThat(cost.getSupplier().getAddress().getCity()).isNull();
    }

    @Test
    void toCost_ShouldNotSetAddress_WhenAdresL1IsNull() {
        // given
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress(null, "00-001 Warszawa");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier()).isNotNull();
        assertThat(cost.getSupplier().getAddress()).isNull();
    }

    @Test
    void toCost_ShouldNotSetAddress_WhenAdresL1IsEmpty() {
        // given
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress("   ", "00-001 Warszawa");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier().getAddress()).isNull();
    }

    @Test
    void toCost_ShouldParseFullAddressFromAdresL1_WithZipAndCity() {
        // given: cały adres w adresL1, bez adresL2
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress("Ul. Żelazna 51/53 00-841 Warszawa", "");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier().getAddress()).isNotNull();
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Żelazna 51/53");
        assertThat(cost.getSupplier().getAddress().getZip()).isEqualTo("00-841");
        assertThat(cost.getSupplier().getAddress().getCity()).isEqualTo("Warszawa");
    }

    @Test
    void toCost_ShouldParseFullAddressAndRemoveUlPrefix() {
        // given: adres z prefiksem "ul."
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress("ul. Szyperska, 13d/32 61-754 Poznań", "");

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Szyperska, 13d/32");
        assertThat(cost.getSupplier().getAddress().getZip()).isEqualTo("61-754");
        assertThat(cost.getSupplier().getAddress().getCity()).isEqualTo("Poznań");
    }

    @Test
    void toCost_ShouldRemoveStreetPrefixes() {
        // given: różne warianty prefiksu "ul."
        InvoiceKsefDto invoiceKsefDto1 = buildInvoiceKsefDtoWithAddress("ul. Testowa 1 00-001 Warszawa", "");
        InvoiceKsefDto invoiceKsefDto2 = buildInvoiceKsefDtoWithAddress("UL. Testowa 2 00-002 Warszawa", "");
        InvoiceKsefDto invoiceKsefDto3 = buildInvoiceKsefDtoWithAddress("ulica Testowa 3 00-003 Warszawa", "");

        // when
        Cost cost1 = mapper.toCost(invoiceKsefDto1);
        Cost cost2 = mapper.toCost(invoiceKsefDto2);
        Cost cost3 = mapper.toCost(invoiceKsefDto3);

        // then
        assertThat(cost1.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 1");
        assertThat(cost2.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 2");
        assertThat(cost3.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 3");
    }

    @Test
    void toCost_ShouldPreferAdresL1WithZipOverAdresL2() {
        // given: kod pocztowy zarówno w adresL1 jak i adresL2
        InvoiceKsefDto invoiceKsefDto = buildInvoiceKsefDtoWithAddress(
                "ul. Testowa 1 00-001 Warszawa",
                "30-001 Kraków"
        );

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then: powinna być uwzględniona wartość z adresL1
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 1");
        assertThat(cost.getSupplier().getAddress().getZip()).isEqualTo("00-001");
        assertThat(cost.getSupplier().getAddress().getCity()).isEqualTo("Warszawa");
    }

    @Test
    void toCost_ShouldMapCostWithMultipleItems() {
        // given
        InvoiceKsefDto invoiceKsefDto = InvoiceKsefDto.builder()
                .naglowek(buildNaglowek())
                .podmiot1(buildPodmiot1("Testowa 1", "00-001 Warszawa"))
                .podmiot2(buildPodmiot2())
                .fakturaCtrl(buildFakturaCtrl())
                .build();

        // when
        Cost cost = mapper.toCost(invoiceKsefDto);

        // then
        assertThat(cost).isNotNull();
        assertThat(cost.getNumber()).isEqualTo("FA/1/2023");
        assertThat(cost.getSupplier().getNip()).isEqualTo("1234567890");
        assertThat(cost.getSupplier().getAddress().getStreet()).isEqualTo("Testowa 1");
        assertThat(cost.getSupplier().getAddress().getCity()).isEqualTo("Warszawa");
        assertThat(cost.getCostItems()).hasSize(1);
    }

    private InvoiceKsefDto buildInvoiceKsefDtoWithAddress(String adresL1, String adresL2) {
        return InvoiceKsefDto.builder()
                .naglowek(buildNaglowek())
                .podmiot1(buildPodmiot1(adresL1, adresL2))
                .podmiot2(buildPodmiot2())
                .fakturaCtrl(buildFakturaCtrl())
                .build();
    }

    private Naglowek buildNaglowek() {
        return Naglowek.builder()
                .kodFormularza(KodFormularza.builder()
                        .kodSystemowy("FA (3)")
                        .wersjaSchemy("1-0E")
                        .value("FA")
                        .build())
                .wariantFormularza(3)
                .build();
    }

    private Podmiot1 buildPodmiot1(String adresL1, String adresL2) {
        Adres adres = Adres.builder()
                .kodKraju("PL")
                .adresL1(adresL1)
                .adresL2(adresL2)
                .build();

        return Podmiot1.builder()
                .daneIdentyfikacyjne(DaneIdentyfikacyjne.builder()
                        .nip("1234567890")
                        .nazwa("Test Supplier")
                        .build())
                .adres(adres)
                .build();
    }

    private Podmiot2 buildPodmiot2() {
        return Podmiot2.builder()
                .daneIdentyfikacyjne(DaneIdentyfikacyjneNabywcy.builder()
                        .nip("0987654321")
                        .nazwa("Test Customer")
                        .build())
                .adres(Adres.builder()
                        .kodKraju("PL")
                        .adresL1("Nabywca 1")
                        .adresL2("00-002 Warszawa")
                        .build())
                .build();
    }

    private FakturaCtrl buildFakturaCtrl() {
        Pozycja pozycja = Pozycja.builder()
                .lpFa(1)
                .nazwaTowaruUslugi("Test Item")
                .jednostkaMiary("szt")
                .ilosc(1.0)
                .kwotaNetto(100.0)
                .kwotaVat(23.0)
                .kwotaBrutto(123.0)
                .stawkaPodatku("23")
                .build();

        Platnosc platnosc = Platnosc.builder()
                .formaPlatnosci(PaymentMethod.TRANSFER.getKsefCode())
                .build();

        return FakturaCtrl.builder()
                .kodWaluty("PLN")
                .numerFaktury("FA/1/2023")
                .dataWystawienia(LocalDate.of(2023, 10, 1))
                .dataSprzedazy(LocalDate.of(2023, 9, 30))
                .rodzajFaktury("VAT")
                .pozycje(List.of(pozycja))
                .platnosc(platnosc)
                .build();
    }
}
