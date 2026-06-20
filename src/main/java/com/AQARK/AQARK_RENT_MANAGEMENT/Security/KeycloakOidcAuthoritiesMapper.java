package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps Keycloak realm roles to Spring authorities for the browser/session (oauth2Login) chain.
 *
 * <p>Realm roles are already named {@code ROLE_USER} / {@code ROLE_ADMIN} / {@code ROLE_ACTUATOR},
 * so they are used as authorities verbatim (no extra {@code ROLE_} prefix). This mirrors
 * {@link KeycloakJwtRolesConverter} used by the Bearer-token chain, so both authentication
 * paths produce identical authorities.
 */
@Component
public class KeycloakOidcAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> authorities) {

        List<GrantedAuthority> mapped = new ArrayList<>();

        for (GrantedAuthority authority : authorities) {
            Map<String, Object> claims = null;
            if (authority instanceof OidcUserAuthority oidc) {
                claims = oidc.getIdToken().getClaims();
            } else if (authority instanceof OAuth2UserAuthority oauth2) {
                claims = oauth2.getAttributes();
            }

            if (claims != null) {
                extractRealmRoles(claims).forEach(role ->
                        mapped.add(new SimpleGrantedAuthority(role)));
            }
        }

        return mapped;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get(REALM_ACCESS);
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Collections.emptyList();
        }
        Object roles = ((Map<String, Object>) realmAccessMap).get(ROLES);
        if (!(roles instanceof List<?>)) {
            return Collections.emptyList();
        }
        return (List<String>) roles;
    }
}
