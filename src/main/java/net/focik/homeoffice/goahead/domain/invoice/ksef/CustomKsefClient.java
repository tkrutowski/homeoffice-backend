package net.focik.homeoffice.goahead.domain.invoice.ksef;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.focik.homeoffice.goahead.domain.exception.KsefResponseException;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.api.DefaultKsefClient;
import pl.akmf.ksef.sdk.api.KsefApiProperties;
import pl.akmf.ksef.sdk.api.services.DefaultQrCodeService;
import pl.akmf.ksef.sdk.api.services.DefaultVerificationLinkService;
import pl.akmf.ksef.sdk.client.model.ApiException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

@Component
public class CustomKsefClient extends DefaultKsefClient {

    private final ObjectMapper objectMapper;
    private final HttpClient apiClient;
    private KsefApiProperties ksefApiProperties;
    private String baseURl;
    private String suffixURl;
    private Duration timeout;
    private Map<String, String> defaultHeaders;
    private final String baseUrl;

    public CustomKsefClient(HttpClient apiClient, KsefApiProperties ksefApiProperties, ObjectMapper objectMapper) {
        super(apiClient, ksefApiProperties, objectMapper);
        this.objectMapper = objectMapper;
        this.apiClient = apiClient;
        this.ksefApiProperties = ksefApiProperties;
        this.baseURl = ksefApiProperties.getBaseUri();
        this.suffixURl = ksefApiProperties.getSuffixUri();
        this.defaultHeaders = ksefApiProperties.getDefaultHeaders();
        this.baseUrl = ksefApiProperties.getBaseUri();
    }

    public byte[] getQrCode(String invoiceKsefNumber, String invoiceHash, String nip, LocalDate invoicingDate) {

        try {
            DefaultVerificationLinkService verificationLinkService = new DefaultVerificationLinkService(ksefApiProperties.getQrUri());
            DefaultQrCodeService qrCodeService = new DefaultQrCodeService();
            String invoiceForOnlineUrl = verificationLinkService.buildInvoiceVerificationUrl(nip, invoicingDate, invoiceHash);

            //Utworzenie kodu QR dla trybu online
            byte[] qrOnline = qrCodeService.generateQrCode(invoiceForOnlineUrl);
            //Dodanie napisu z numerem faktury do kodu QR (Label)
            qrOnline = qrCodeService.addLabelToQrCode(qrOnline, invoiceKsefNumber);

            return qrOnline;
        } catch (Exception e) {
            throw new KsefResponseException("Error creating qr code",e);
        }
    }
}
