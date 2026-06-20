package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import javax.money.Monetary;

@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Pozycja {

    @XmlElement(name = "NrWierszaFa")
    private Integer lpFa; // Liczba porządkowa

    @XmlElement(name = "P_7")
    private String nazwaTowaruUslugi;

    @XmlElement(name = "PKWiU")
    private String pkwiu; // Opcjonalnie

    @XmlElement(name = "P_8A")
    private String jednostkaMiary;

    @XmlElement(name = "P_8B")
    private Double ilosc;

    @XmlElement(name = "P_9A")
    private Double cenaJednostkowaNetto;

    @XmlElement(name = "P_11")
    private Double kwotaNetto;

    @XmlElement(name = "P_11A")
    private Double kwotaBrutto;

    @XmlElement(name = "P_11Vat")
    private Double kwotaVat;

    @XmlElement(name = "P_12")
    private String stawkaPodatku; // np. 23

    public void calculateMissingAmounts() {
        if (stawkaPodatku == null) {
            return;
        }

        try {
            double vatRate = Double.parseDouble(stawkaPodatku) / 100.0;
            var pln = Monetary.getCurrency("PLN");

            // Oblicz brakujące kwotaNetto
            if (kwotaNetto == null) {
                if (kwotaVat != null) {
                    // kwotaNetto = kwotaVat / (stawka / 100)
                    Money vat = Money.of(kwotaVat, pln);
                    Money netto = vat.divide(vatRate);
                    this.kwotaNetto = netto.getNumber().doubleValue();
                } else if (kwotaBrutto != null) {
                    // kwotaNetto = kwotaBrutto / (1 + stawka / 100)
                    Money brutto = Money.of(kwotaBrutto, pln);
                    Money netto = brutto.divide(1 + vatRate);
                    this.kwotaNetto = netto.getNumber().doubleValue();
                }
            }

            // Jeśli nadal nie mamy kwotaNetto, przerwij
            if (kwotaNetto == null) {
                return;
            }

            // Oblicz brakujące kwotaVat
            if (kwotaVat == null) {
                Money netto = Money.of(kwotaNetto, pln);
                Money vat = netto.multiply(vatRate);
                this.kwotaVat = vat.getNumber().doubleValue();
            }

            // Oblicz brakujące kwotaBrutto
            if (kwotaBrutto == null) {
                Money netto = Money.of(kwotaNetto, pln);
                Money vat = Money.of(kwotaVat, pln);
                Money brutto = netto.add(vat);
                this.kwotaBrutto = brutto.getNumber().doubleValue();
            }
        } catch (NumberFormatException e) {
            log.error("Failed to parse VAT rate: {}", e.getMessage());
            // stawka VAT nie może być sparsowana, pozostaw pola puste
        }
    }
}
