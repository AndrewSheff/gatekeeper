package com.ashevtsov.gatekeeper.security;

import com.ashevtsov.gatekeeper.role.Permission;
import com.ashevtsov.gatekeeper.role.Role;
import com.ashevtsov.gatekeeper.user.User;
import com.ashevtsov.gatekeeper.user.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Загрузка пользователя для Spring Security.
 * Собираем permissions из ролей (с учетом иерархии) как GrantedAuthority.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final int MAX_DEPTH = 5;

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username может содержать tenantId: "admin@00000000-..."
        // но для простоты ищем по всем тенантам (в реальном продакшене — по тенанту из контекста)
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь '%s' не найден".formatted(username)));

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            collectPermissions(role, authorities, 0);
        }

        return new GatekeeperUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getTenant().getId(),
                authorities
        );
    }

    private void collectPermissions(Role role, Set<GrantedAuthority> authorities, int depth) {
        if (depth >= MAX_DEPTH) return;
        for (Permission p : role.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(p.getName()));
        }
        if (role.getParent() != null) {
            collectPermissions(role.getParent(), authorities, depth + 1);
        }
    }
}
