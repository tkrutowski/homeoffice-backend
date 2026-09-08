package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Surowa treść e-maila dostarczona przez adapter wejściowy (n8n). To jedyne, co ingest
 * przyjmuje od n8n - żadnej logiki biznesowej po stronie workflow.
 */
@Builder
@AllArgsConstructor
@Getter
@ToString(exclude = {"textBody", "htmlBody", "rawEml"})
public class RawLoanEmail {
    /** Nagłówek Message-ID e-maila - klucz deduplikacji. */
    private String messageId;
    private String from;
    private String subject;
    /** Treść tekstowa (text/plain), jeśli n8n ją dostarczyło. */
    private String textBody;
    /** Treść HTML (text/html), używana gdy textBody jest puste - zdzierana do tekstu przez jsoup. */
    private String htmlBody;
    /** Surowy plik .eml (MIME), do zapisu w S3 dla audytu. Może być null. */
    private byte[] rawEml;
}
