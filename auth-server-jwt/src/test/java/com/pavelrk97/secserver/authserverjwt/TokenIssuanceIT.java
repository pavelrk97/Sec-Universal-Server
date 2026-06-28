package com.pavelrk97.secserver.authserverjwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenIssuanceIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void clientCredentials_returnsJwt_withCustomPriorityClaim() {
        ResponseEntity<Map> resp = requestToken();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        String accessToken = (String) resp.getBody().get("access_token");
        assertThat(accessToken).isNotBlank();
        assertThat(resp.getBody().get("token_type")).isEqualTo("Bearer");

        String[] parts = accessToken.split("\\.");
        assertThat(parts).hasSize(3);

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload)
                .contains("\"priority\":\"HIGH\"")   // our OAuth2TokenCustomizer
                .contains("\"scope\"")
                .contains("read");
    }

    @Test
    void discoveryEndpoint_isAvailable() {
        ResponseEntity<Map> resp = rest.getForEntity(
                "/.well-known/openid-configuration", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKeys(
                "issuer", "token_endpoint", "jwks_uri", "introspection_endpoint");
    }

    private ResponseEntity<Map> requestToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("client", "secret");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "read");

        return rest.postForEntity("/oauth2/token", new HttpEntity<>(form, headers), Map.class);
    }
}
