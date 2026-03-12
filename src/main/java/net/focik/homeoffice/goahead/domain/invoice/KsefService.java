package net.focik.homeoffice.goahead.domain.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.config.KsefInvoiceApiProperties;
import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.company.CompanyFacade;
import net.focik.homeoffice.goahead.domain.exception.KsefResponseException;
import net.focik.homeoffice.goahead.domain.exception.StatusWaitingException;
import net.focik.homeoffice.goahead.domain.invoice.ksef.CustomKsefClient;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.SendKsefInvoiceResponse;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.api.builders.auth.AuthKsefTokenRequestBuilder;
import pl.akmf.ksef.sdk.api.builders.auth.AuthTokenRequestBuilder;
import pl.akmf.ksef.sdk.api.builders.auth.AuthTokenRequestSerializer;
import pl.akmf.ksef.sdk.api.builders.certificate.CertificateBuilders;
import pl.akmf.ksef.sdk.api.builders.invoices.InvoicesAsyncQueryFiltersBuilder;
import pl.akmf.ksef.sdk.api.builders.session.OpenOnlineSessionRequestBuilder;
import pl.akmf.ksef.sdk.api.builders.session.SendInvoiceOnlineSessionRequestBuilder;
import pl.akmf.ksef.sdk.api.services.DefaultCertificateService;
import pl.akmf.ksef.sdk.api.services.DefaultCryptographyService;
import pl.akmf.ksef.sdk.api.services.DefaultSignatureService;
import pl.akmf.ksef.sdk.client.model.ApiException;
import pl.akmf.ksef.sdk.client.model.UpoVersion;
import pl.akmf.ksef.sdk.client.model.auth.*;
import pl.akmf.ksef.sdk.client.model.certificate.SelfSignedCertificate;
import pl.akmf.ksef.sdk.client.model.invoice.*;
import pl.akmf.ksef.sdk.client.model.session.*;
import pl.akmf.ksef.sdk.client.model.session.online.OpenOnlineSessionRequest;
import pl.akmf.ksef.sdk.client.model.session.online.OpenOnlineSessionResponse;
import pl.akmf.ksef.sdk.client.model.session.online.SendInvoiceOnlineSessionRequest;
import pl.akmf.ksef.sdk.client.model.session.online.SendInvoiceResponse;
import pl.akmf.ksef.sdk.client.model.xml.AuthTokenRequest;
import pl.akmf.ksef.sdk.client.model.xml.SubjectIdentifierTypeEnum;
import pl.akmf.ksef.sdk.system.FilesUtil;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@Component
@RequiredArgsConstructor
public class KsefService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(KsefService.class);

    private final CustomKsefClient ksefClient;
    private final KsefXmlGenerator ksefXmlGenerator;
    private final ObjectMapper objectMapper;
    private final CompanyFacade companyFacade;
    private final KsefInvoiceApiProperties properties;

    // Pola do cache'owania sesji
    private String cachedAccessToken;
    private LocalDateTime tokenCreationTime;

    public String getToken() {
        // Sprawdź czy mamy token i czy jest on "świeży" (np. mniej niż 20 minut)
        // Sesje KSeF trwają dłużej, ale dla bezpieczeństwa odświeżamy co 20 min
        if (cachedAccessToken != null && tokenCreationTime != null &&
                tokenCreationTime.plusMinutes(20).isAfter(LocalDateTime.now())) {
            log.info("Using cached KSeF token created at: {}", tokenCreationTime);
            return cachedAccessToken;
        }
        log.info("No valid token found. Performing full login to KSeF.");
        Company company = companyFacade.get();
//        return loginWithCertificate(company);
        return  loginWithToken(company);
    }

    String createXml(Invoice invoice, Company goAhead)  {
        String xml;
        try {
            xml = ksefXmlGenerator.generateInvoiceXml(invoice, goAhead);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return xml;
    }

    InvoiceKsefDto createDto(Invoice invoice, Company goAhead) {
        return ksefXmlGenerator.generateInvoice(invoice, goAhead);
    }

    private String loginWithToken(Company company) {
     try{
         AuthenticationChallengeResponse challenge = ksefClient.getAuthChallenge();
        DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);
        byte[] encryptedToken = defaultCryptographyService.encryptKsefTokenWithRSAUsingPublicKey(properties.getToken(), challenge.getTimestamp());

        AuthKsefTokenRequest authTokenRequest = new AuthKsefTokenRequestBuilder()
                .withChallenge(challenge.getChallenge())
                .withContextIdentifier(new ContextIdentifier(ContextIdentifier.IdentifierType.NIP, company.getNipWithoutDashes()))
                .withEncryptedToken(Base64.getEncoder().encodeToString(encryptedToken))
                .build();

        SignatureResponse response = ksefClient.authenticateByKSeFToken(authTokenRequest);

        waitForAuthProcess(response.getReferenceNumber(), response.getAuthenticationToken().getToken());

         AuthOperationStatusResponse tokenResponse = ksefClient.redeemToken(response.getAuthenticationToken().getToken());
         System.out.println();

         // Zapisz token w cache
         this.cachedAccessToken = tokenResponse.getAccessToken().getToken();
         this.tokenCreationTime = LocalDateTime.now();

         return tokenResponse.getAccessToken().getToken();

    } catch (ApiException | InterruptedException e) {
        throw new KsefResponseException("Failed to login with certificate to KSeF system", e);
    }
    }


    private String loginWithCertificate(Company company) {
        DefaultSignatureService signatureService = new DefaultSignatureService();
        DefaultCertificateService certificateService = new DefaultCertificateService();
        //"9720495827";//NIP
        //wykonanie auth challenge
        try {

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

        } catch (ApiException | IOException | JAXBException | InterruptedException e) {
            throw new KsefResponseException("Failed to login with certificate to KSeF system", e);
        }
        return this.cachedAccessToken;
    }


    public List<InvoiceKsefDto> findInvoices(LocalDate fromDate, LocalDate toDate, InvoiceQuerySubjectType subjectType) {
        List<InvoiceKsefDto> foundInvoices = new ArrayList<>();
        try {
            DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);
            String accessToken = getToken();
            EncryptionData encryptionData = defaultCryptographyService.getEncryptionData();
            InvoiceExportFilters filters = new InvoicesAsyncQueryFiltersBuilder()
                    .withSubjectType(subjectType)
                    .withDateRange(new InvoiceQueryDateRange(InvoiceQueryDateType.INVOICING, fromDate.atStartOfDay().atOffset(ZoneOffset.UTC), toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)))
                    .build();

            InvoiceExportRequest request = new InvoiceExportRequest(
                    new EncryptionInfo(encryptionData.encryptionInfo().getEncryptedSymmetricKey(),
                            encryptionData.encryptionInfo().getInitializationVector()), filters);

            InitAsyncInvoicesQueryResponse response = ksefClient.initAsyncQueryInvoice(request, accessToken);

            waitForCondition(() -> isFindInvoicesQueryProcessed(response.getReferenceNumber(), accessToken), 45, 5);

            InvoiceExportStatus invoiceExportStatus = ksefClient.checkStatusAsyncQueryInvoice(response.getReferenceNumber(), accessToken);

            List<InvoicePackagePart> parts = invoiceExportStatus.getPackageParts().getParts();
            byte[] mergedZip = FilesUtil.mergeZipParts(
                    encryptionData,
                    parts,
                    ksefClient::downloadPackagePart,
                    defaultCryptographyService::decryptBytesWithAes256
            );
            Map<String, String> downloadedFiles = FilesUtil.unzip(mergedZip);

            String metadataJson = downloadedFiles.keySet()
                    .stream()
                    .filter(fileName -> fileName.endsWith(".json"))
                    .findFirst()
                    .map(downloadedFiles::get)
                    .orElse(null);
