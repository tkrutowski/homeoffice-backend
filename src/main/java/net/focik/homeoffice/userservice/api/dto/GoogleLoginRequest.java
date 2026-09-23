package net.focik.homeoffice.userservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginRequest {

    //ID token (JWT) zwrocony przez Google Identity Services / natywny Google Sign-In SDK
    private String idToken;
}
