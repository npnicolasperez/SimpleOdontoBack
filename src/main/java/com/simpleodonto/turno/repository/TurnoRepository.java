package com.simpleodonto.turno.repository;

import com.simpleodonto.turno.domain.EstadoTurno;
import com.simpleodonto.turno.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    @Query("""
        SELECT COUNT(DISTINCT t.paciente.id) FROM Turno t
        WHERE t.profesional.id = :profId
          AND t.paciente IS NOT NULL
          AND t.fechaHora >= :ahora
        """)
    long countPacientesConTurnoProximo(
            @Param("profId") Long          profId,
            @Param("ahora")  LocalDateTime ahora
    );
    List<Turno> findByProfesionalIdAndFechaHoraBetweenOrderByFechaHora(Long profesionalId, LocalDateTime desde, LocalDateTime hasta);
    List<Turno> findByProfesionalIdAndPacienteIdOrderByFechaHoraDesc(Long profesionalId, Long pacienteId);
    Optional<Turno> findByGoogleEventId(String googleEventId);
    Optional<Turno> findByIdAndProfesionalId(Long id, Long profesionalId);
}
