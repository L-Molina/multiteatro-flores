package com.tpouade.multiteatro.exception;

/**
 * Conflicto de negocio: el recurso ya existe o la operación choca con un
 * estado actual (por ejemplo, un email ya registrado).
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
