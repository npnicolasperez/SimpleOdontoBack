package com.simpleodonto.shared;

import com.simpleodonto.finanzas.service.MedioPagoService;
import com.simpleodonto.profesional.domain.Especialidad;
import com.simpleodonto.profesional.repository.EspecialidadRepository;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final EspecialidadRepository especialidadRepository;
    private final ProfesionalRepository  profesionalRepository;
    private final MedioPagoService       medioPagoService;

    @Override
    public void run(ApplicationArguments args) {
        if (!especialidadRepository.existsByNombre("Odontología")) {
            especialidadRepository.save(Especialidad.builder().nombre("Odontología").build());
        }
        // Idempotente — asegura que cada profesional existente tenga "Efectivo" y "Transferencia"
        // como medios de sistema. Corre cada boot: si ya están, no toca nada.
        profesionalRepository.findAll().forEach(medioPagoService::bootstrapSistema);
    }
}