//        InvoicePackageMetadata invoicePackageMetadata = objectMapper.readValue(metadataJson, InvoicePackageMetadata.class);

            List<String> invoices = downloadedFiles.keySet()
                    .stream()
                    .filter(fileName -> fileName.endsWith(".xml"))
                    .toList();

            for (String fileName : invoices) {
                String xmlContent = downloadedFiles.get(fileName);
                System.out.println("Nazwa pliku: " + fileName);
                //System.out.println("Treść XML: " + xmlContent);

                try {
                    JAXBContext context = JAXBContext.newInstance(InvoiceKsefDto.class);
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    InvoiceKsefDto invoiceDto = (InvoiceKsefDto) unmarshaller.unmarshal(new StringReader(xmlContent));
                    foundInvoices.add(invoiceDto);
                } catch (JAXBException e) {
                    log.error("Błąd mapowania XML na InvoiceKsefDto: " + e.getMessage(), e);
                }

                System.out.println();
            }
        } catch (ApiException | IOException | InterruptedException e) {
            throw new KsefResponseException("Failed to login with certificate to KSeF system", e);
        }
        return foundInvoices;
    }

    private boolean isFindInvoicesQueryProcessed(String referenceNumber, String accessToken) {
        try {
            InvoiceExportStatus status = ksefClient.checkStatusAsyncQueryInvoice(referenceNumber, accessToken);
            // 200 - OK, UPO jest dostępne
            return status != null && status.getStatus() != null && status.getStatus().getCode() == 200;
        } catch (ApiException e) {
            log.warn("Failed to check status of async query invoice: {}", e.getMessage());
            return false;
        }
    }

    public List<SendKsefInvoiceResponse> sendInvoices(List<String> xmls) {
        try {
            String accessToken = getToken();
            DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);

            EncryptionData encryptionData = defaultCryptographyService.getEncryptionData();

            // Step 1: Open session and return referenceNumber
            String sessionReferenceNumber = openOnlineSession(encryptionData, accessToken);


            // Step 2: Send invoice
            List<String> invoiceReferenceNumbers = new ArrayList<>();
            for (String xml : xmls) {
                String invoiceReferenceNumber = sendInvoiceOnlineSession(sessionReferenceNumber, encryptionData,
                        xml, accessToken);
                invoiceReferenceNumbers.add(invoiceReferenceNumber);
            }

            List<SessionInvoiceStatusResponse> invoiceStatusResponses = new ArrayList<>();
            for (String invoiceReferenceNumber : invoiceReferenceNumbers) {
                waitForCondition(() -> isInvoiceInSessionProcessed(sessionReferenceNumber, invoiceReferenceNumber, accessToken), 40, 5);
                SessionInvoiceStatusResponse sessionInvoiceStatus = ksefClient.getSessionInvoiceStatus(sessionReferenceNumber, invoiceReferenceNumber, accessToken);
                invoiceStatusResponses.add(sessionInvoiceStatus);
            }

            // Step 3: Close session
            closeOnlineSession(sessionReferenceNumber, accessToken);

            waitForCondition(() -> isUpoGenerated(sessionReferenceNumber, accessToken), 30, 7);

            SessionStatusResponse sessionStatus = ksefClient.getSessionStatus(sessionReferenceNumber, accessToken);

            List<SendKsefInvoiceResponse> sendKsefInvoiceRespons = new ArrayList<>();
            for (SessionInvoiceStatusResponse sessionInvoiceStatus : invoiceStatusResponses) {
                String upo = null;
                if (sessionInvoiceStatus.getKsefNumber() != null) {
                    byte[] sessionInvoiceUpoByKsefNumber = ksefClient.getSessionInvoiceUpoByKsefNumber(sessionReferenceNumber, sessionInvoiceStatus.getKsefNumber(), accessToken);
                    upo = new String(sessionInvoiceUpoByKsefNumber);
                }

                int invoiceCount = sessionStatus.getInvoiceCount();
                int successfulInvoiceCount = Optional.of(sessionStatus).map(SessionStatusResponse::getSuccessfulInvoiceCount).orElse(0);
                int failedInvoiceCount = Optional.of(sessionStatus).map(SessionStatusResponse::getFailedInvoiceCount).orElse(0);
                sendKsefInvoiceRespons.add(new SendKsefInvoiceResponse(sessionInvoiceStatus.getKsefNumber(), sessionInvoiceStatus.getInvoiceNumber(), upo, invoiceCount, successfulInvoiceCount, failedInvoiceCount));
            }
            return sendKsefInvoiceRespons;
        } catch (ApiException | InterruptedException e) {
            throw new KsefResponseException("Failed to login with certificate to KSeF system", e);
        }
    }


    private void getInvoiceByKsefNumber(String ksefNumber) throws ApiException {
        byte[] invoiceBytes = ksefClient.getInvoice(ksefNumber, getToken());
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

    private String openOnlineSession(EncryptionData encryptionData, String accessToken) throws ApiException {
        OpenOnlineSessionRequest request = new OpenOnlineSessionRequestBuilder()
                .withFormCode(new FormCode(SystemCode.FA_3, SchemaVersion.VERSION_1_0E, SessionValue.FA))
                .withEncryptionInfo(encryptionData.encryptionInfo())
                .build();

        OpenOnlineSessionResponse openOnlineSessionResponse = ksefClient.openOnlineSession(request, UpoVersion.UPO_4_3, accessToken);

        if (openOnlineSessionResponse == null || openOnlineSessionResponse.getReferenceNumber() == null) {
            throw new KsefResponseException("Failed to open online session. Response or reference number is null.");
        }

        return openOnlineSessionResponse.getReferenceNumber();
    }

    private String sendInvoiceOnlineSession(String sessionReferenceNumber, EncryptionData encryptionData,
                                            String xml, String accessToken) throws ApiException {
        DefaultCryptographyService defaultCryptographyService = new DefaultCryptographyService(ksefClient);
        byte[] invoice = xml.getBytes(StandardCharsets.UTF_8);

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



    private void waitForAuthProcess(String referenceNumber, String tempToken) throws InterruptedException, ApiException {
        for (int i = 0; i < 15; i++) { // Pętla próbująca przez ok. 15 sekund
            AuthStatus authStatus = ksefClient.getAuthStatus(referenceNumber, tempToken);
            int code = authStatus.getStatus().getCode();

            if (code == 200) {
                return; // Sukces, proces zakończony
            }
            if (code >= 400) {
                throw new KsefResponseException("Błąd uwierzytelnienia KSeF (kod " + code + "): " + authStatus.getStatus().getDescription());
            }
            TimeUnit.SECONDS.sleep(1); // Czekaj 1 sekundę przed kolejną próbą
        }
        throw new StatusWaitingException("Authentication process did not finish in time.");
    }

    private boolean isInvoicesInSessionProcessed(String sessionReferenceNumber, String accessToken) {
        try {
            SessionStatusResponse statusResponse = ksefClient.getSessionStatus(sessionReferenceNumber, accessToken);
            // 315 - zakończono przetwarzanie
            return statusResponse != null &&
                    statusResponse.getSuccessfulInvoiceCount() != null &&
                    statusResponse.getSuccessfulInvoiceCount() > 0;
        } catch (ApiException e) {
            log.warn("Polling for invoice processing status failed for session {}: {}", sessionReferenceNumber, e.getMessage());
            return false;
        }
    }

    private boolean isInvoiceInSessionProcessed(String sessionReferenceNumber, String invoiceReferenceNumber, String accessToken) {
        try {
            SessionInvoiceStatusResponse status = ksefClient.getSessionInvoiceStatus(sessionReferenceNumber, invoiceReferenceNumber, accessToken);
            return status != null && status.getStatus() != null && status.getStatus().getCode() == 200;
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

    private void waitForCondition(BooleanSupplier condition, int timeoutSeconds, int pollIntervalSeconds) throws InterruptedException {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return; // Warunek spełniony
            }
            TimeUnit.SECONDS.sleep(pollIntervalSeconds);
        }
    }
}
