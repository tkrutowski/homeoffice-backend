package net.focik.homeoffice.goahead.domain.cost;

import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.*;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class KsefCostMapper {

    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("(\\d{2}-\\d{3})");

    public Cost toCost(InvoiceKsefDto invoiceKsefDto) {
        if (invoiceKsefDto == null) {
            return null;
        }

        Cost cost = new Cost();

        FakturaCtrl fakturaCtrl = invoiceKsefDto.getFakturaCtrl();
        if (fakturaCtrl != null) {
            cost.setNumber(fakturaCtrl.getNumerFaktury());
            cost.setSellDate(fakturaCtrl.getDataSprzedazy() != null ? fakturaCtrl.getDataSprzedazy() : fakturaCtrl.getDataWystawienia());
            cost.setInvoiceDate(fakturaCtrl.getDataWystawienia());

            Platnosc platnosc = fakturaCtrl.getPlatnosc();
            if (platnosc != null) {
                // Forma płatności
                if (platnosc.getFormaPlatnosci() != null) {
                    if (platnosc.getFormaPlatnosci() == PaymentMethod.CASH.getKsefCode()) {
                        cost.setPaymentMethod(PaymentMethod.CASH);
                    } else if (platnosc.getFormaPlatnosci() == PaymentMethod.TRANSFER.getKsefCode()) {
                        cost.setPaymentMethod(PaymentMethod.TRANSFER);
                    } else {
                        cost.setPaymentMethod(PaymentMethod.TRANSFER);
                    }
                }

                // Status płatności i data płatności
                if (platnosc.getZaplacono() != null && platnosc.getZaplacono() == 1) {
                    cost.setPaymentStatus(PaymentStatus.PAID);
                    if (platnosc.getDataZaplaty() != null) {
                        cost.setPaymentDate(platnosc.getDataZaplaty());
                    } else {
                        cost.setPaymentDate(cost.getInvoiceDate());
                    }
                } else {
                    cost.setPaymentStatus(PaymentStatus.TO_PAY);
                    List<TerminPlatnosci> terminPlatnosci = platnosc.getTerminPlatnosci();
                    if (terminPlatnosci != null && !terminPlatnosci.isEmpty()) {
                        LocalDate deadline = terminPlatnosci.getFirst().getTermin();
                        if (deadline != null) {
                            if (deadline.isBefore(LocalDate.now())) {
                                cost.setPaymentStatus(PaymentStatus.OVER_DUE);
                            }
                            cost.setPaymentDate(deadline);
                        }
                    }
                }
            } else {
                cost.setPaymentStatus(PaymentStatus.TO_PAY);
            }

            List<Pozycja> pozycje = fakturaCtrl.getPozycje();
            if (pozycje != null) {
                List<CostItem> costItems = pozycje.stream()
                        .map(this::toCostItem)
                        .collect(Collectors.toList());
                cost.setCostItems(costItems);
            } else {
                cost.setCostItems(Collections.emptyList());
            }
        }

        Podmiot1 podmiot1 = invoiceKsefDto.getPodmiot1();
        if (podmiot1 != null && podmiot1.getDaneIdentyfikacyjne() != null) {
            Supplier supplier = new Supplier();
            supplier.setNip(podmiot1.getDaneIdentyfikacyjne().getNip());
            supplier.setName(podmiot1.getDaneIdentyfikacyjne().getNazwa());

            Adres adres = podmiot1.getAdres();
            if (adres != null) {
                parseAndSetAddress(supplier, adres.getAdresL1(), adres.getAdresL2());
            }

            cost.setSupplier(supplier);
        }

        return cost;
    }

    private CostItem toCostItem(Pozycja pozycja) {
        if (pozycja == null) {
            return null;
        }

        CostItem costItem = new CostItem();
        costItem.setName(pozycja.getNazwaTowaruUslugi());
        costItem.setUnit(pozycja.getJednostkaMiary());
        costItem.setQuantity(pozycja.getIlosc() != null ? pozycja.getIlosc().floatValue() : 0f);

        Vat vat = mapVat(pozycja.getStawkaPodatku());
        costItem.setVat(vat);

        if (pozycja.getKwotaNetto() != null) {
            costItem.setAmountNet(Money.of(pozycja.getKwotaNetto(), "PLN"));
        } else {
            costItem.setAmountNet(Money.of(0, "PLN"));
        }

        if (pozycja.getKwotaBrutto() != null) {
            costItem.setAmountGross(Money.of(pozycja.getKwotaBrutto(), "PLN"));
        } else {
            costItem.setAmountGross(Money.of(0, "PLN"));
        }

        if (pozycja.getKwotaVat() != null) {
            costItem.setAmountVat(Money.of(pozycja.getKwotaVat(), "PLN"));
        } else {
            costItem.setAmountVat(Money.of(0, "PLN"));
        }

        return costItem;
    }

    private Vat mapVat(String ksefVatValue) {
        if (ksefVatValue == null) {
            return Vat.VAT_23; // domyślnie
        }
        for (Vat vat : Vat.values()) {
            if (ksefVatValue.equals(vat.getKsefValue())) {
                return vat;
            }
        }
        return Vat.VAT_23; // domyślnie jeśli nie zmapowano
    }

    private void parseAndSetAddress(Supplier supplier, String adresL1, String adresL2) {
        if (adresL1 == null || adresL1.isBlank()) {
            return;
        }

        String street = adresL1.trim();
        String city = null;
        String zip = null;

        Matcher zipMatcher = ZIP_CODE_PATTERN.matcher(street);
        if (zipMatcher.find()) {
            zip = zipMatcher.group(1);
            int zipStart = zipMatcher.start();
            city = street.substring(zipMatcher.end()).trim();
            if (city.isEmpty()) {
                city = null;
            }
            street = street.substring(0, zipStart).trim();
        } else if (adresL2 != null && !adresL2.isBlank()) {
            Matcher l2Matcher = ZIP_CODE_PATTERN.matcher(adresL2);
            if (l2Matcher.find()) {
                zip = l2Matcher.group(1);
                String remaining = adresL2.substring(l2Matcher.end()).trim();
                if (!remaining.isEmpty()) {
                    city = remaining;
                }
            }
        }

        street = removeStreetPrefix(street);
        supplier.setAddress(city, street, zip);
    }

    private String removeStreetPrefix(String street) {
        if (street == null || street.isBlank()) {
            return street;
        }
        return street.replaceAll("(?i)^(ul\\.?|ulica)\\s+", "").trim();
    }
}