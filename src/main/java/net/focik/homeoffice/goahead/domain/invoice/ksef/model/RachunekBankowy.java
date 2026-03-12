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
public class RachunekBankowy {

    @XmlElement(name = "NrRB")
    private String nrRB;

    @XmlElement(name = "SWIFT")
    private String swift;

    @XmlElement(name = "RachunekWlasnyBanku")
    private Integer rachunekWlasnyBanku;

    @XmlElement(name = "NazwaBanku")
    private String nazwaBanku;

    @XmlElement(name = "OpisRachunku")
    private String opisRachunku;
}
