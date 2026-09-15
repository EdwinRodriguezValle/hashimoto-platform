package com.hashimoto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // <-- Activamos la seguridad reactiva oficial de Spring Boot 3
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // 1. Desactivamos CSRF ya que trabajaremos con Tokens JWT
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 2. Definimos las reglas de los endpoints
                .authorizeExchange(exchanges -> exchanges
                        // Permitimos ver el estado de salud del gateway sin token
                        .pathMatchers("/actuator/**").permitAll()
                        // CUALQUIER otra petición a las APIs exige autenticación obligatoria
                        .anyExchange().authenticated()
                )

                // 3. Le ordenamos al Gateway actuar como Resource Server para validar JWTs
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

}
