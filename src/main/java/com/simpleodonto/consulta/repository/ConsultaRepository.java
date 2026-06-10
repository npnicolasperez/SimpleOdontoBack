package com.simpleodonto.consulta.repository;

import com.simpleodonto.consulta.domain.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    Page<Consulta> findByProfesionalId(Long profesionalId, Pageable pageable);
    List<Consulta> findByPacienteIdAndProfesionalId(Long pacienteId, Long profesionalId, Sort sort);
    Optional<Consulta> findByIdAndProfesionalId(Long id, Long profesionalId);
    boolean existsByConsultorioId(Long consultorioId);

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
          AND (LOWER(c.paciente.apellido) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(c.paciente.nombre)   LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :buscar, '%')))
        """)
    Page<Consulta> buscar(@Param("profesionalId") Long profesionalId,
                          @Param("buscar") String buscar,
                          Pageable pageable);

    @Query("""
        SELECT c FROM Consulta c
        WHERE c.profesional.id = :profesionalId
          AND c.firmaSolicitadaEn IS NOT NULL
          AND c.firmaPng IS NULL
        ORDER BY c.firmaSolicitadaEn DESC
        """)
    List<Consulta> findFirmasPendientes(@Param("profesionalId") Long profesionalId, Pageable pageable);
}
