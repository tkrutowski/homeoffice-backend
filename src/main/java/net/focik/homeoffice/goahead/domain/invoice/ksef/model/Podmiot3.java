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
public class Podmiot3 {

    @XmlElement(name = "DaneIdentyfikacyjne")
    private DaneIdentyfikacyjnePodmiotu3 daneIdentyfikacyjne;

    @XmlElement(name = "Adres")
    private Adres adres;

    @XmlElement(name = "DaneKontaktowe")
    private DaneKontaktowe daneKontaktowe;

    @XmlElement(name = "Rola")
    private Integer rola;
}
