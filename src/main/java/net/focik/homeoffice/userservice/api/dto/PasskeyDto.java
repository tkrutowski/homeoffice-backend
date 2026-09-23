package net.focik.homeoffice.userservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasskeyDto {

    //base64url credentialId - dokladnie ten string trafia w {id} do DELETE /webauthn/register/{id}
    private String id;
    private String label;
    private Instant created;
    private Instant lastUsed;
}
