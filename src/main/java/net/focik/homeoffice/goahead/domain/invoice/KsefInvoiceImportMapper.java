package net.focik.homeoffice.goahead.domain.invoice;

import net.focik.homeoffice.goahead.domain.invoice.ksef.model.FakturaCtrl;
import net.focik.homeoffice.goahead.domain.invoice.ksef.KsefAddressParser;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.Platnosc;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.Podmiot2;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.Pozycja;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.TerminPlatnosci;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Mapuje fakturę pobraną z KSeF (wystawioną poza aplikacją) na {@link Invoice}.
 * Nabywcę (klienta) ustala osobno {@link KsefInvoiceImportService}.
 * Odwrotność {@link KsefInvoiceMapper}.
 */
@Component
class KsefInvoiceImportMapper {

    private static final String CURRENCY = "PLN";
    private static final int VAT_GROUP_FLAG = 1;
    private static final int VAT_GROUP_MEMBER_ROLE = 10;

    Invoice toInvoice(InvoiceKsefDto dto) {
        FakturaCtrl fa = dto.getFakturaCtrl();
        if (fa == null) {
            throw new IllegalArgumentException("Brak sekcji Fa w fakturze");
        }

        Invoice invoice = new Invoice();
        invoice.setNumber(fa.getNumerFaktury());
        invoice.setInvoiceDate(fa.getDataWystawienia());
        invoice.setSellDate(fa.getDataSprzedazy() != null ? fa.getDataSprzedazy() : fa.getDataWystawienia());
        invoice.setInvoiceItems(fa.getPozycje() == null ? List.of() : fa.getPozycje().stream().map(this::toItem).toList());

        mapPayment(invoice, fa.getPlatnosc());
        mapBuyerDetails(invoice, dto);
        return invoice;
    }

    /**
     * Faktura grupowa: nabywcą (Podmiot2) jest grupa VAT (GV=1), a faktycznym odbiorcą - jej członek
     * podany w Podmiot3 z rolą 10 (członek grupy VAT - odbiorca).
     */
    private void mapBuyerDetails(Invoice invoice, InvoiceKsefDto dto) {
        Podmiot2 buyer = dto.getPodmiot2();
        if (buyer == null) {
            return;
        }
        if (buyer.getDaneKontaktowe() != null && StringUtils.hasText(buyer.getDaneKontaktowe().getEmail())) {
            invoice.setBuyerContactEmail(buyer.getDaneKontaktowe().getEmail());
        }
        if (buyer.getGv() == null || buyer.getGv() != VAT_GROUP_FLAG || dto.getPodmiot3() == null) {
            return;
        }

        dto.getPodmiot3().stream()
                .filter(p -> p.getRola() != null && p.getRola() == VAT_GROUP_MEMBER_ROLE)
                .filter(p -> p.getDaneIdentyfikacyjne() != null && StringUtils.hasText(p.getDaneIdentyfikacyjne().getNip()))
                .findFirst()
                .ifPresent(member -> {
                    invoice.setVatGroupRecipientNip(member.getDaneIdentyfikacyjne().getNip());
                    invoice.setVatGroupRecipientName(member.getDaneIdentyfikacyjne().getNazwa());
                    if (member.getAdres() != null) {
                        KsefAddressParser.parse(member.getAdres().getAdresL1(), member.getAdres().getAdresL2())
                                .ifPresent(address -> {
                                    invoice.setVatGroupRecipientStreet(address.street());
                                    invoice.setVatGroupRecipientZip(address.zip());
                                    invoice.setVatGroupRecipientCity(address.city());
                                });
                    }
                });
    }

    private void mapPayment(Invoice invoice, Platnosc platnosc) {
        if (platnosc == null) {
            invoice.setPaymentMethod(PaymentMethod.CASH);
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setPaymentDate(invoice.getInvoiceDate());
            return;
        }

        if (platnosc.getFormaPlatnosci() != null && platnosc.getFormaPlatnosci() == PaymentMethod.CASH.getKsefCode()) {
            invoice.setPaymentMethod(PaymentMethod.CASH);
        } else {
            invoice.setPaymentMethod(PaymentMethod.TRANSFER);
        }

        if (platnosc.getZaplacono() != null && platnosc.getZaplacono() == 1) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setPaymentDate(platnosc.getDataZaplaty() != null ? platnosc.getDataZaplaty() : invoice.getInvoiceDate());
            return;
        }

        invoice.setPaymentStatus(PaymentStatus.TO_PAY);
        List<TerminPlatnosci> terminy = platnosc.getTerminPlatnosci();
        if (terminy != null && !terminy.isEmpty() && terminy.getFirst().getTermin() != null) {
            LocalDate deadline = terminy.getFirst().getTermin();
            if (deadline.isBefore(LocalDate.now())) {
                invoice.setPaymentStatus(PaymentStatus.OVER_DUE);
            }
            invoice.setPaymentDate(deadline);
        } else {
            invoice.setPaymentDate(invoice.getInvoiceDate());
        }
    }

    private InvoiceItem toItem(Pozycja pozycja) {
        InvoiceItem item = new InvoiceItem();
        item.setName(pozycja.getNazwaTowaruUslugi());
        item.setPkwiu(pozycja.getPkwiu());
        item.setUnit(pozycja.getJednostkaMiary());
        item.setVat(mapVat(pozycja.getStawkaPodatku()));

        BigDecimal quantity = pozycja.getIlosc() != null && pozycja.getIlosc() > 0
                ? BigDecimal.valueOf(pozycja.getIlosc())
                : BigDecimal.ONE;
        item.setQuantity(quantity.floatValue());

        // InvoiceItem.amount to cena jednostkowa brutto
        BigDecimal unitGross;
        if (pozycja.getKwotaBrutto() != null) {
            unitGross = BigDecimal.valueOf(pozycja.getKwotaBrutto()).divide(quantity, 4, RoundingMode.HALF_UP);
        } else if (pozycja.getCenaJednostkowaBrutto() != null) {
            unitGross = BigDecimal.valueOf(pozycja.getCenaJednostkowaBrutto());
        } else if (pozycja.getKwotaNetto() != null) {
            // brak VAT w pozycji (np. zw.) - brutto = netto
            unitGross = BigDecimal.valueOf(pozycja.getKwotaNetto()).divide(quantity, 4, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("Pozycja '" + pozycja.getNazwaTowaruUslugi() + "' nie ma kwoty");
        }
        item.setAmount(Money.of(unitGross, CURRENCY));
        return item;
    }

    private Vat mapVat(String ksefValue) {
        if (ksefValue != null) {
            String normalized = ksefValue.trim();
            for (Vat vat : Vat.values()) {
                if (normalized.equals(vat.getKsefValue())) {
                    return vat;
                }
            }
            // "0 KR", "0 WDT", "0 EX" - warianty stawki 0%
            if (normalized.startsWith("0 ")) {
                return Vat.VAT_0;
            }
        }
        throw new IllegalArgumentException("Nieobsługiwana stawka VAT: " + ksefValue);
    }
}
