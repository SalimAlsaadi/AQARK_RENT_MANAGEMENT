package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class KeycloakJwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";

    @Value("${keycloak.client-id:spring-boot-app}")
    private String clientId;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Stream<String> realmRoles = extractRealmRoles(jwt);
        Stream<String> clientRoles = extractClientRoles(jwt);

        // Realm roles are already named ROLE_USER / ROLE_ADMIN / ROLE_ACTUATOR, so they are
        // used as authorities verbatim. Adding a ROLE_ prefix here would produce
        // ROLE_ROLE_ADMIN and break hasRole("ADMIN").
        return Stream.concat(realmRoles, clientRoles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Stream<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess == null) {
            return Stream.empty();
        }
        List<String> roles = (List<String>) realmAccess.getOrDefault(ROLES, Collections.emptyList());
        return roles.stream();
    }

    @SuppressWarnings("unchecked")
    private Stream<String> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
        if (resourceAccess == null) {
            return Stream.empty();
        }
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        if (clientAccess == null) {
            return Stream.empty();
        }
        List<String> roles = (List<String>) clientAccess.getOrDefault(ROLES, Collections.emptyList());
        return roles.stream();
    }
}
