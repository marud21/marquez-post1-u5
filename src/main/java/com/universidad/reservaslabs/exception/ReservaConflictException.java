package com.universidad.reservaslabs.exception;

/**
 * La reserva está bien formada, pero choca con el estado actual del sistema:
 * el horario ya está ocupado o la reserva ya no admite la operación pedida.
 * Se presenta como 409 Conflict en la API REST.
 */
public class ReservaConflictException extends RuntimeException {
    public ReservaConflictException(String mensaje) { super(mensaje); }
}
