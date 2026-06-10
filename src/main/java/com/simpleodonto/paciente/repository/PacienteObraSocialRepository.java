package com.simpleodonto.paciente.repository;

import com.simpleodonto.paciente.domain.PacienteObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteObraSocialRepository extends JpaRepository<PacienteObraSocial, Long> {
    List<PacienteObraSocial> findByPacienteIdOrderByOrdenAsc(Long pacienteId);
    boolean existsByObraSocialId(Long obraSocialId);
    void deleteByPacienteId(Long pacienteId);
}
