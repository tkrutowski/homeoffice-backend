package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@XmlRootElement(name = "Faktura", namespace = "http://crd.gov.pl/wzor/2025/06/25/13775/")
public class InvoiceKsefDto {

    @XmlElement(name = "Naglowek")
    private Naglowek naglowek;

    @XmlElement(name = "Podmiot1")
    private Podmiot1 podmiot1; // Sprzedawca

    @XmlElement(name = "Podmiot2")
    private Podmiot2 podmiot2; // Nabywca

    @XmlElement(name = "Fa")
    private FakturaCtrl fakturaCtrl;

    @XmlElement(name = "FaWiersz")
    private List<Pozycja> pozycje;
}
