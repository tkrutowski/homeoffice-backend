package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.goahead.api.dto.BasicDto;
import net.focik.homeoffice.goahead.api.dto.InvoiceDto;
import net.focik.homeoffice.goahead.api.dto.PaymentMethodDto;
import net.focik.homeoffice.goahead.api.mapper.ApiInvoiceMapper;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoicePdf;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.AddInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.DeleteInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.GetInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.UpdateInvoiceUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentMethod;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/goahead/invoice")
//@CrossOrigin
public class InvoiceController extends ExceptionHandling {

    private final GetInvoiceUseCase getInvoiceUseCase;
    private final AddInvoiceUseCase addInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final DeleteInvoiceUseCase deleteInvoiceUseCase;
    private final ApiInvoiceMapper mapper;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<InvoiceDto> getById(@PathVariable int id) {
        log.info("Try find invoice by id: " + id);

        Invoice invoice = getInvoiceUseCase.findById(id);

        log.info(invoice != null ? "Found invoice for id = " + id : "Not found invoice for id = " + id);

        if (invoice == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(mapper.toDto(invoice), HttpStatus.OK);
    }

    @GetMapping("/pdf/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<?> getPdfById(@PathVariable int id) {
        log.info("Try download invoice by id: " + id);
        Invoice invoice = getInvoiceUseCase.findFullById(id);
        String fileName = InvoicePdf.createPdf(invoice);
        Resource resource;
        try {
            assert fileName != null;
            Path path = Path.of(fileName);
            resource = new UrlResource(path.toUri());
        } catch (IOException e) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);
    }

    @GetMapping("/number/{year}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<Integer> getInvoiceNumber(@PathVariable int year) {
        log.info("Try get new invoice number");

        int newInvoiceNumber = getInvoiceUseCase.getNewInvoiceNumber(year);

        log.info(newInvoiceNumber != 0 ? "New invoice number = " + newInvoiceNumber : "Not found new invoice number");

        return new ResponseEntity<>(newInvoiceNumber, HttpStatus.OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<List<InvoiceDto>> getAllInvoices(@RequestParam PaymentStatus status) {
        log.info("Try find all invoices by PaymentStatus = " + status);
        List<Invoice> invoices;
        invoices = getInvoiceUseCase.findAllBy(status, true, true);
        log.info("Found " + invoices.size() + " invoices.");

        return new ResponseEntity<>(invoices.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<InvoiceDto> addInvoice(@RequestBody InvoiceDto invoiceDto) {
        log.info("Try add new invoice.");
        Invoice invoice = mapper.toDomain(invoiceDto);
        Invoice result = addInvoiceUseCase.addInvoice(invoice);

        log.info(result.getIdInvoice() > 0 ? "Invoice added with id = " + result.getIdInvoice() : "No invoice added!");

        return new ResponseEntity<>(mapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<InvoiceDto> updateInvoice(@RequestBody InvoiceDto invoiceDto) {
        log.info("Try update invoice.");
        Invoice invoice = updateInvoiceUseCase.updateInvoice(mapper.toDomain(invoiceDto));
        return new ResponseEntity<>(mapper.toDto(invoice), OK);
    }

    @DeleteMapping("/{idInvoice}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_DELETE_ALL')")
    public ResponseEntity<HttpResponse> deleteInvoice(@PathVariable int idInvoice) {
        log.info("Try delete invoice with id: " + idInvoice);
        deleteInvoiceUseCase.deleteInvoice(idInvoice);
        log.info("Deleted invoice with id = " + idInvoice);
        return response(HttpStatus.NO_CONTENT, "Faktura usunięty.");
    }

    @PutMapping("/paymentstatus/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<HttpResponse> updatePaymentStatus(@PathVariable int id,  @RequestBody BasicDto basicDto) {
        log.info("Try update payment status.");
        updateInvoiceUseCase.updatePaymentStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        return response(HttpStatus.OK, "Zaaktualizowano status pracownika.");
    }

    @GetMapping("/paymenttype")
    ResponseEntity<List<PaymentMethodDto>> getPaymentTypes() {
        PaymentMethod[] collect = (PaymentMethod.values());
        List<PaymentMethodDto> paymentTypeDtos = Arrays.stream(collect)
                .map(type -> new PaymentMethodDto(type.name(), type.getViewValue()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(paymentTypeDtos, OK);
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}