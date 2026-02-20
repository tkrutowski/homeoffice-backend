package net.focik.homeoffice.goahead.domain.invoice;

import jakarta.xml.bind.JAXBException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.exception.KsefResponseException;
import net.focik.homeoffice.goahead.domain.exception.StatusWaitingException;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.api.DefaultKsefClient;
import pl.akmf.ksef.sdk.api.builders.auth.AuthTokenRequestBuilder;
import pl.akmf.ksef.sdk.api.builders.auth.AuthTokenRequestSerializer;
import pl.akmf.ksef.sdk.api.builders.certificate.CertificateBuilders;
import pl.akmf.ksef.sdk.api.builders.session.OpenOnlineSessionRequestBuilder;
import pl.akmf.ksef.sdk.api.builders.session.SendInvoiceOnlineSessionRequestBuilder;
import pl.akmf.ksef.sdk.api.services.DefaultCertificateService;
import pl.akmf.ksef.sdk.api.services.DefaultCryptographyService;
import pl.akmf.ksef.sdk.api.services.DefaultSignatureService;
import pl.akmf.ksef.sdk.client.model.ApiException;
import pl.akmf.ksef.sdk.client.model.UpoVersion;
import pl.akmf.ksef.sdk.client.model.auth.*;
import pl.akmf.ksef.sdk.client.model.certificate.SelfSignedCertificate;
import pl.akmf.ksef.sdk.client.model.session.*;
import pl.akmf.ksef.sdk.client.model.session.online.OpenOnlineSessionRequest;
import pl.akmf.ksef.sdk.client.model.session.online.OpenOnlineSessionResponse;
import pl.akmf.ksef.sdk.client.model.session.online.SendInvoiceOnlineSessionRequest;
import pl.akmf.ksef.sdk.client.model.session.online.SendInvoiceResponse;
import pl.akmf.ksef.sdk.client.model.xml.AuthTokenRequest;
import pl.akmf.ksef.sdk.client.model.xml.SubjectIdentifierTypeEnum;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.concurrent.TimeUnit;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class KsefService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(KsefService.class);

    private final DefaultKsefClient ksefClient;
    private final KsefXmlGenerator ksefXmlGenerator;
