package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.controller.ReservaController;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import com.universidad.reservaslabs.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Parte 2: la vista MVC usa el mismo Service y las mismas excepciones que la API REST. */
@SpringBootTest
@AutoConfigureMockMvc
class ReservaWebControllerTest {

    private static final String MENSAJE_SOLAPAMIENTO = "El laboratorio Lab. Web ya tiene una reserva en ese horario";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaboratorioRepository laboratorioRepo;

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private ReservaController reservaController;

    @Autowired
    private ReservaWebController reservaWebController;

    @Autowired
    private ReservaService reservaService;

    private Long labId;
    private final String dia = LocalDate.now().plusDays(30).toString();

    @BeforeEach
    void preparar() {
        reservaRepo.deleteAll();
        labId = laboratorioRepo.save(new Laboratorio(null, "Lab. Web", "Bloque D", 20, "COMPUTO")).getId();
    }

    private ResultActions enviarFormulario(String nombre, String inicio, String fin) throws Exception {
        return mockMvc.perform(post("/reservas")
                .param("laboratorio.id", labId.toString())
                .param("nombreSolicitante", nombre)
                .param("correoSolicitante", "ana@udes.edu.co")
                .param("inicio", dia + "T" + inicio)
                .param("fin", dia + "T" + fin)
                .param("motivo", "Práctica"));
    }

    @Test
    void ambosControladoresCompartenLaMismaInstanciaDeReservaService() {
        Object servicioRest = ReflectionTestUtils.getField(reservaController, "service");
        Object servicioWeb = ReflectionTestUtils.getField(reservaWebController, "service");
        assertSame(reservaService, servicioRest);
        assertSame(servicioRest, servicioWeb);
    }

    @Test
    void laListaSeRenderizaConLasReservas() throws Exception {
        enviarFormulario("Ana Torres", "09:00", "11:00");
        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista"))
                .andExpect(content().string(containsString("Ana Torres")))
                .andExpect(content().string(containsString("CONFIRMADA")));
    }

    @Test
    void elFormularioCargaLosLaboratoriosDesdeLaBase() throws Exception {
        mockMvc.perform(get("/reservas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/nueva"))
                .andExpect(model().attributeExists("laboratorios"))
                .andExpect(content().string(containsString("Lab. Web")));
    }

    @Test
    void crearDesdeElFormularioRedirigeALaListaConMensaje() throws Exception {
        enviarFormulario("Ana Torres", "09:00", "11:00")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("mensaje", "Reserva creada correctamente"));
        assertEquals(1, reservaRepo.count());
    }

    @Test
    void reservaSolapadaEnMvcMuestraElMismoMensajeQueLaApiRest() throws Exception {
        enviarFormulario("Ana Torres", "09:00", "11:00");

        // MVC: redirección al formulario con el mensaje de negocio como flash.
        enviarFormulario("Luis Gómez", "10:00", "12:00")
                .andExpect(redirectedUrl("/reservas/nueva"))
                .andExpect(flash().attribute("error", MENSAJE_SOLAPAMIENTO));

        // REST: el mismo mensaje, en JSON y con 409.
        String json = """
                {"laboratorio":{"id":%d},"nombreSolicitante":"Luis Gómez","correoSolicitante":"luis@udes.edu.co",
                 "inicio":"%sT10:00:00","fin":"%sT12:00:00"}
                """.formatted(labId, dia, dia);
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(MENSAJE_SOLAPAMIENTO));
    }

    @Test
    void reservaFueraDeHorarioEnMvcVuelveAlFormularioConElMensaje() throws Exception {
        enviarFormulario("Carlos Ruiz", "22:00", "23:00")
                .andExpect(redirectedUrl("/reservas/nueva"))
                .andExpect(flash().attribute("error", "La reserva debe estar dentro del horario de atención (07:00 - 21:00)"));
    }

    @Test
    void elFormularioConCamposInvalidosSeVuelveAMostrarConErroresPorCampo() throws Exception {
        enviarFormulario("", "09:00", "10:00")
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/nueva"))
                .andExpect(model().attributeHasFieldErrors("reserva", "nombreSolicitante"))
                .andExpect(content().string(containsString("El nombre del solicitante no puede estar vacío")));
        assertEquals(0, reservaRepo.count());
    }

    @Test
    void cancelarDesdeLaListaRedirigeConMensaje() throws Exception {
        enviarFormulario("Ana Torres", "09:00", "11:00");
        Long id = reservaRepo.findAll().get(0).getId();

        mockMvc.perform(post("/reservas/" + id + "/cancelar"))
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("mensaje", "Reserva cancelada correctamente"));
        assertEquals(EstadoReserva.CANCELADA, reservaRepo.findById(id).orElseThrow().getEstado());
    }

    @Test
    void unaCancelacionRechazadaVuelveALaListaNoAlFormulario() throws Exception {
        LocalDateTime ayer = LocalDate.now().minusDays(1).atTime(9, 0);
        Laboratorio lab = laboratorioRepo.findById(labId).orElseThrow();
        Long id = reservaRepo.save(new Reserva(null, lab, "Ana", "ana@udes.edu.co",
                ayer, ayer.plusHours(1), null, EstadoReserva.CONFIRMADA)).getId();

        mockMvc.perform(post("/reservas/" + id + "/cancelar"))
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("error", "No se puede cancelar una reserva cuyo horario de inicio ya pasó"));
    }

    @Test
    void cancelarUnaReservaInexistenteVuelveALaListaConElError() throws Exception {
        mockMvc.perform(post("/reservas/9999/cancelar"))
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("error", "Reserva no encontrada: 9999"));
    }

    @Test
    void elManejadorMvcNoInterfiereConLasRespuestasJsonDeLaApi() throws Exception {
        mockMvc.perform(delete("/api/reservas/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reserva no encontrada: 9999"));
    }
}
