package com.ashevtsov.gatekeeper.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Расширенный UserDetails — хранит tenantId и userId для JWT claims
 */
public class GatekeeperUserDetails implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final UUID tenantId;
    private final Set<GrantedAuthority> authorities;

    public GatekeeperUserDetails(UUID userId, String username, String password,
                                  boolean enabled, UUID tenantId,
                                  Set<GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.authorities = authorities;
    }

    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return enabled; }
}
