package net.focik.homeoffice.goahead.domain.invoice;

import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KsefInvoiceMapperTest {

    private KsefInvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KsefInvoiceMapper();
    }

    @Test
    void toKsefFaktura_ShouldReturnNull_WhenInvoiceIsNull() {
        // when
        InvoiceKsefDto result = mapper.toKsefFaktura(null, new Company());

        // then
        assertThat(result).isNull();
    }

    @Test
    void toKsefFaktura_ShouldMapInvoiceToKsefDto_WithMultipleVatRates() {
        // given
        Company company = Company.builder()
                .fullName("GoAhead Sp. z o.o.")
                .nip("123-456-78-90")
                .address(Address.builder()
                        .city("Warszawa")
                        .street("Testowa 1")
                        .zip("00-001")
                        .build())
                .build();

        Customer customer = Customer.builder()
                .customerType(CustomerType.COMPANY)
                .name("Jan Kowalski IT")
                .nip("0987654321")
                .address(Address.builder()
                        .city("Kraków")
                        .street("Długa 2")
                        .zip("30-001")
                        .build())
                .build();

        List<InvoiceItem> items = List.of(
                InvoiceItem.builder()
                        .name("Usługi IT")
                        .quantity(1f)
                        .unit("szt")
                        .amount(Money.of(123.00, "PLN")) // Brutto 123 PLN, Netto 100 PLN, VAT 23 PLN
                        .vat(Vat.VAT_23)
                        .build(),
                InvoiceItem.builder()
                        .name("Myszka")
                        .quantity(2f)
                        .unit("szt")
                        .amount(Money.of(21.60, "PLN")) // Brutto 43.20 PLN, Netto 40 PLN, VAT 3.20 PLN (8%)
                        .vat(Vat.VAT_8)
                        .build(),
                InvoiceItem.builder()
                        .name("Szkolenie")
                        .quantity(1f)
                        .unit("szt")
                        .amount(Money.of(200.00, "PLN")) // Brutto 200 PLN, Netto 200 PLN, ZW
                        .vat(Vat.VAT_ZW)
                        .build()
        );

        Invoice invoice = Invoice.builder()
                .number("FA/1/2023")
                .invoiceDate(LocalDate.of(2023, 10, 1))
                .sellDate(LocalDate.of(2023, 9, 30))
                .paymentMethod(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.TO_PAY)
                .paymentDate(LocalDate.of(2023, 10, 15))
                .customer(customer)
                .invoiceItems(items)
                .otherInfo("Inne::Testowa uwaga")
                .build();

        // when
        InvoiceKsefDto result = mapper.toKsefFaktura(invoice, company);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNaglowek()).isNotNull();
        assertThat(result.getNaglowek().getKodFormularza().getWersjaSchemy()).isEqualTo("1-0E");

        // Podmiot 1 (Sprzedawca)
        assertThat(result.getPodmiot1().getDaneIdentyfikacyjne().getNip()).isEqualTo("1234567890");
        assertThat(result.getPodmiot1().getDaneIdentyfikacyjne().getNazwa()).isEqualTo("GoAhead Sp. z o.o.");
        assertThat(result.getPodmiot1().getAdres().getAdresL1()).contains("Testowa 1").contains("Warszawa");

        // Podmiot 2 (Nabywca)
        assertThat(result.getPodmiot2().getDaneIdentyfikacyjne().getNip()).isEqualTo("0987654321");
        assertThat(result.getPodmiot2().getDaneIdentyfikacyjne().getNazwa()).isEqualTo("Jan Kowalski IT");
        assertThat(result.getPodmiot2().getAdres().getAdresL1()).contains("Długa 2").contains("Kraków");

        // Faktura Ctrl
        assertThat(result.getFakturaCtrl().getNumerFaktury()).isEqualTo("FA/1/2023");
        assertThat(result.getFakturaCtrl().getDataWystawienia()).isEqualTo(LocalDate.of(2023, 10, 1));
        assertThat(result.getFakturaCtrl().getDataSprzedazy()).isEqualTo(LocalDate.of(2023, 9, 30));

        // Obliczenia VAT z mapowania
        // P_13_1 / P_14_1 (23%)
        assertThat(result.getFakturaCtrl().getP13_1()).isEqualTo(100.00); // netto
        assertThat(result.getFakturaCtrl().getP14_1()).isEqualTo(23.00);  // vat

        // P_13_2 / P_14_2 (8%) - z 2 sztuk po 21.60 brutto (43.20 brutto / 1.08 = 40.00 netto, vat 3.20)
        assertThat(result.getFakturaCtrl().getP13_2()).isEqualTo(40.00); // netto
        assertThat(result.getFakturaCtrl().getP14_2()).isEqualTo(3.20);  // vat

        // P_13_7 (ZW)
        assertThat(result.getFakturaCtrl().getP13_7()).isEqualTo(200.00); // netto dla zw.

        // Suma brutto P_15 = 123 + 43.20 + 200 = 366.20
        assertThat(result.getFakturaCtrl().getP15()).isEqualTo(366.20);

        // Dodatkowy opis
        assertThat(result.getFakturaCtrl().getDodatkowyOpis()).hasSize(1);
        assertThat(result.getFakturaCtrl().getDodatkowyOpis().getFirst().getKlucz()).isEqualTo("Inne");
        assertThat(result.getFakturaCtrl().getDodatkowyOpis().getFirst().getWartosc()).isEqualTo("Testowa uwaga");

        // Pozycje
        assertThat(result.getFakturaCtrl().getPozycje()).hasSize(3);
        assertThat(result.getFakturaCtrl().getPozycje().get(0).getNazwaTowaruUslugi()).isEqualTo("Usługi IT");
        assertThat(result.getFakturaCtrl().getPozycje().get(0).getLpFa()).isEqualTo(1);
        assertThat(result.getFakturaCtrl().getPozycje().get(1).getNazwaTowaruUslugi()).isEqualTo("Myszka");
        assertThat(result.getFakturaCtrl().getPozycje().get(1).getLpFa()).isEqualTo(2);

        // Płatność
        assertThat(result.getFakturaCtrl().getPlatnosc().getFormaPlatnosci()).isEqualTo(6); // Transfer
        assertThat(result.getFakturaCtrl().getPlatnosc().getZaplacono()).isNull();
        assertThat(result.getFakturaCtrl().getPlatnosc().getTerminPlatnosci().getFirst().getTermin()).isEqualTo(LocalDate.of(2023, 10, 15));
        
        // Adnotacje zwolnienie z VAT
        assertThat(result.getFakturaCtrl().getAdnotacje().getZwolnienie().getP19()).isEqualTo(1);
        assertThat(result.getFakturaCtrl().getAdnotacje().getZwolnienie().getP19A()).isNotNull();
    }

    @Test
    void toKsefFaktura_ShouldMapPaidInvoiceCorrectly() {
        // given
        Company company = Company.builder()
                .fullName("GoAhead Sp. z o.o.")
                .nip("123-456-78-90")
                .address(Address.builder()
                        .city("Warszawa")
                        .street("Testowa 1")
                        .zip("00-001")
                        .build())
                .build();
                
        Customer customer = Customer.builder()
                .customerType(CustomerType.CUSTOMER)
                .name("Kowalski")
                .firstName("Jan")
                .address(Address.builder()
                        .city("Kraków")
                        .street("Długa 2")
                        .zip("30-001")
                        .build())
                .build();

        Invoice invoice = Invoice.builder()
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PAID)
                .sellDate(LocalDate.of(2023, 1, 1))
                .customer(customer)
                .invoiceItems(List.of(
                        InvoiceItem.builder()
                                .quantity(1f)
                                .amount(Money.of(100.00, "PLN"))
                                .vat(Vat.VAT_0)
                                .build()
                ))
                .build();

        // when
        InvoiceKsefDto result = mapper.toKsefFaktura(invoice, company);

        // then
        assertThat(result.getFakturaCtrl().getPlatnosc().getZaplacono()).isEqualTo(1);
        assertThat(result.getFakturaCtrl().getPlatnosc().getDataZaplaty()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(result.getFakturaCtrl().getPlatnosc().getFormaPlatnosci()).isEqualTo(1); // CASH
    }
}
