package com.simpleodonto.consulta.repository;

import com.simpleodonto.consulta.domain.Consulta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    List<Consulta> findByProfesionalId(Long profesionalId, Pageable pageable);
    List<Consulta> findByPacienteIdAndProfesionalId(Long pacienteId, Long profesionalId, Sort sort);
    Optional<Consulta> findByIdAndProfesionalId(Long id, Long profesionalId);
}
