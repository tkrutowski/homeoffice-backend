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
public class Adnotacje {

    @XmlElement(name = "P_16")
    private Integer p16; // Metoda kasowa: 1-tak, 2-nie

    @XmlElement(name = "P_17")
    private Integer p17; // Samofakturowanie: 1-tak, 2-nie

    @XmlElement(name = "P_18")
    private Integer p18; // Odwrotne obciążenie: 1-tak, 2-nie

    @XmlElement(name = "P_18A")
    private Integer p18A; // Mechanizm podzielonej płatności: 1-tak, 2-nie

    @XmlElement(name = "Zwolnienie")
    private Zwolnienie zwolnienie;

    @XmlElement(name = "NoweSrodkiTransportu")
    private NoweSrodkiTransportu noweSrodkiTransportu;

    @XmlElement(name = "P_23")
    private Integer p23; // Procedura uproszczona: 1-tak, 2-nie

    @XmlElement(name = "PMarzy")
    private PMarzy pMarzy;
}