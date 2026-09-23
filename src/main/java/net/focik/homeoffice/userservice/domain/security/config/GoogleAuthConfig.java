package net.focik.homeoffice.userservice.domain.security.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GoogleAuthConfig {

    //lista Client ID z Google Cloud Console dozwolonych jako "audience" tokena (web, ew. Android/iOS) -
    //bez tego GoogleIdTokenVerifier przyjalby token wystawiony dla DOWOLNEJ aplikacji Google, nie tylko naszej
    @Value("${google.oauth.client-ids}")
    private String clientIdsProperty;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        List<String> clientIds = Arrays.stream(clientIdsProperty.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
        if (clientIds.isEmpty()) {
            throw new IllegalStateException("google.oauth.client-ids nie moze byc puste - wymagany co najmniej jeden Client ID");
        }
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build();
    }
}
