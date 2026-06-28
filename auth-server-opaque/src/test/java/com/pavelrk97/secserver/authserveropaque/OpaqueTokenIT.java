package com.pavelrk97.secserver.authserveropaque;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpaqueTokenIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void clientCredentials_returnsOpaqueToken_notJwt() {
        String token = obtainToken();

        // opaque token is a random string, NOT three dot-separated JWT sections
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(1);
    }

    @Test
    void introspect_validToken_isActive() {
        String token = obtainToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("client", "secret");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);

        ResponseEntity<Map> resp = rest.postForEntity(
                "/oauth2/introspect", new HttpEntity<>(form, headers), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("active")).isEqualTo(true);
        assertThat(resp.getBody().get("sub")).isEqualTo("client");
    }

    @Test
    void introspect_garbageToken_isInactive() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("client", "secret");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", "totally-not-a-real-token");

        ResponseEntity<Map> resp = rest.postForEntity(
                "/oauth2/introspect", new HttpEntity<>(form, headers), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("active")).isEqualTo(false);
    }

    private String obtainToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("client", "secret");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "read");

        ResponseEntity<Map> resp = rest.postForEntity(
                "/oauth2/token", new HttpEntity<>(form, headers), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resp.getBody().get("access_token");
    }
}
