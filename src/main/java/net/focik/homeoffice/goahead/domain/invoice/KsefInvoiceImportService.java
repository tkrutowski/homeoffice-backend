package net.focik.homeoffice.goahead.domain.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;
import net.focik.homeoffice.goahead.domain.customer.port.primary.AddCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.GetCustomerUseCase;
import net.focik.homeoffice.goahead.domain.invoice.ksef.KsefAddressParser;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.FakturaCtrl;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.Podmiot2;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.AddInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.ImportKsefInvoicesUseCase;
import org.springframework.stereotype.Service;
import pl.akmf.ksef.sdk.client.model.invoice.InvoiceQuerySubjectType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import faktur sprzedażowych z KSeF, w tym wystawionych przez portal KSeF poza aplikacją.
 * Pomijane są faktury inne niż zwykła VAT (korygujące, zaliczkowe itd.) oraz w walucie obcej.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KsefInvoiceImportService implements ImportKsefInvoicesUseCase {

    private static final String REGULAR_INVOICE = "VAT";
    private static final String DEFAULT_CURRENCY = "PLN";

    private final KsefService ksefService;
    private final KsefInvoiceImportMapper mapper;
    private final InvoiceService invoiceService;
    private final AddInvoiceUseCase addInvoiceUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final AddCustomerUseCase addCustomerUseCase;

    @Override
    public KsefInvoiceImportResult importKsefInvoices(LocalDate fromDate, LocalDate toDate) {
        Map<InvoiceKsefDto, String> found = ksefService.findInvoices(fromDate, toDate, InvoiceQuerySubjectType.SUBJECT1);

        List<Invoice> imported = new ArrayList<>();
        List<AsyncTaskError> errors = new ArrayList<>();
        int duplicates = 0;
        int skipped = 0;

        for (Map.Entry<InvoiceKsefDto, String> entry : found.entrySet()) {
            InvoiceKsefDto dto = entry.getKey();
            String metadata = entry.getValue();
            FakturaCtrl fa = dto.getFakturaCtrl();
            String number = fa != null ? fa.getNumerFaktury() : null;
            String ksefNumber = ksefService.getFromJson(metadata, "ksefNumber");

            try {
                if (fa == null) {
                    throw new IllegalArgumentException("Brak sekcji Fa w fakturze");
                }
                if (ksefNumber == null) {
                    throw new IllegalArgumentException("Brak numeru KSeF w metadanych faktury");
                }
                if (invoiceService.existsByKsefNumber(ksefNumber)) {
                    duplicates++;
                    continue;
                }
                if (isSkipped(fa)) {
                    log.info("Pomijam fakturę {} ({}): rodzaj={}, waluta={}", number, ksefNumber, fa.getRodzajFaktury(), fa.getKodWaluty());
                    skipped++;
                    continue;
                }

                Invoice invoice = mapper.toInvoice(dto);
                invoice.setKsefNumber(ksefNumber);
                invoice.setInvoiceHash(ksefService.getFromJson(metadata, "invoiceHash"));
                invoice.setCustomer(resolveCustomer(dto.getPodmiot2()));

                imported.add(addInvoiceUseCase.addInvoice(invoice));
            } catch (Exception e) {
                log.error("Błąd importu faktury {} ({}) z KSeF", number, ksefNumber, e);
                errors.add(new AsyncTaskError(ksefNumber != null ? ksefNumber : number, e.getMessage()));
            }
        }

        return new KsefInvoiceImportResult(imported, found.size(), duplicates, skipped, errors);
    }

    private boolean isSkipped(FakturaCtrl fa) {
        boolean regular = fa.getRodzajFaktury() == null || REGULAR_INVOICE.equalsIgnoreCase(fa.getRodzajFaktury());
        boolean pln = fa.getKodWaluty() == null || DEFAULT_CURRENCY.equalsIgnoreCase(fa.getKodWaluty());
        return !regular || !pln;
    }

    private Customer resolveCustomer(Podmiot2 buyer) {
        if (buyer == null || buyer.getDaneIdentyfikacyjne() == null || buyer.getDaneIdentyfikacyjne().getNip() == null) {
            throw new IllegalArgumentException("Nabywca bez NIP - nie można dopasować klienta");
        }
        String nip = buyer.getDaneIdentyfikacyjne().getNip();

        return getCustomerUseCase.findByNip(nip).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setName(buyer.getDaneIdentyfikacyjne().getNazwa());
            customer.setNip(nip);
            customer.setCustomerType(CustomerType.COMPANY);
            customer.setActiveStatus(ActiveStatus.ACTIVE);
            if (buyer.getDaneKontaktowe() != null) {
                customer.setMail(buyer.getDaneKontaktowe().getEmail());
                customer.setPhone(buyer.getDaneKontaktowe().getTelefon());
            }
            var address = buyer.getAdres() == null ? java.util.Optional.<KsefAddressParser.ParsedAddress>empty()
                    : KsefAddressParser.parse(buyer.getAdres().getAdresL1(), buyer.getAdres().getAdresL2());
            customer.setAddress(
                    address.map(KsefAddressParser.ParsedAddress::city).orElse(""),
                    address.map(KsefAddressParser.ParsedAddress::street).orElse(""),
                    address.map(KsefAddressParser.ParsedAddress::zip).orElse(""));
            return addCustomerUseCase.addCustomer(customer);
        });
    }
}