//    private CertificateService certificateService;
//    private DefaultSignatureService signatureService;
//private DefaultCryptographyService defaultCryptographyService;
    
    // Pola do cache'owania sesji
    private String cachedAccessToken;
    private LocalDateTime tokenCreationTime;

    public String login(Company company) throws ApiException, IOException, JAXBException, InterruptedException {
        // Sprawdź czy mamy token i czy jest on "świeży" (np. mniej niż 20 minut)
        // Sesje KSeF trwają dłużej, ale dla bezpieczeństwa odświeżamy co 20 min
        if (cachedAccessToken != null && tokenCreationTime != null && 
            tokenCreationTime.plusMinutes(20).isAfter(LocalDateTime.now())) {
            log.info("Using cached KSeF token created at: {}", tokenCreationTime);
            return cachedAccessToken;
        }
        
        log.info("No valid token found. Performing full login to KSeF.");
        return loginWithCertificate(company);
    }
    String  createXml(Invoice invoice, Company goAhead) throws JAXBException, IOException {
        String xml = ksefXmlGenerator.generateInvoiceXml(invoice, goAhead);
        Files.writeString(Path.of("invoice.xml"), xml);
        return xml;
    }

    InvoiceKsefDto  createDto(Invoice invoice, Company goAhead) throws JAXBException, IOException {
        return ksefXmlGenerator.generateInvoice(invoice, goAhead);
    }

    private String loginWithCertificate(Company company) throws ApiException, IOException, JAXBException, InterruptedException {
        DefaultSignatureService signatureService = new DefaultSignatureService();
        DefaultCertificateService certificateService = new DefaultCertificateService();
        //"9720495827";//NIP
        //wykonanie auth challenge
        AuthenticationChallengeResponse challenge = ksefClient.getAuthChallenge();

        //xml niezbędny do uwierzytelnienia
        AuthTokenRequest authTokenRequest = new AuthTokenRequestBuilder()
                .withChallenge(challenge.getChallenge())
                .withContextNip(company.getNipWithoutDashes())
                .withSubjectType(SubjectIdentifierTypeEnum.CERTIFICATE_SUBJECT)
                .build();

        String xml = AuthTokenRequestSerializer.authTokenRequestSerializer(authTokenRequest);

        //wygenerowanie certyfikatu oraz klucza prywatnego
        CertificateBuilders.X500NameHolder x500 = new CertificateBuilders()
                .buildForOrganization(company.getName(), "VATPL-" + company.getNipWithoutDashes(), company.getName(), "PL");

        SelfSignedCertificate cert = certificateService.generateSelfSignedCertificateRsa(x500);

        //podpisanie xml wygenerowanym certyfikatem oraz kluczem prywatnym
        String signedXml = signatureService.sign(xml.getBytes(), cert.certificate(), cert.getPrivateKey());

        // Przesłanie podpisanego XML do systemu KSeF
        SignatureResponse submitAuthTokenResponse = ksefClient.submitAuthTokenRequest(signedXml, false);

        //Czekanie na zakończenie procesu
        waitForAuthProcess(submitAuthTokenResponse.getReferenceNumber(), submitAuthTokenResponse.getAuthenticationToken().getToken());
        //pobranie tokenów
        AuthOperationStatusResponse token = ksefClient.redeemToken(submitAuthTokenResponse.getAuthenticationToken().getToken());

        System.out.println();
        
        // Zapisz token w cache
        this.cachedAccessToken = token.getAccessToken().getToken();
        this.tokenCreationTime = LocalDateTime.now();
        
        return this.cachedAccessToken;
    }
    public void generateInvoice(){

    }

    public String sendInvoice(String accessToken, String contextNip, String xml) throws JAXBException, IOException, ApiException, InterruptedException {
        DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);

        EncryptionData encryptionData = defaultCryptographyService.getEncryptionData();

        // Step 1: Open session and return referenceNumber
        String sessionReferenceNumber = openOnlineSession(encryptionData, SystemCode.FA_2, SchemaVersion.VERSION_1_0E, SessionValue.FA, accessToken);

        // Step 2: Send invoice
        String invoiceReferenceNumber = sendInvoiceOnlineSession(contextNip, sessionReferenceNumber, encryptionData,
                xml, accessToken);

        // Wait for invoice to be processed && check session status
        waitForCondition(() -> isInvoicesInSessionProcessed(sessionReferenceNumber, accessToken), 90, 15, "Invoice processing did not finish in time.");

        // Step 3: Close session
        closeOnlineSession(sessionReferenceNumber, accessToken);

        waitForCondition(() -> isUpoGenerated(sessionReferenceNumber, accessToken), 30, 5, "UPO generation did not finish in time.");

        // Step 4: Get documents
        SessionInvoiceStatusResponse sessionInvoice = getOnlineSessionDocuments(sessionReferenceNumber, accessToken);
        return sessionInvoice.getKsefNumber();
    }

    public void onlineSessionE2EIntegrationTest(String accessToken, String contextNip, String xml) throws JAXBException, IOException, ApiException, InterruptedException {
        DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);

        EncryptionData encryptionData = defaultCryptographyService.getEncryptionData();

        // Step 1: Open session and return referenceNumber
        String sessionReferenceNumber = openOnlineSession(encryptionData, SystemCode.FA_2, SchemaVersion.VERSION_1_0E, SessionValue.FA, accessToken);

        // Step 2: Send invoice
        String invoiceReferenceNumber = sendInvoiceOnlineSession(contextNip, sessionReferenceNumber, encryptionData,
                xml, accessToken);

        // Wait for invoice to be processed && check session status
        waitForCondition(() -> isInvoicesInSessionProcessed(sessionReferenceNumber, accessToken), 30, 5, "Invoice processing did not finish in time.");

        // Step 3: Close session
        closeOnlineSession(sessionReferenceNumber, accessToken);

        waitForCondition(() -> isUpoGenerated(sessionReferenceNumber, accessToken), 30, 5, "UPO generation did not finish in time.");

        // Step 4: Get documents
        SessionInvoiceStatusResponse sessionInvoice = getOnlineSessionDocuments(sessionReferenceNumber, accessToken);
        String ksefNumber = sessionInvoice.getKsefNumber();

        // Step 5: Get status after close
        String upoReferenceNumber = getOnlineSessionUpoAfterCloseSession(sessionReferenceNumber, accessToken);

        // Step 6: Get UPO
        getOnlineSessionInvoiceUpo(sessionReferenceNumber, ksefNumber, accessToken);
        getOnlineSessionInvoiceUpoByInvoiceReferenceNumber(sessionReferenceNumber, invoiceReferenceNumber, accessToken);

        // Step 7: Get session UPO
        getOnlineSessionUpo(sessionReferenceNumber, upoReferenceNumber, accessToken);

        // Step 8: Get invoice
        getInvoice(sessionInvoice.getKsefNumber(), accessToken);
        System.out.println();
    }

    private void getInvoice(String ksefNumber, String accessToken) throws ApiException {
        byte[] invoiceBytes = ksefClient.getInvoice(ksefNumber, accessToken);
        if (invoiceBytes == null || invoiceBytes.length == 0) {
            throw new KsefResponseException(String.format("Failed to get invoice '%s'. Response was empty.", ksefNumber));
        }
    }
    private void getOnlineSessionInvoiceUpo(String sessionReferenceNumber, String ksefNumber, String accessToken) throws ApiException {
        byte[] upoResponse = ksefClient.getSessionInvoiceUpoByKsefNumber(sessionReferenceNumber, ksefNumber, accessToken);

        if (upoResponse == null || upoResponse.length == 0) {
            throw new KsefResponseException(String.format("Failed to get session invoice UPO for KSeF number '%s'. Response was empty.", ksefNumber));
        }
    }

    private void getOnlineSessionInvoiceUpoByInvoiceReferenceNumber(String sessionReferenceNumber, String invoiceReferenceNumber, String accessToken) throws ApiException {
        byte[] upoResponse = ksefClient.getSessionInvoiceUpoByReferenceNumber(sessionReferenceNumber, invoiceReferenceNumber, accessToken);

        if (upoResponse == null || upoResponse.length == 0) {
            throw new KsefResponseException(String.format("Failed to get session invoice UPO for reference number '%s'. Response was empty.", invoiceReferenceNumber));
        }
    }
    private void getOnlineSessionUpo(String sessionReferenceNumber, String upoReferenceNumber, String accessToken) throws ApiException {
        byte[] sessionUpo = ksefClient.getSessionUpo(sessionReferenceNumber, upoReferenceNumber, accessToken);

        if (sessionUpo == null || sessionUpo.length == 0) {
            throw new KsefResponseException(String.format("Failed to get session UPO for reference number '%s'. Response was empty.", upoReferenceNumber));
        }
    }
        private String openOnlineSession(EncryptionData encryptionData, SystemCode systemCode,
                                     SchemaVersion schemaVersion, SessionValue value, String accessToken) throws ApiException {
        OpenOnlineSessionRequest request = new OpenOnlineSessionRequestBuilder()
                .withFormCode(new FormCode(systemCode, schemaVersion, value))
                .withEncryptionInfo(encryptionData.encryptionInfo())
                .build();

        OpenOnlineSessionResponse openOnlineSessionResponse = ksefClient.openOnlineSession(request, UpoVersion.UPO_4_3, accessToken);

        if (openOnlineSessionResponse == null || openOnlineSessionResponse.getReferenceNumber() == null) {
            throw new KsefResponseException("Failed to open online session. Response or reference number is null.");
        }

        return openOnlineSessionResponse.getReferenceNumber();
    }

    private String sendInvoiceOnlineSession(String nip, String sessionReferenceNumber, EncryptionData encryptionData,
                                            String xml, String accessToken) throws IOException, ApiException {
        DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);
        String invoiceTemplate = xml
                .replace("#nip#", nip)
                .replace("#invoicing_date#", LocalDate.of(2025, 6, 15).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .replace("#invoice_number#", UUID.randomUUID().toString());

        byte[] invoice = invoiceTemplate.getBytes(StandardCharsets.UTF_8);

        byte[] encryptedInvoice = defaultCryptographyService.encryptBytesWithAES256(invoice,
                encryptionData.cipherKey(),
                encryptionData.cipherIv());

        FileMetadata invoiceMetadata = defaultCryptographyService.getMetaData(invoice);
        FileMetadata encryptedInvoiceMetadata = defaultCryptographyService.getMetaData(encryptedInvoice);

        SendInvoiceOnlineSessionRequest sendInvoiceOnlineSessionRequest = new SendInvoiceOnlineSessionRequestBuilder()
                .withInvoiceHash(invoiceMetadata.getHashSHA())
                .withInvoiceSize(invoiceMetadata.getFileSize())
                .withEncryptedInvoiceHash(encryptedInvoiceMetadata.getHashSHA())
                .withEncryptedInvoiceSize(encryptedInvoiceMetadata.getFileSize())
                .withEncryptedInvoiceContent(Base64.getEncoder().encodeToString(encryptedInvoice))
                .build();

        SendInvoiceResponse sendInvoiceResponse = ksefClient.onlineSessionSendInvoice(sessionReferenceNumber, sendInvoiceOnlineSessionRequest, accessToken);

        if (sendInvoiceResponse == null || sendInvoiceResponse.getReferenceNumber() == null) {
            throw new KsefResponseException("Failed to send invoice in online session. Response or reference number is null.");
        }

        return sendInvoiceResponse.getReferenceNumber();
    }
    
    private void isAuthStatusReady(String referenceNumber, String tempToken) throws ApiException {
        AuthStatus authStatus = ksefClient.getAuthStatus(referenceNumber, tempToken);

        if (authStatus.getStatus().getCode() != 200) {
            throw new StatusWaitingException("Authentication process has not been finished yet");
        }
    }

    private void closeOnlineSession(String sessionReferenceNumber, String accessToken) throws ApiException {
        ksefClient.closeOnlineSession(sessionReferenceNumber, accessToken);
    }

    private SessionInvoiceStatusResponse getOnlineSessionDocuments(String sessionReferenceNumber, String accessToken) throws ApiException {
        SessionInvoicesResponse sessionInvoices = ksefClient.getSessionInvoices(sessionReferenceNumber, null, 10, accessToken);

        if (sessionInvoices == null || sessionInvoices.getInvoices() == null || sessionInvoices.getInvoices().size() != 1) {
            int count = (sessionInvoices == null || sessionInvoices.getInvoices() == null) ? 0 : sessionInvoices.getInvoices().size();
            throw new KsefResponseException(String.format("Expected exactly one invoice in the session, but found %d.", count));
        }

        SessionInvoiceStatusResponse invoice = sessionInvoices.getInvoices().getFirst();

        if (invoice == null || invoice.getKsefNumber() == null || invoice.getStatus() == null) {
            throw new KsefResponseException("Received incomplete invoice data from KSeF. KSeF number or status is null.");
        }

        if (invoice.getStatus().getCode() != 200) {
            throw new KsefResponseException(String.format("Invoice processing failed with status code: %d. Message: %s",
                    invoice.getStatus().getCode(), invoice.getStatus().getDescription()));
        }

        return invoice;
    }
    
    private void waitForAuthProcess(String referenceNumber, String tempToken) throws InterruptedException, ApiException {
        for (int i = 0; i < 14; i++) { // Pętla próbująca 10 razy
            AuthStatus authStatus = ksefClient.getAuthStatus(referenceNumber, tempToken);
            if (authStatus.getStatus().getCode() == 200) {
                return; // Sukces, proces zakończony
            }
            TimeUnit.SECONDS.sleep(1); // Czekaj 1 sekundę przed kolejną próbą
        }
        throw new StatusWaitingException("Authentication process did not finish in time.");
    }

    private boolean isInvoicesInSessionProcessed(String sessionReferenceNumber, String accessToken) {
        try {
            SessionStatusResponse statusResponse = ksefClient.getSessionStatus(sessionReferenceNumber, accessToken);
            // 315 - zakończono przetwarzanie
            return  statusResponse != null &&
                    statusResponse.getSuccessfulInvoiceCount() != null &&
                    statusResponse.getSuccessfulInvoiceCount() > 0;
        } catch (ApiException e) {
            log.warn("Polling for invoice processing status failed for session {}: {}", sessionReferenceNumber, e.getMessage());
            return false;
        }
    }

    private boolean isUpoGenerated(String sessionReferenceNumber, String accessToken) {
        try {
            SessionStatusResponse status = ksefClient.getSessionStatus(sessionReferenceNumber, accessToken);
            // 200 - OK, UPO jest dostępne
            return status != null && status.getStatus() != null && status.getStatus().getCode() == 200;
        } catch (ApiException e) {
            log.warn("Polling for UPO generation status failed for session {}: {}", sessionReferenceNumber, e.getMessage());
            return false;
        }
    }

    private String getOnlineSessionUpoAfterCloseSession(String sessionReferenceNumber, String accessToken) throws ApiException {
        SessionStatusResponse statusResponse = ksefClient.getSessionStatus(sessionReferenceNumber, accessToken);

        if (statusResponse == null) {
            throw new KsefResponseException("Failed to get session status after close. Response was null.");
        }
        if (statusResponse.getStatus() == null || statusResponse.getStatus().getCode() != 200) {
            int code = statusResponse.getStatus() != null ? statusResponse.getStatus().getCode() : -1;
            String message = statusResponse.getStatus() != null ? statusResponse.getStatus().getDescription() : "No status message";
            throw new KsefResponseException(String.format("Invalid session status after close. Code: %d, Message: %s", code, message));
        }
        if (statusResponse.getSuccessfulInvoiceCount() == null || statusResponse.getSuccessfulInvoiceCount() != 1) {
            throw new KsefResponseException("Expected exactly one successful invoice, but found: " + statusResponse.getSuccessfulInvoiceCount());
        }
        if (statusResponse.getUpo() == null || statusResponse.getUpo().getPages() == null || statusResponse.getUpo().getPages().isEmpty()) {
            throw new KsefResponseException("UPO data is missing in the session status response.");
        }

        UpoPageResponse upoPageResponse = statusResponse.getUpo().getPages().getFirst();
        if (upoPageResponse == null || upoPageResponse.getReferenceNumber() == null) {
            throw new KsefResponseException("UPO page or its reference number is missing.");
        }

        return upoPageResponse.getReferenceNumber();
    }

    private void waitForCondition(BooleanSupplier condition, int timeoutSeconds, int pollIntervalSeconds, String timeoutMessage) throws InterruptedException {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return; // Warunek spełniony
            }
            TimeUnit.SECONDS.sleep(pollIntervalSeconds);
        }
        throw new StatusWaitingException(timeoutMessage);
    }

