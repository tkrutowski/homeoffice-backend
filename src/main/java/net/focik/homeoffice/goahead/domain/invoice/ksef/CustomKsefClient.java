package net.focik.homeoffice.goahead.domain.invoice.ksef;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.api.DefaultKsefClient;
import pl.akmf.ksef.sdk.api.KsefApiProperties;
import pl.akmf.ksef.sdk.client.model.ApiException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static pl.akmf.ksef.sdk.client.Headers.*;

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

    public byte[] getInvoicePdf(String invoiceKsefNumber, String invoiceXml, String accessToken) throws ApiException {
        Map<String, Object> body = new HashMap<>();
        body.put("offline", false);
        body.put("invoiceKsefNumber", invoiceKsefNumber);
        body.put("invoiceData", invoiceXml);
        body.put("outputFormat", "pdf");
        body.put("outputLanguage", "pl");

        Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION, BEARER + accessToken);
        headers.put(ACCEPT, "application/pdf");
        headers.put(CONTENT_TYPE, APPLICATION_JSON);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            // Budowa request
            String URL_INVOICES_PDF = "/invoice/visualize";
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + URL_INVOICES_PDF));

            headers.forEach(builder::header);

            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Wysyłka (sync)
            HttpResponse<byte[]> response = apiClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray()); // dla PDF binary [web:15]

            int status = response.statusCode();
            byte[] pdfBytes = response.body();

// Obsłuż response, np. zapisz PDF
            Files.write(Paths.get("invoice.pdf"), pdfBytes);
//            String fullUri = ksefApiProperties.getUrl() + URL_INVOICES_PDF;
//            HttpRequest request = buildRequest(fullUri, "POST", jsonBody, headers);
//            HttpResponse<byte[]> response = sendHttpRequest(request, HttpResponse.BodyHandlers.ofByteArray());
//
//            if (response.statusCode() != OK.getCode()) {
//                throw new ApiException(response.statusCode(), "Failed to download PDF", response.headers(), null);
//            }

            return response.body();
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

}
