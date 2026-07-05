package com.simpleodonto.turno.repository;

import com.simpleodonto.turno.domain.EstadoTurno;
import com.simpleodonto.turno.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simpleodonto.turno.domain.EstadoTurno;
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
    Optional<Turno> findFirstByProfesionalIdAndFechaHoraAfterOrderByFechaHora(Long profesionalId, LocalDateTime ahora);
    long countByProfesionalIdAndFechaHoraBetweenAndEstado(Long profesionalId, LocalDateTime desde, LocalDateTime hasta, EstadoTurno estado);

    /** Próximo turno (>= ahora) por paciente para un set de pacientes del profesional. */
    @Query("""
        SELECT t.paciente.id, MIN(t.fechaHora)
        FROM Turno t
        WHERE t.profesional.id = :profesionalId
          AND t.paciente.id IN :pacienteIds
          AND t.fechaHora >= :ahora
        GROUP BY t.paciente.id
        """)
    List<Object[]> findProximoTurnoByPacienteIds(@Param("profesionalId") Long profesionalId,
                                                 @Param("pacienteIds") java.util.Collection<Long> pacienteIds,
                                                 @Param("ahora") LocalDateTime ahora);

    /** Próximo turno (>= ahora) para un único paciente. */
    @Query("""
        SELECT MIN(t.fechaHora)
        FROM Turno t
        WHERE t.profesional.id = :profesionalId
          AND t.paciente.id   = :pacienteId
          AND t.fechaHora     >= :ahora
        """)
    Optional<LocalDateTime> findProximoTurnoPaciente(@Param("profesionalId") Long profesionalId,
                                                     @Param("pacienteId")    Long pacienteId,
                                                     @Param("ahora")         LocalDateTime ahora);
}
