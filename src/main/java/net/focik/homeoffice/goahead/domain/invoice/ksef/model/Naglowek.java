package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.*;
import net.focik.homeoffice.utils.OffsetDateTimeAdapter;

import java.time.OffsetDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Naglowek {

    @XmlElement(name = "KodFormularza")
    private KodFormularza kodFormularza;

    @XmlElement(name = "WariantFormularza")
    private Integer wariantFormularza;

    @XmlElement(name = "DataWytworzeniaFa")
    @XmlJavaTypeAdapter(OffsetDateTimeAdapter.class)
    private OffsetDateTime dataWytworzeniaFa;

    @XmlElement(name = "SystemInfo")
    private String systemInfo;
}
