package com.simpleodonto.analisis.repository;

import com.simpleodonto.analisis.domain.Analisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalisisRepository extends JpaRepository<Analisis, Long> {
    List<Analisis> findByProfesionalIdOrderByLastUpdatedDesc(Long profesionalId);
    List<Analisis> findByProfesionalIdAndPacienteIdOrderByLastUpdatedDesc(Long profesionalId, Long pacienteId);
    Optional<Analisis> findByIdAndProfesionalId(Long id, Long profesionalId);
}
