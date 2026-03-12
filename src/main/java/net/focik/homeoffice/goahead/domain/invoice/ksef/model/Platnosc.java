package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.*;
import net.focik.homeoffice.utils.LocalDateAdapter;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentType;

import java.time.LocalDate;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Platnosc {

    @XmlElement(name = "Zaplacono")
    private Integer zaplacono; // 1 - zapłacono

    @XmlElement(name = "DataZaplaty")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate dataZaplaty;

    @XmlElement(name = "TerminPlatnosci")
    private List<TerminPlatnosci> terminPlatnosci;

    @XmlElement(name = "FormaPlatnosci")
    private Integer formaPlatnosci;

    @XmlElement(name = "RachunekBankowy")
    private List<RachunekBankowy> rachunekBankowy;
}
