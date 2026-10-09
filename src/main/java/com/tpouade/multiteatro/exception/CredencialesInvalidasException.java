package com.tpouade.multiteatro.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}