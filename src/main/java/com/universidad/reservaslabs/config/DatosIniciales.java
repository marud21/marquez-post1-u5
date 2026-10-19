package com.universidad.reservaslabs.config;

import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga un catálogo mínimo de laboratorios al arrancar, para que la base H2 en
 * memoria (create-drop) no inicie vacía y el formulario MVC tenga opciones.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final LaboratorioRepository laboratorioRepo;

    public DatosIniciales(LaboratorioRepository laboratorioRepo) {
        this.laboratorioRepo = laboratorioRepo;
    }

    @Override
    public void run(String... args) {
        if (laboratorioRepo.count() > 0) {
            return;
        }
        laboratorioRepo.save(new Laboratorio(null, "Lab. Cómputo 1", "Bloque A, piso 1", 25, "COMPUTO"));
        laboratorioRepo.save(new Laboratorio(null, "Lab. Redes", "Bloque B, piso 3", 20, "REDES"));
        laboratorioRepo.save(new Laboratorio(null, "Lab. Electrónica", "Bloque C, piso 2", 18, "ELECTRONICA"));
    }
}
