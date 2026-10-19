package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas de negocio de ReservaService con repositorios JPA reales sobre H2. */
@DataJpaTest
@Import(ReservaService.class)
class ReservaServiceTest {

    @Autowired
    private ReservaService service;

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private LaboratorioRepository laboratorioRepo;

    private Laboratorio lab;
    private final LocalDate dia = LocalDate.now().plusDays(30);

    @BeforeEach
    void preparar() {
        lab = laboratorioRepo.save(new Laboratorio(null, "Lab A", "Bloque A", 20, "COMPUTO"));
    }

    private Reserva nueva(LocalDateTime inicio, LocalDateTime fin) {
        Laboratorio ref = new Laboratorio();
        ref.setId(lab.getId());
        return new Reserva(null, ref, "Ana Torres", "ana@udes.edu.co", inicio, fin, "Práctica", null);
    }

    @Test
    void crearUnaReservaValidaLaConfirmaYCargaElLaboratorio() {
        Reserva creada = service.crear(nueva(dia.atTime(9, 0), dia.atTime(11, 0)));
        assertNotNull(creada.getId());
        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        assertEquals("Lab A", creada.getLaboratorio().getNombre());
    }

    @Test
    void rechazaUnHorarioSolapadoConConflicto() {
        service.crear(nueva(dia.atTime(9, 0), dia.atTime(11, 0)));
        ReservaConflictException ex = assertThrows(ReservaConflictException.class,
                () -> service.crear(nueva(dia.atTime(10, 0), dia.atTime(12, 0))));
        assertEquals("El laboratorio Lab A ya tiene una reserva en ese horario", ex.getMessage());
        assertEquals(1, reservaRepo.count());
    }

    @Test
    void permiteReservarElHorarioDeUnaReservaCancelada() {
        Reserva primera = service.crear(nueva(dia.atTime(9, 0), dia.atTime(11, 0)));
        service.cancelar(primera.getId());
        assertDoesNotThrow(() -> service.crear(nueva(dia.atTime(9, 0), dia.atTime(11, 0))));
    }

    @Test
    void rechazaReservasFueraDelHorarioDeAtencion() {
        assertThrows(ReservaInvalidaException.class,
                () -> service.crear(nueva(dia.atTime(6, 0), dia.atTime(8, 0))));
        assertThrows(ReservaInvalidaException.class,
                () -> service.crear(nueva(dia.atTime(20, 0), dia.atTime(22, 0))));
    }

    @Test
    void rechazaUnaReservaQueCruzaLaMedianoche() {
        assertThrows(ReservaInvalidaException.class,
                () -> service.crear(nueva(dia.atTime(22, 0), dia.plusDays(1).atTime(0, 30))));
    }

    @Test
    void rechazaDuracionesFueraDelRangoPermitido() {
        assertThrows(ReservaInvalidaException.class,
                () -> service.crear(nueva(dia.atTime(9, 0), dia.atTime(9, 20))));
        assertThrows(ReservaInvalidaException.class,
                () -> service.crear(nueva(dia.atTime(9, 0), dia.atTime(12, 30))));
    }

    @Test
    void rechazaUnRangoInvertido() {
        assertThrows(ReservaInvalidaException.class,
                () -> service.crear(nueva(dia.atTime(11, 0), dia.atTime(9, 0))));
    }

    @Test
    void rechazaUnLaboratorioInexistente() {
        Reserva r = nueva(dia.atTime(9, 0), dia.atTime(10, 0));
        r.getLaboratorio().setId(999L);
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(r));
    }

    @Test
    void noSeCancelaUnaReservaCuyoInicioYaPaso() {
        LocalDateTime ayer = LocalDate.now().minusDays(1).atTime(9, 0);
        Reserva pasada = reservaRepo.save(new Reserva(null, lab, "Ana", "ana@udes.edu.co",
                ayer, ayer.plusHours(1), null, EstadoReserva.CONFIRMADA));
        assertThrows(ReservaConflictException.class, () -> service.cancelar(pasada.getId()));
        assertEquals(EstadoReserva.CONFIRMADA, reservaRepo.findById(pasada.getId()).orElseThrow().getEstado());
    }

    @Test
    void noSeCancelaDosVecesLaMismaReserva() {
        Reserva r = service.crear(nueva(dia.atTime(9, 0), dia.atTime(10, 0)));
        service.cancelar(r.getId());
        assertThrows(ReservaConflictException.class, () -> service.cancelar(r.getId()));
    }

    @Test
    void consultarReservasDeUnLaboratorioInexistenteEsNoEncontrado() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.findByLaboratorio(999L));
    }
}
