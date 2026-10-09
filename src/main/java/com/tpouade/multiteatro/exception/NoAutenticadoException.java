package com.tpouade.multiteatro.exception;

public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