//    protected AuthTokensPair authWithCustomPesel(String context, String pesel, EncryptionMethod encryptionMethod) throws ApiException, JAXBException, IOException {
//        AuthenticationChallengeResponse challenge = ksefClient.getAuthChallenge();
//
//        AuthTokenRequest authTokenRequest = new AuthTokenRequestBuilder()
//                .withChallenge(challenge.getChallenge())
//                .withContextNip(context)
//                .withSubjectType(SubjectIdentifierTypeEnum.CERTIFICATE_SUBJECT)
//                .build();
//
//        String xml = AuthTokenRequestSerializer.authTokenRequestSerializer(authTokenRequest);
//
//        SelfSignedCertificate cert = certificateService.getPersonalCertificate("M", "B", "PNOPL", pesel, "M B", encryptionMethod);
//
//        String signedXml = signatureService.sign(xml.getBytes(), cert.certificate(), cert.getPrivateKey());
//
//        SignatureResponse submitAuthTokenResponse = ksefClient.submitAuthTokenRequest(signedXml, false);
//
//        //Czekanie na zakończenie procesu
//        await().atMost(14, SECONDS)
//                .pollInterval(1, SECONDS)
//                .until(() -> isAuthProcessReady(submitAuthTokenResponse.getReferenceNumber(), submitAuthTokenResponse.getAuthenticationToken().getToken()));
//
//        AuthOperationStatusResponse tokenResponse = ksefClient.redeemToken(submitAuthTokenResponse.getAuthenticationToken().getToken());
//
//        return new AuthTokensPair(tokenResponse.getAccessToken().getToken(), tokenResponse.getRefreshToken().getToken());
//    }

}
