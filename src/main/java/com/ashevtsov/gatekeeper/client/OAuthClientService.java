package com.ashevtsov.gatekeeper.client;

import com.ashevtsov.gatekeeper.client.dto.ClientResponse;
import com.ashevtsov.gatekeeper.client.dto.RegisterClientRequest;
import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Обертка над RegisteredClientRepository — CRUD для OAuth2-клиентов через REST API.
 * Secret показывается только один раз при создании.
 */
@Service
public class OAuthClientService {

    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuthClientService(RegisteredClientRepository clientRepository,
                               PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ClientResponse register(RegisterClientRequest request) {
        String rawSecret = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString().substring(0, 8);

        var builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(rawSecret))
                .clientName(request.clientName())
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);

        for (String gt : request.grantTypes()) {
            builder.authorizationGrantType(new AuthorizationGrantType(gt));
        }

        if (request.redirectUris() != null) {
            request.redirectUris().forEach(builder::redirectUri);
        }

        request.scopes().forEach(builder::scope);

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(request.accessTokenTtlSeconds()))
                .refreshTokenTimeToLive(Duration.ofSeconds(request.refreshTokenTtlSeconds()))
                .build());

        var registeredClient = builder.build();
        clientRepository.save(registeredClient);

        return toResponse(registeredClient, rawSecret);
    }

    public ClientResponse getByClientId(String clientId) {
        var client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("OAuth2 Client", clientId);
        }
        return toResponse(client, null);
    }

    private ClientResponse toResponse(RegisteredClient client, String rawSecret) {
        return new ClientResponse(
                client.getId(),
                client.getClientId(),
                rawSecret,
                client.getClientName(),
                client.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue).collect(Collectors.toSet()),
                Set.copyOf(client.getRedirectUris()),
                Set.copyOf(client.getScopes()),
                client.getClientIdIssuedAt()
        );
    }
}
