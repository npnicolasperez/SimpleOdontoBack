package com.simpleodonto.consulta.repository;

import com.simpleodonto.consulta.domain.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    Page<Consulta> findByProfesionalId(Long profesionalId, Pageable pageable);
    List<Consulta> findByPacienteIdAndProfesionalId(Long pacienteId, Long profesionalId, Sort sort);
    Optional<Consulta> findByIdAndProfesionalId(Long id, Long profesionalId);
    boolean existsByConsultorioId(Long consultorioId);

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
}
