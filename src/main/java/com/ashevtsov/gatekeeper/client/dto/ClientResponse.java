package com.ashevtsov.gatekeeper.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientResponse(
        String id,
        String clientId,
        String clientSecret,   // показывается только при создании
        String clientName,
        Set<String> grantTypes,
        Set<String> redirectUris,
        Set<String> scopes,
        Instant clientIdIssuedAt
) {
}
