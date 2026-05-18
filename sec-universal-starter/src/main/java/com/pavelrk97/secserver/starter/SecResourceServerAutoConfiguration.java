package com.pavelrk97.secserver.starter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration(before = SecurityAutoConfiguration.class)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(SecProperties.class)
public class SecResourceServerAutoConfiguration {

    private JwtDecoder jwtDecoder(SecProperties props) {
        return NimbusJwtDecoder.withJwkSetUri(props.getJwt().getJwksUri()).build();
    }

    private OpaqueTokenIntrospector opaqueIntrospector(SecProperties props) {
        SecProperties.Opaque o = props.getOpaque();
        return SpringOpaqueTokenIntrospector
                .withIntrospectionUri(o.getIntrospectionUri())
                .clientId(o.getClientId())
                .clientSecret(o.getClientSecret())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(prefix = "sec.security", name = "mode", havingValue = "jwt", matchIfMissing = true)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http, SecProperties props) throws Exception {
        http
                .oauth2ResourceServer(rs -> rs.jwt(j -> j.decoder(jwtDecoder(props))))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(prefix = "sec.security", name = "mode", havingValue = "opaque")
    public SecurityFilterChain opaqueFilterChain(HttpSecurity http, SecProperties props) throws Exception {
        http
                .oauth2ResourceServer(rs -> rs.opaqueToken(o -> o.introspector(opaqueIntrospector(props))))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(prefix = "sec.security", name = "mode", havingValue = "both")
    public SecurityFilterChain bothFilterChain(HttpSecurity http, SecProperties props) throws Exception {
        http
                .oauth2ResourceServer(rs -> rs.authenticationManagerResolver(resolver(props)))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
        return http.build();
    }

    private AuthenticationManagerResolver<HttpServletRequest> resolver(SecProperties props) {
        AuthenticationManager jwtAuth =
                new ProviderManager(new JwtAuthenticationProvider(jwtDecoder(props)));
        AuthenticationManager opaqueAuth =
                new ProviderManager(new OpaqueTokenAuthenticationProvider(opaqueIntrospector(props)));
        String headerName = props.getTypeHeaderName();
        return request -> "jwt".equals(request.getHeader(headerName)) ? jwtAuth : opaqueAuth;
    }
}
