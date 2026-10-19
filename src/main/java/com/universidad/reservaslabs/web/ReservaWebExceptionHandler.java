package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Restringido con assignableTypes a ReservaWebController: no compite con
// GlobalRestExceptionHandler, que ya está restringido a los @RestController
// de la Parte 1. Mismas excepciones de dominio, presentación distinta.
@ControllerAdvice(assignableTypes = ReservaWebController.class)
public class ReservaWebExceptionHandler {

    /** Horario ocupado o reserva que ya no se puede cancelar (409 en la API REST). */
    @ExceptionHandler(ReservaConflictException.class)
    public String conflicto(ReservaConflictException ex, HandlerMethod origen, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return volverA(origen);
    }

    /** Horario de atención, duración o rango inválidos (400 en la API REST). */
    @ExceptionHandler(ReservaInvalidaException.class)
    public String reservaInvalida(ReservaInvalidaException ex, HandlerMethod origen, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return volverA(origen);
    }

    /** Laboratorio o reserva inexistente (404 en la API REST). */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String noEncontrado(RecursoNoEncontradoException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return "redirect:/reservas";
    }

    /**
     * El error se muestra en la página desde donde el usuario actuó: si falló
     * la creación, vuelve al formulario; si falló una cancelación, a la lista.
     */
    private String volverA(HandlerMethod origen) {
        return "crear".equals(origen.getMethod().getName()) ? "redirect:/reservas/nueva" : "redirect:/reservas";
    }
}
