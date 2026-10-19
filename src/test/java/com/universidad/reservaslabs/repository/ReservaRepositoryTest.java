package com.universidad.reservaslabs.repository;

import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica la consulta JPQL de solapamientos contra H2 real. */
@DataJpaTest
class ReservaRepositoryTest {

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private LaboratorioRepository laboratorioRepo;

    private Laboratorio lab;
    private Laboratorio otroLab;
    private final LocalDate dia = LocalDate.now().plusDays(30);

    @BeforeEach
    void preparar() {
        lab = laboratorioRepo.save(new Laboratorio(null, "Lab A", "Bloque A", 20, "COMPUTO"));
        otroLab = laboratorioRepo.save(new Laboratorio(null, "Lab B", "Bloque B", 20, "REDES"));
        // Reserva existente de 09:00 a 11:00
        reservaRepo.save(reserva(lab, 9, 11, EstadoReserva.CONFIRMADA));
    }

    private Reserva reserva(Laboratorio l, int horaInicio, int horaFin, EstadoReserva estado) {
        return new Reserva(null, l, "Ana", "ana@udes.edu.co",
                dia.atTime(horaInicio, 0), dia.atTime(horaFin, 0), null, estado);
    }

    private LocalDateTime hora(int h, int m) {
        return dia.atTime(h, m);
    }

    @Test
    void detectaUnRangoQueSeSolapaParcialmente() {
        assertEquals(1, reservaRepo.buscarSolapamientos(lab.getId(), hora(10, 0), hora(12, 0)).size());
    }

    @Test
    void detectaUnRangoContenidoDentroDeOtro() {
        assertEquals(1, reservaRepo.buscarSolapamientos(lab.getId(), hora(9, 30), hora(10, 30)).size());
    }

    @Test
    void unRangoContiguoNoSeConsideraSolapado() {
        // [09:00, 11:00) y [11:00, 12:00) solo se tocan en el borde.
        assertTrue(reservaRepo.buscarSolapamientos(lab.getId(), hora(11, 0), hora(12, 0)).isEmpty());
        assertTrue(reservaRepo.buscarSolapamientos(lab.getId(), hora(8, 0), hora(9, 0)).isEmpty());
    }

    @Test
    void lasReservasCanceladasNoBloqueanElHorario() {
        reservaRepo.save(reserva(lab, 14, 16, EstadoReserva.CANCELADA));
        assertTrue(reservaRepo.buscarSolapamientos(lab.getId(), hora(14, 0), hora(15, 0)).isEmpty());
    }

    @Test
    void unaReservaDeOtroLaboratorioNoBloqueaElHorario() {
        assertTrue(reservaRepo.buscarSolapamientos(otroLab.getId(), hora(9, 0), hora(11, 0)).isEmpty());
    }
}
