package com.pavelrk97.secserver.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sec.security")
public class SecProperties {

    public enum Mode { JWT, OPAQUE, BOTH }

    private Mode mode = Mode.JWT;

    private String typeHeaderName = "type";

    private final Jwt jwt = new Jwt();
    private final Opaque opaque = new Opaque();

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public String getTypeHeaderName() { return typeHeaderName; }
    public void setTypeHeaderName(String typeHeaderName) { this.typeHeaderName = typeHeaderName; }

    public Jwt getJwt() { return jwt; }
    public Opaque getOpaque() { return opaque; }

    public static class Jwt {
        private String jwksUri;
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
    }

    public static class Opaque {
        private String introspectionUri;
        private String clientId;
        private String clientSecret;
        public String getIntrospectionUri() { return introspectionUri; }
        public void setIntrospectionUri(String introspectionUri) { this.introspectionUri = introspectionUri; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }
}
