package com.ashevtsov.gatekeeper.common.exception;

/**
 * Базовое исключение приложения — от него наследуются все доменные ошибки
 */
public class GatekeeperException extends RuntimeException {

    public GatekeeperException(String message) {
        super(message);
    }

    public GatekeeperException(String message, Throwable cause) {
        super(message, cause);
    }
}
