package com.universidad.reservaslabs.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;

// Restringido a los @RestController: en la Parte 2 convive con el manejador
// del controlador MVC sin que ninguno intercepte los errores del otro.
@RestControllerAdvice(annotations = RestController.class)
public class GlobalRestExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> notFound(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ReservaConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(ReservaConflictException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ReservaInvalidaException.class)
    public ResponseEntity<Map<String, String>> reservaInvalida(ReservaInvalidaException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validationError(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ResponseEntity.status(400).body(Map.of("errores", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(400).body(Map.of("error",
            "El cuerpo de la petición no es un JSON válido o tiene un formato de fecha incorrecto "
                + "(use yyyy-MM-ddTHH:mm:ss)"));
    }
}
