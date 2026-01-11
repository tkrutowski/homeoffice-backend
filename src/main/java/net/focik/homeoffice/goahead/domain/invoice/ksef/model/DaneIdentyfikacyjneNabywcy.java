package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DaneIdentyfikacyjneNabywcy {

    /**
     * Pole NIP. Wypełniane, gdy nabywca jest identyfikowany polskim numerem NIP.
     * JAXB pominie ten element w XML, jeśli wartość będzie null.
     */
    @XmlElement(name = "NIP")
    private String nip;

    /**
     * Pole BrakID. Wartość "1" oznacza, że podmiot nie posiada identyfikatora.
     * JAXB pominie ten element w XML, jeśli wartość będzie null.
     */
    @XmlElement(name = "BrakID")
    private Integer brakID;

    /**
     * Nazwa nabywcy. Jest to pole opcjonalne w schemacie, ale w praktyce zawsze występuje.
     */
    @XmlElement(name = "Nazwa")
    private String nazwa;

    // Celowo pomijam inne opcje z <choice> (NrVatUE, NrID) oraz pola opcjonalne
    // z Podmiot2 (NrEORI, AdresKoresp, etc.), zgodnie z ustaleniami.
}
