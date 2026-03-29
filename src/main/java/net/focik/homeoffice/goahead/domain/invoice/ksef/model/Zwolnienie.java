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
public class Zwolnienie {

    @XmlElement(name = "P_19")
    private Integer p19; // 1 - dostawa zwolniona

    @XmlElement(name = "P_19A")
    private String p19A; // Podstawa prawna z ustawy

    @XmlElement(name = "P_19B")
    private String p19B; // Podstawa prawna z dyrektywy UE

    @XmlElement(name = "P_19C")
    private String p19C; // Inna podstawa prawna

    @XmlElement(name = "P_19N")
    private Integer p19N; // 1 - brak zwolnienia
}