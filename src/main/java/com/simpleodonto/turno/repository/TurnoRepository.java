package com.simpleodonto.turno.repository;

import com.simpleodonto.turno.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByProfesionalIdAndFechaHoraBetweenOrderByFechaHora(Long profesionalId, LocalDateTime desde, LocalDateTime hasta);
    List<Turno> findByProfesionalIdAndPacienteIdOrderByFechaHoraDesc(Long profesionalId, Long pacienteId);
    Optional<Turno> findByGoogleEventId(String googleEventId);
    Optional<Turno> findByIdAndProfesionalId(Long id, Long profesionalId);
}
