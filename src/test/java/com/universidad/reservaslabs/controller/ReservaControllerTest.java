package com.universidad.reservaslabs.controller;

import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Checkpoints de la Parte 1 sobre la API REST con el contexto completo de Spring. */
@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaboratorioRepository laboratorioRepo;

    @Autowired
    private ReservaRepository reservaRepo;

    private Long labId;
    private final String dia = LocalDate.now().plusDays(30).toString();

    @BeforeEach
    void preparar() {
        reservaRepo.deleteAll();
        labId = laboratorioRepo.save(new Laboratorio(null, "Lab. Cómputo 3", "Bloque B, piso 2", 30, "COMPUTO")).getId();
    }

    private ResultActions crearReserva(String nombre, String inicio, String fin) throws Exception {
        String json = """
                {"laboratorio":{"id":%d},"nombreSolicitante":"%s","correoSolicitante":"ana@udes.edu.co",
                 "inicio":"%sT%s","fin":"%sT%s"}
                """.formatted(labId, nombre, dia, inicio, dia, fin);
        return mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void crearLaboratorioResponde201() throws Exception {
        mockMvc.perform(post("/api/laboratorios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Lab. Redes 2\",\"ubicacion\":\"Bloque C\",\"capacidad\":15,\"tipo\":\"REDES\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void crearLaboratorioInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/api/laboratorios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"ubicacion\":\"Bloque C\",\"capacidad\":0,\"tipo\":\"REDES\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void reservaEnHorarioLibreResponde201() throws Exception {
        crearReserva("Ana Torres", "09:00:00", "11:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.laboratorio.nombre").value("Lab. Cómputo 3"));
    }

    @Test
    void reservaSolapadaResponde409ConMensaje() throws Exception {
        crearReserva("Ana Torres", "09:00:00", "11:00:00").andExpect(status().isCreated());
        crearReserva("Luis Gómez", "10:00:00", "12:00:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El laboratorio Lab. Cómputo 3 ya tiene una reserva en ese horario"));
    }

    @Test
    void reservaFueraDelHorarioDeAtencionResponde400() throws Exception {
        crearReserva("Carlos Ruiz", "22:00:00", "23:00:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La reserva debe estar dentro del horario de atención (07:00 - 21:00)"));
    }

    @Test
    void reservaConDuracionInvalidaResponde400() throws Exception {
        crearReserva("Carlos Ruiz", "09:00:00", "09:15:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La duración de la reserva debe estar entre 30 minutos y 3 horas"));
    }

    @Test
    void reservaConDatosMalFormadosResponde400ConErroresPorCampo() throws Exception {
        crearReserva("", "09:00:00", "10:00:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("nombreSolicitante: El nombre del solicitante no puede estar vacío")));
    }

    @Test
    void reservaEnLaboratorioInexistenteResponde404() throws Exception {
        labId = 9999L;
        crearReserva("Ana Torres", "09:00:00", "10:00:00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Laboratorio no encontrado: 9999"));
    }

    @Test
    void cancelarResponde204YLiberaElHorario() throws Exception {
        String respuesta = crearReserva("Ana Torres", "09:00:00", "11:00:00")
                .andReturn().getResponse().getContentAsString();
        String id = respuesta.replaceAll(".*?\"id\":(\\d+).*", "$1");

        mockMvc.perform(delete("/api/reservas/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/reservas/" + id)).andExpect(jsonPath("$.estado").value("CANCELADA"));
        crearReserva("Luis Gómez", "10:00:00", "12:00:00").andExpect(status().isCreated());
    }

    @Test
    void cancelarUnaReservaInexistenteResponde404() throws Exception {
        mockMvc.perform(delete("/api/reservas/9999")).andExpect(status().isNotFound());
    }

    @Test
    void listarReservasDeUnLaboratorio() throws Exception {
        crearReserva("Ana Torres", "09:00:00", "11:00:00");
        mockMvc.perform(get("/api/reservas/laboratorio/" + labId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
