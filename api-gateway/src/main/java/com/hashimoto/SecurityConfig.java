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

    // 🔒 EL ESCUDO REACTIVO: Lee las llaves en background sin colapsar el arranque
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(this.issuerUri));
        return decoder;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 👈 Forzar a que esta regla se evalúe de primero
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        if (this.securityDisabled) {
//            // TEMPORARY DEV BYPASS: removes Keycloak from the local path while debugging the platform.
//            http
//                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                    .authorizeExchange(exchanges -> exchanges
//                            .pathMatchers("/**").permitAll()
//                    );
//            return http.build();
//        }

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/prometheus", "/error").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // 🚀 CORREGIDO: Le forzamos a usar nuestro escudo decodificador
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
                );

//               .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

}
