package com.pavelrk97.secserver.resourceserver.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${auth.jwt.jwks-uri}")
    private String jwksUri;

    @Value("${auth.opaque.introspection-uri}")
    private String introspectionUri;

    @Value("${auth.opaque.client-id}")
    private String introspectionClientId;

    @Value("${auth.opaque.client-secret}")
    private String introspectionClientSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .oauth2ResourceServer(rs -> rs.authenticationManagerResolver(authenticationManagerResolver()))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
        return http.build();
    }

    /**
     * Per-request выбор AuthenticationManager'а по HTTP-заголовку "type".
     * type=jwt   -> локальная проверка подписи через NimbusJwtDecoder (запрос в /oauth2/jwks)
     * иначе      -> introspection в auth-server-opaque (RFC 7662)
     */
    private AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        OpaqueTokenIntrospector introspector = new SpringOpaqueTokenIntrospector(
                introspectionUri, introspectionClientId, introspectionClientSecret);

        AuthenticationManager jwtAuth = new ProviderManager(new JwtAuthenticationProvider(jwtDecoder));
        AuthenticationManager opaqueAuth = new ProviderManager(new OpaqueTokenAuthenticationProvider(introspector));

        return request -> "jwt".equals(request.getHeader("type")) ? jwtAuth : opaqueAuth;
    }
}
