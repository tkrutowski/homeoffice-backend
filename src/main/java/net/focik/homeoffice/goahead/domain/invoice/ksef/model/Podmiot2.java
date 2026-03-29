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
public class Podmiot2 {

    @XmlElement(name = "DaneIdentyfikacyjne")
    private DaneIdentyfikacyjneNabywcy daneIdentyfikacyjne;

    @XmlElement(name = "Adres")
    private Adres adres;

    /**
     * Znacznik jednostki podrzędnej JST.
     * Wartość "2" oznacza, że faktura nie dotyczy jednostki podrzędnej JST.
     */
    @XmlElement(name = "JST")
    private Integer jst;

    /**
     * Znacznik członka grupy VAT.
     *  Wartość "2" oznacza, że faktura nie dotyczy członka grupy VAT
     */
    @XmlElement(name = "GV")
    private Integer gv;
}
