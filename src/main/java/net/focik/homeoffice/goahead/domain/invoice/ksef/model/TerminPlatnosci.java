package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.*;
import net.focik.homeoffice.utils.LocalDateAdapter;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class TerminPlatnosci {

    @XmlElement(name = "Termin")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate termin;

    // TerminOpis pomijam dla uproszczenia, chyba że jest potrzebny
}
