package com.ashevtsov.gatekeeper.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Добавляет tenantId, roles и permissions в JWT claims.
 * Downstream-сервисы могут авторизовать без обращения к GateKeeper.
 */
@Component
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        var principal = context.getPrincipal();
        if (principal == null) return;

        var authentication = principal;
        if (authentication.getPrincipal() instanceof GatekeeperUserDetails userDetails) {
            context.getClaims().claim("tenant_id", userDetails.getTenantId().toString());
            context.getClaims().claim("user_id", userDetails.getUserId().toString());

            var roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .collect(Collectors.toList());
            context.getClaims().claim("roles", roles);

            var permissions = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> !a.startsWith("ROLE_"))
                    .collect(Collectors.toList());
            context.getClaims().claim("permissions", permissions);
        }
    }
}
