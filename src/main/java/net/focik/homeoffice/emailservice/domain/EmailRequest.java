package net.focik.homeoffice.emailservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Email request model containing template and variables for sending templated emails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> templateVariables;
}
