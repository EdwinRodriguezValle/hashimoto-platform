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


@Configuration
@EnableWebFluxSecurity // <-- Activamos la seguridad reactiva oficial de Spring Boot 3
public class SecurityConfig {

    // Inyectamos la URL interna leída desde tu platform-env.yaml de K8s
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    // 🔒 EL ESCUDO REACTIVO: Lee las llaves en background sin colapsar el arranque
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 👈 Forzar a que esta regla se evalúe de primero
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
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

//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

}
