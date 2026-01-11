package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class KodFormularza {

    @XmlAttribute(name = "kodSystemowy")
    private String kodSystemowy;

    @XmlAttribute(name = "wersjaSchemy")
    private String wersjaSchemy;

    @XmlValue
    private String value; // Wartość FA
}
