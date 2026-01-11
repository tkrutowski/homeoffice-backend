package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Podmiot1 {

    @XmlElement(name = "DaneIdentyfikacyjne")
    private DaneIdentyfikacyjne daneIdentyfikacyjne;

    @XmlElement(name = "Adres")
    private Adres adres;
}
