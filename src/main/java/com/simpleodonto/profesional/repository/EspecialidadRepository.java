package com.simpleodonto.profesional.repository;

import com.simpleodonto.profesional.domain.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
    boolean existsByNombre(String nombre);
}
