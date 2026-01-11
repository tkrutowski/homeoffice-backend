package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DaneIdentyfikacyjne {

    @XmlElement(name = "NIP")
    private String nip;

    @XmlElement(name = "Nazwa")
    private String nazwa;
}
