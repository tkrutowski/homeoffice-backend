package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.*;
import net.focik.homeoffice.utils.LocalDateAdapter;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class FakturaCtrl {

    @XmlElement(name = "KodWaluty")
    private String kodWaluty;

    @XmlElement(name = "P_1")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate dataWystawienia;

    @XmlElement(name = "P_2")
    private String numerFaktury;

    @XmlElement(name = "P_6")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate dataSprzedazy;

    // Pola dla stawki 23% (lub podstawowej)
    @XmlElement(name = "P_13_1")
    private Double p13_1; // Suma netto dla stawki 23%
    @XmlElement(name = "P_14_1")
    private Double p14_1; // Kwota podatku dla stawki 23%

    // Pola dla stawki 8%
    @XmlElement(name = "P_13_2")
    private Double p13_2; // Suma netto dla stawki 8%
    @XmlElement(name = "P_14_2")
    private Double p14_2; // Kwota podatku dla stawki 8%

    // Pola dla stawki 5%
    @XmlElement(name = "P_13_3")
    private Double p13_3; // Suma netto dla stawki 5%
    @XmlElement(name = "P_14_3")
    private Double p14_3; // Kwota podatku dla stawki 5%

    // Pola dla stawki 0%
    @XmlElement(name = "P_13_6_1")
    private Double p13_6_1; // Suma wartości dla stawki 0%
    @XmlElement(name = "P_14_6_2")
    private Double p14_6_2; // Suma wartości dla stawki 0% - to samo co wyżej

    // Pole dla stawki "zw" (zwolnione)
    @XmlElement(name = "P_13_7")
    private Double p13_7; // Suma netto dla stawki "zw"

    // Kwota należności ogółem (brutto)
    @XmlElement(name = "P_15")
    private Double p15;

    //TODO dodać typ RodzajFaktury, dodać obsługę korekt
    @XmlElement(name = "RodzajFaktury")
    private String rodzajFaktury; // np. VAT
}