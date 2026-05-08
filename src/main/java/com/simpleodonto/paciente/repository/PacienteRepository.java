package com.simpleodonto.paciente.repository;

import com.simpleodonto.paciente.domain.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

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
}
