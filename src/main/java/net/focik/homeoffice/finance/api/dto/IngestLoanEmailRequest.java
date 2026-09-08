package net.focik.homeoffice.finance.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Payload wysyłany przez workflow n8n na /internal/loan-proposals/ingest. n8n dostarcza
 * wyłącznie surową treść maila - żadnej logiki biznesowej po jego stronie.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = {"textBody", "htmlBody", "rawEmlBase64"})
public class IngestLoanEmailRequest {
    private String messageId;
    private String from;
    private String subject;
    private String textBody;
    private String htmlBody;
    /** Surowy .eml zakodowany w Base64, do archiwizacji w S3. Opcjonalne. */
    private String rawEmlBase64;
}
