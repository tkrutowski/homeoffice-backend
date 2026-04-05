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
public class TKluczWartosc {

    @XmlElement(name = "NrWiersza")
    private Integer nrWiersza;

    @XmlElement(name = "Klucz")
    private String klucz;

    @XmlElement(name = "Wartosc")
    private String wartosc;
}
