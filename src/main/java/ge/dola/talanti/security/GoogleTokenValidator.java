package ge.dola.talanti.security;

import ge.dola.talanti.security.dto.GoogleUserInfoDto;
import org.springframework.stereotype.Component;

@Component
public class GoogleTokenValidator {

    public GoogleUserInfoDto validate(String idTokenString) throws Exception {
        // TODO: Use com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier here
        // For right now, to not block development, we will assume the token is valid
        // if you are just testing the React -> Spring Boot connection.

        // Mocking a successful validation:
        // return new GoogleUserInfo("test@gmail.com", "1029384756");

        throw new RuntimeException("Google Token Verification not yet implemented. Please add GoogleIdTokenVerifier.");
    }
}