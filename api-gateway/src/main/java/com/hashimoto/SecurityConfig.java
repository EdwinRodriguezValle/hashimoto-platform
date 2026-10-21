package com.hashimoto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.beans.factory.annotation.Value; // 🚀 Requerido
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder; // 🚀 Requerido
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder; // 🚀 Requerido
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;


@Configuration
@EnableWebFluxSecurity // <-- Activamos la seguridad reactiva oficial de Spring Boot 3
public class SecurityConfig {

    // TEMPORARY DEV BYPASS: set app.security.disabled=false to restore Keycloak later.
    // @Value("${app.security.disabled:true}")
//    private boolean securityDisabled;

    // Inyectamos la URL interna leída desde tu platform-env.yaml de K8s
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${app.security.api-audience:api-gateway-client}")
    private String apiAudience;

    // 🔒 EL ESCUDO REACTIVO: Lee las llaves en background sin colapsar el arranque
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null
                && jwt.getAudience().contains(this.apiAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Token is not intended for this API", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(this.issuerUri), audienceValidator));
        return decoder;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 👈 Forzar a que esta regla se evalúe de primero
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/error").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // 🚀 CORREGIDO: Le forzamos a usar nuestro escudo decodificador
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
                );

        return http.build();
    }

}
