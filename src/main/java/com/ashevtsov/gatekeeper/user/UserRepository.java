package com.ashevtsov.gatekeeper.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("select u from User u where u.tenant.id = :tenantId " +
            "and (lower(u.username) like lower(concat('%', :search, '%')) " +
            "or lower(u.email) like lower(concat('%', :search, '%')))")
    Page<User> searchByTenantId(UUID tenantId, String search, Pageable pageable);
}
