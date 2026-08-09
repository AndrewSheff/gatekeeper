package com.ashevtsov.gatekeeper.common.exception;

/**
 * Ресурс не найден — маппится в 404
 */
public class NotFoundException extends GatekeeperException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entityName, Object id) {
        super("%s с id '%s' не найден".formatted(entityName, id));
    }
}
