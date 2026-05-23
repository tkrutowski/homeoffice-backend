package net.focik.homeoffice.fileService.infrastructure.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.config.AwsProperties;
import net.focik.homeoffice.fileService.domain.model.InvoiceClaudeResponseDto;
import net.focik.homeoffice.fileService.domain.port.secondary.InvoiceExtractorPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeInvoiceExtractorAdapter implements InvoiceExtractorPort {
    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Jesteś ekspertem od analizowania polskich faktur VAT.
            Zwróć TYLKO obiekt JSON, bez żadnego dodatkowego tekstu ani formatowania markdown.
            Jeśli pole nie jest widoczne na fakturze, ustaw je na null.
            Kwoty: liczby dziesiętne z kropką (np. "123.45"), bez waluty.
            Daty: format YYYY-MM-DD.
            NIP: bez myślników i spacji.
            """;

    private static final String USER_PROMPT = """
            Przeanalizuj tę polską fakturę VAT i zwróć dane JSON:
            {
              "number": "numer faktury",
              "sellDate": "YYYY-MM-DD",
              "invoiceDate": "YYYY-MM-DD",
              "paymentDate": "YYYY-MM-DD",
              "paymentMethod": "przelew|gotówka|płatność odroczona",
              "supplierName": "nazwa firmy",
              "supplierNip": "NIP bez myślników",
              "supplierStreet": "ulica i numer domu",
              "supplierZip": "kod pocztowy (format: XX-XXX)",
              "supplierCity": "miasto",
              "supplierAccount": "numer konta lub null",
              "supplierBank": "nazwa banku lub null",
              "items": [{"name":"nazwa pozycji","unit":"szt","quantity":"liczba","amountNet":"cena netto","amountVat":"VAT","amountGross":"brutto","vatRate":"23|8|5|0|zw"}]
            }
            """;

    @Override
    public InvoiceClaudeResponseDto extractFromS3(String s3Key) {
        log.info("Extracting invoice from S3: s3Key={}", s3Key);

        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("S3 bucket name not configured");
        }

        byte[] fileBytes = downloadFromS3(bucketName, s3Key);
        String lowerKey = s3Key.toLowerCase();
        String invoiceJson;

        if (lowerKey.endsWith(".pdf")) {
            String textContent = extractTextFromPdf(fileBytes);
            invoiceJson = callClaudeApiWithText(textContent);
        } else if (isImage(lowerKey)) {
            String mediaType = getImageMediaType(lowerKey);
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            invoiceJson = callClaudeApiWithImage(base64Content, mediaType);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + s3Key);
        }

        return parseResponse(invoiceJson);
    }

    private boolean isImage(String lowerKey) {
        return lowerKey.endsWith(".png") || lowerKey.endsWith(".jpg")
                || lowerKey.endsWith(".jpeg") || lowerKey.endsWith(".gif")
                || lowerKey.endsWith(".webp");
    }

    private String getImageMediaType(String lowerKey) {
        if (lowerKey.endsWith(".png")) return "image/png";
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) return "image/jpeg";
        if (lowerKey.endsWith(".gif")) return "image/gif";
        if (lowerKey.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private byte[] downloadFromS3(String bucketName, String s3Key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(request);
            log.debug("Downloaded file from S3: bucket={}, key={}, size={} bytes", bucketName, s3Key, s3Object.asByteArray().length);
            return s3Object.asByteArray();
        } catch (Exception e) {
            log.error("Error downloading file from S3: bucket={}, key={}", bucketName, s3Key, e);
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    private String extractTextFromPdf(byte[] pdfBytes) {
        try {
            PDDocument document = Loader.loadPDF(pdfBytes);
            try {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                log.debug("Extracted {} characters from PDF", text.length());
                return text;
            } finally {
                document.close();
            }
        } catch (Exception e) {
            log.error("Error extracting text from PDF", e);
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private String callClaudeApiWithText(String textContent) {
        try {
            String prompt = USER_PROMPT + "\n\n" + "Invoice content:\n" + textContent;
            UserMessage userMessage = new UserMessage(prompt);
            Prompt promptObj = new Prompt(userMessage);

            String response = chatModel.call(promptObj).getResult().getOutput().getContent();
            log.debug("Claude API response received (text), response length: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("Error calling Claude API with text", e);
            throw new RuntimeException("Failed to call Claude API: " + e.getMessage(), e);
        }
    }

    private String callClaudeApiWithImage(String base64Content, String mediaType) {
        try {
            Media media = new Media(MimeType.valueOf(mediaType), base64Content.getBytes());
            UserMessage userMessage = new UserMessage(USER_PROMPT, media);
            Prompt promptObj = new Prompt(userMessage);

            String response = chatModel.call(promptObj).getResult().getOutput().getContent();
            log.debug("Claude API response received (image), response length: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("Error calling Claude API with image", e);
            throw new RuntimeException("Failed to call Claude API: " + e.getMessage(), e);
        }
    }

    private InvoiceClaudeResponseDto parseResponse(String jsonResponse) {
        try {
            String cleanedJson = cleanJsonResponse(jsonResponse);
            InvoiceClaudeResponseDto result = objectMapper.readValue(cleanedJson, InvoiceClaudeResponseDto.class);
            log.info("Successfully parsed invoice response from Claude");
            return result;
        } catch (Exception e) {
            log.error("Error parsing Claude response as JSON", e);
            throw new RuntimeException("Failed to parse invoice JSON from Claude: " + e.getMessage(), e);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Response is null");
        }
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}
