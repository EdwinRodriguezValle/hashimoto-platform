package com.hashimoto.patient_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
            // 1. Extraer el objeto 'realm_access' del token
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if(realmAccess ==null||realmAccess.isEmpty())

            {
                return Collections.emptyList();
            }

            // 2. Extraer la lista de 'roles' de adentro de 'realm_access'
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

        if(roles ==null)

            {
                return Collections.emptyList();
            }

            // 3. Convertirlos al formato que entiende Spring Security (Asegurando el prefijo ROLE_)
        return roles.stream()
                .

            map(roleName ->roleName.startsWith("ROLE_")?roleName :"ROLE_"+roleName.toUpperCase())
                    .

            map(SimpleGrantedAuthority::new)
                .

            collect(Collectors.toList());
        }
    }