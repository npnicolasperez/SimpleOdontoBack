package com.simpleodonto.shared;

import com.simpleodonto.profesional.domain.Especialidad;
import com.simpleodonto.profesional.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final EspecialidadRepository especialidadRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!especialidadRepository.existsByNombre("Odontología")) {
            especialidadRepository.save(Especialidad.builder().nombre("Odontología").build());
        }
    }
}
