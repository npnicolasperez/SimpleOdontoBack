package com.simpleodonto.paciente.repository;

import com.simpleodonto.paciente.domain.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    long countByProfesionalId(Long profesionalId);

    @Query("SELECT COUNT(p) FROM Paciente p WHERE p.profesional.id = :profId AND p.dateCreated >= :desde")
    long countNuevosDesde(@Param("profId") Long profId, @Param("desde") LocalDateTime desde);

    Page<Paciente> findByProfesionalId(Long profesionalId, Pageable pageable);

    @Query("""
        SELECT p FROM Paciente p
        WHERE p.profesional.id = :profesionalId
          AND (LOWER(p.apellido)  LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(p.nombre)    LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR p.dni              LIKE CONCAT('%', :buscar, '%'))
        """)
    Page<Paciente> buscar(@Param("profesionalId") Long profesionalId,
                          @Param("buscar")        String buscar,
                          Pageable pageable);

    Optional<Paciente> findByIdAndProfesionalId(Long id, Long profesionalId);

    @Query("""
        SELECT COUNT(DISTINCT p.id) FROM Paciente p
        WHERE p.profesional.id = :profId
          AND EXISTS (SELECT c FROM Consulta c WHERE c.paciente.id = p.id)
          AND NOT EXISTS (SELECT c FROM Consulta c WHERE c.paciente.id = p.id AND c.fecha >= :fechaCorte)
        """)
    long countPacientesNoVolvieronDesde(@Param("profId") Long profId, @Param("fechaCorte") LocalDate fechaCorte);

    @Query("""
        SELECT pos.obraSocial.nombre FROM PacienteObraSocial pos
        WHERE pos.paciente.profesional.id = :profId
        GROUP BY pos.obraSocial.nombre ORDER BY COUNT(DISTINCT pos.paciente.id) DESC
        """)
    List<String> findTopObraSociales(@Param("profId") Long profId, Pageable pageable);
}
