package com.ashevtsov.gatekeeper.common.exception;

/**
 * Конфликт данных (дубликат, нарушение уникальности) — маппится в 409
 */
public class ConflictException extends GatekeeperException {

    public ConflictException(String message) {
        super(message);
    }
}
