package com.ashevtsov.gatekeeper.client;

import com.ashevtsov.gatekeeper.client.dto.RegisterClientRequest;
import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты OAuth2-клиент сервиса — регистрация и поиск
 */
@ExtendWith(MockitoExtension.class)
class OAuthClientServiceTest {

    @Mock
    RegisteredClientRepository clientRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    OAuthClientService oAuthClientService;

    @BeforeEach
    void setUp() {
        oAuthClientService = new OAuthClientService(clientRepository, passwordEncoder);
    }

    @Test
    void register_createsClientWithSecret() {
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");

        var request = new RegisterClientRequest(
                "Test App", Set.of("client_credentials"), null, Set.of("read"), 3600L, 86400L
        );

        var response = oAuthClientService.register(request);

        assertNotNull(response.clientId());
        assertNotNull(response.clientSecret()); // секрет показывается при создании
        assertEquals("Test App", response.clientName());
        assertTrue(response.grantTypes().contains("client_credentials"));
        assertTrue(response.scopes().contains("read"));

        verify(clientRepository).save(any(RegisteredClient.class));
    }

    @Test
    void getByClientId_notFound_throwsNotFoundException() {
        when(clientRepository.findByClientId("nonexistent")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> oAuthClientService.getByClientId("nonexistent"));
    }
}
