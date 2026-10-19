package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservas")
public class ReservaWebController {

    private final ReservaService service;              // misma instancia que usa ReservaController (REST)
    private final LaboratorioRepository laboratorioRepo; // solo para llenar el combo del catálogo

    public ReservaWebController(ReservaService service, LaboratorioRepository laboratorioRepo) {
        this.service = service;
        this.laboratorioRepo = laboratorioRepo;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("reservas", service.findAll());
        return "reservas/lista";
    }

    @GetMapping("/nueva")
    public String formularioNueva(Model model) {
        Reserva reserva = new Reserva();
        reserva.setLaboratorio(new Laboratorio());
        model.addAttribute("reserva", reserva);
        model.addAttribute("laboratorios", laboratorioRepo.findAll());
        return "reservas/nueva";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("reserva") Reserva reserva, BindingResult errores,
                        Model model, RedirectAttributes redirect) {
        // Errores de formato del formulario (campos vacíos, correo, fecha ilegible):
        // se devuelven al mismo formulario conservando lo digitado, igual que el
        // @Valid de la API REST responde 400 antes de llegar al Service.
        if (errores.hasErrors()) {
            model.addAttribute("laboratorios", laboratorioRepo.findAll());
            return "reservas/nueva";
        }
        service.crear(reserva); // MISMA regla de solapamiento y horario que la API REST
        redirect.addFlashAttribute("mensaje", "Reserva creada correctamente");
        return "redirect:/reservas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        service.cancelar(id);
        redirect.addFlashAttribute("mensaje", "Reserva cancelada correctamente");
        return "redirect:/reservas";
    }
}
