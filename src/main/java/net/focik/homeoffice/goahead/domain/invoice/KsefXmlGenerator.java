package net.focik.homeoffice.goahead.domain.invoice;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import org.springframework.stereotype.Service;

import java.io.StringWriter;

@Service
@RequiredArgsConstructor
public class KsefXmlGenerator {

    private final KsefInvoiceMapper ksefInvoiceMapper;

    public String generateInvoiceXml(Invoice invoice) throws JAXBException {
        // 1. Mapowanie obiektu domenowego na obiekt DTO (JAXB)
        InvoiceKsefDto ksefFaktura = ksefInvoiceMapper.toKsefFaktura(invoice);

        // 2. Utworzenie kontekstu JAXB dla wygenerowanych klas
        JAXBContext context = JAXBContext.newInstance(InvoiceKsefDto.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // Opcjonalnie: ładne formatowanie XML

        // 3. Marshalling (konwersja obiektu Java do XML)
        StringWriter sw = new StringWriter();
        marshaller.marshal(ksefFaktura, sw);

        return sw.toString();
    }
}