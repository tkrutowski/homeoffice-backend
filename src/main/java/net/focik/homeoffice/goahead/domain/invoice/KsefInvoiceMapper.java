package net.focik.homeoffice.goahead.domain.invoice;

import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.*;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class KsefInvoiceMapper {
    public InvoiceKsefDto toKsefFaktura(Invoice invoice, Company goAhead) {
        if (invoice == null) {
            return null;
        }

        List<InvoiceItem> items = invoice.getInvoiceItems();

        return InvoiceKsefDto.builder()
                .naglowek(buildNaglowek())
                .podmiot1(buildPodmiot1(goAhead))
                .podmiot2(buildPodmiot2(invoice.getCustomer()))
                .fakturaCtrl(buildFakturaCtrl(invoice))
                .pozycje(IntStream.range(0, items.size())
                        .mapToObj(i -> toPozycja(items.get(i), i + 1))
                        .collect(Collectors.toList()))
                .platnosc(buildPlatnosc(invoice))
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
                .dataWytworzeniaFa(OffsetDateTime.now())
//                .systemInfo("home-office")
                .build();
    }

    private Podmiot1 buildPodmiot1(Company goAhead) {
        //TODO dane firmy sprzedającej powinny być pobierane z konfiguracji
        DaneIdentyfikacyjne dane = DaneIdentyfikacyjne.builder()
                .nip(goAhead.getNipWithoutDashes())
                .nazwa(goAhead.getFullName())
                .build();

        Adres adres = Adres.builder()
                .kodKraju("PL")
                .adresL1(goAhead.getAddress().toString())
                .build();

        return Podmiot1.builder()
                .daneIdentyfikacyjne(dane)
                .adres(adres)
                .build();
    }

    private Podmiot2 buildPodmiot2(Customer customer) {
        DaneIdentyfikacyjneNabywcy.DaneIdentyfikacyjneNabywcyBuilder daneBuilder = DaneIdentyfikacyjneNabywcy.builder()
                .nazwa(customer.getFullName())
                .nip(customer.getNipWithoutDashes());

        Adres adres = Adres.builder()
                .kodKraju("PL")
                .adresL1(customer.getAddress().toString())
                .build();

        return Podmiot2.builder()
                .daneIdentyfikacyjne(daneBuilder.build())
                .adres(adres)
                .jst(2) // Ustawienie wartości "2" dla pola JST
                .gv(2) // Ustawienie wartości "2" dla pola GV
                .build();
    }

    private FakturaCtrl buildFakturaCtrl(Invoice invoice) {
        FakturaCtrl.FakturaCtrlBuilder builder = FakturaCtrl.builder()
                .kodWaluty("PLN")
                .dataWystawienia(invoice.getInvoiceDate())
                .dataSprzedazy(invoice.getSellDate())
                .numerFaktury(invoice.getInvoiceNumber())
                .rodzajFaktury("VAT");

        // 1. Grupujemy pozycje faktury po stawce VAT
        Map<Vat, List<InvoiceItem>> itemsByVatRate = invoice.getInvoiceItems().stream()
                .collect(Collectors.groupingBy(InvoiceItem::getVat));

        BigDecimal totalGrossAmount = BigDecimal.ZERO;

        // 2. Iterujemy po każdej grupie i obliczamy sumy
        for (Map.Entry<Vat, List<InvoiceItem>> entry : itemsByVatRate.entrySet()) {
            Vat vat = entry.getKey();
            List<InvoiceItem> items = entry.getValue();

            // Obliczamy sumę brutto dla danej grupy
            Money sumOfGrossForVat = items.stream()
                    .map(item -> item.getAmount().multiply(item.getQuantity()))
                    .reduce(Money.of(0, "PLN"), Money::add);

            // Obliczamy sumę netto i VAT z sumy brutto
            BigDecimal sumOfGrossDecimal = sumOfGrossForVat.getNumber().numberValue(BigDecimal.class);
            BigDecimal vatRateDecimal = BigDecimal.valueOf(vat.getMultiplier());
            BigDecimal sumOfNetDecimal = sumOfGrossDecimal.divide(vatRateDecimal, 2, RoundingMode.HALF_UP);
            BigDecimal sumOfVatDecimal = sumOfGrossDecimal.subtract(sumOfNetDecimal);

            totalGrossAmount = totalGrossAmount.add(sumOfGrossDecimal);

            // 3. Ustawiamy odpowiednie pola w builderze na podstawie stawki VAT
            switch (vat) {
                case VAT_23:
                    builder.p13_1(sumOfNetDecimal.doubleValue());
                    builder.p14_1(sumOfVatDecimal.doubleValue());
                    break;
                case VAT_8:
                    builder.p13_2(sumOfNetDecimal.doubleValue());
                    builder.p14_2(sumOfVatDecimal.doubleValue());
                    break;
                case VAT_5:
                    builder.p13_3(sumOfNetDecimal.doubleValue());
                    builder.p14_3(sumOfVatDecimal.doubleValue());
                    break;
                case VAT_0:
                    builder.p13_6_1(sumOfNetDecimal.doubleValue());
                    builder.p14_6_2(sumOfVatDecimal.doubleValue());
                    break;
                case VAT_ZW:
                    builder.p13_7(sumOfNetDecimal.doubleValue());
                    break;
            }
        }

        builder.p15(totalGrossAmount.doubleValue());

        return builder.build();
    }


    private Pozycja toPozycja(InvoiceItem item, int lp) {
        Money itemGrossAmount = item.getAmount().multiply(item.getQuantity());
        BigDecimal itemGrossDecimal = itemGrossAmount.getNumber().numberValue(BigDecimal.class);
        BigDecimal vatRateDecimal = BigDecimal.valueOf(item.getVat().getMultiplier());
        BigDecimal itemNetDecimal = itemGrossDecimal.divide(vatRateDecimal, 2, RoundingMode.HALF_UP);
        BigDecimal itemVatDecimal = itemGrossDecimal.subtract(itemNetDecimal);

        return Pozycja.builder()
                .lpFa(lp) // Używamy przekazanego numeru porządkowego
                .nazwaTowaruUslugi(item.getName())
                .pkwiu(item.getPkwiu())
                .ilosc((double) item.getQuantity())
                .jednostkaMiary(item.getUnit())
                .cenaJednostkowaNetto(item.getAmount().getNumber().numberValue(BigDecimal.class)
                        .divide(vatRateDecimal, 2, RoundingMode.HALF_UP).doubleValue())
                .stawkaPodatku(item.getVat().getNumberValue())
                .kwotaNetto(itemNetDecimal.doubleValue())
                .kwotaVat(itemVatDecimal.doubleValue())
                .kwotaBrutto(itemGrossDecimal.doubleValue())
                .build();
    }

    private Platnosc buildPlatnosc(Invoice invoice) {
        Platnosc.PlatnoscBuilder builder = Platnosc.builder()
                .formaPlatnosci(invoice.getPaymentMethod().getKsefCode());

        if (PaymentStatus.PAID.equals(invoice.getPaymentStatus())) {
            builder.zaplacono(1);
            // Jeśli zapłacono, data zapłaty to data sprzedaży (lub inna, jeśli dostępna)
            // W modelu Invoice nie ma pola "data faktycznej zapłaty", więc używam daty sprzedaży jako przybliżenia
            // lub daty płatności jeśli jest ustawiona na przeszłość/teraźniejszość
            builder.dataZaplaty(invoice.getPaymentDate() != null ? invoice.getPaymentDate() : invoice.getSellDate());
        } else {
            // Jeśli nie zapłacono, ustawiamy termin płatności
            if (invoice.getPaymentDate() != null) {
                builder.terminPlatnosci(Collections.singletonList(
                        TerminPlatnosci.builder()
                                .termin(invoice.getPaymentDate())
                                .build()
                ));
            }
        }

        // Dodanie numeru konta dla przelewu
        if (PaymentMethod.TRANSFER.equals(invoice.getPaymentMethod())) {
            //TODO numer konta z konfiguracji
            builder.rachunekBankowy(Collections.singletonList(
                    RachunekBankowy.builder()
                            .nrRB("12345678901234567890123456") // Przykładowy numer
                            .build()
            ));
        }

        return builder.build();
    }
}
