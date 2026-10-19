package com.universidad.reservaslabs.exception;

/** El laboratorio o la reserva referenciados no existen. Se presenta como 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) { super(mensaje); }
}
