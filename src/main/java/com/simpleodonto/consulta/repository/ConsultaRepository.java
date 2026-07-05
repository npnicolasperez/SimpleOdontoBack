package com.simpleodonto.consulta.repository;

import com.simpleodonto.consulta.domain.Consulta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    List<Consulta> findByPacienteIdAndProfesionalId(Long pacienteId, Long profesionalId, Sort sort);
    Optional<Consulta> findByIdAndProfesionalId(Long id, Long profesionalId);
    boolean existsByConsultorioId(Long consultorioId);
    boolean existsByObraSocialId(Long obraSocialId);

    @Query("""
        SELECT c FROM Consulta c
        WHERE c.profesional.id = :profId AND c.fecha >= :desde AND c.fecha < :hasta
        """)
    List<Consulta> findByProfesionalIdAndFechaBetween(@Param("profId") Long profId,
                                                       @Param("desde") LocalDate desde,
                                                       @Param("hasta") LocalDate hasta);

    @Query("""
        SELECT c FROM Consulta c
        WHERE c.profesional.id = :profId
          AND c.fecha >= :desde AND c.fecha < :hasta
          AND (:consultorioId IS NULL OR c.consultorio.id = :consultorioId)
        """)
    List<Consulta> findByProfesionalIdAndFechaBetweenAndConsultorio(@Param("profId") Long profId,
                                                                    @Param("desde") LocalDate desde,
                                                                    @Param("hasta") LocalDate hasta,
                                                                    @Param("consultorioId") Long consultorioId);

    @Query("""
        SELECT c FROM Consulta c
        WHERE c.profesional.id = :profesionalId
          AND c.firmaSolicitadaEn IS NOT NULL
          AND c.firmaPng IS NULL
        ORDER BY c.firmaSolicitadaEn DESC
        """)
    List<Consulta> findFirmasPendientes(@Param("profesionalId") Long profesionalId, Pageable pageable);

    /** Última fecha de consulta por paciente para un set de pacientes del profesional. */
    @Query("""
        SELECT c.paciente.id, MAX(c.fecha)
        FROM Consulta c
        WHERE c.profesional.id = :profesionalId
          AND c.paciente.id IN :pacienteIds
        GROUP BY c.paciente.id
        """)
    List<Object[]> findMaxFechaByPacienteIds(@Param("profesionalId") Long profesionalId,
                                             @Param("pacienteIds") java.util.Collection<Long> pacienteIds);
}
