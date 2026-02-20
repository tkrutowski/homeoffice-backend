package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;

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

    @XmlElement(name = "P_8B")
    private Double ilosc;

    @XmlElement(name = "P_8A")
    private String jednostkaMiary;

    @XmlElement(name = "P_9A")
    private Double cenaJednostkowaNetto;

    @XmlElement(name = "P_12")
    private Integer stawkaPodatku; // np. 23

    @XmlElement(name = "P_11")
    private Double kwotaNetto;

    @XmlElement(name = "P_11Vat")
    private Double kwotaVat;

    @XmlElement(name = "P_11A")
    private Double kwotaBrutto;
}
