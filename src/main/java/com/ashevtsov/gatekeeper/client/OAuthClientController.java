package com.ashevtsov.gatekeeper.client;

import com.ashevtsov.gatekeeper.client.dto.ClientResponse;
import com.ashevtsov.gatekeeper.client.dto.RegisterClientRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Управление OAuth2-клиентами через REST API
 */
@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "OAuth2 Clients", description = "Управление OAuth2-клиентами")
public class OAuthClientController {

    private final OAuthClientService clientService;

    public OAuthClientController(OAuthClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Зарегистрировать OAuth2-клиент (secret показывается один раз)")
    public ClientResponse register(@Valid @RequestBody RegisterClientRequest request) {
        return clientService.register(request);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Получить клиент по clientId")
    public ClientResponse getByClientId(@PathVariable String clientId) {
        return clientService.getByClientId(clientId);
    }
}
