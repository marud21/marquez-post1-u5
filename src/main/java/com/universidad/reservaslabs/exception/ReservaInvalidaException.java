package com.universidad.reservaslabs.exception;

/**
 * La reserva viola una regla de negocio que depende solo de sus propios datos
 * (rango de fechas, duración, horario de atención), sin importar qué otras
 * reservas existan. Reintentarla igual nunca funcionará: se presenta como
 * 400 Bad Request, a diferencia de {@link ReservaConflictException} (409).
 */
public class ReservaInvalidaException extends RuntimeException {
    public ReservaInvalidaException(String mensaje) { super(mensaje); }
}
