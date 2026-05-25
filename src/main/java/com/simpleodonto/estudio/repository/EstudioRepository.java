package com.simpleodonto.estudio.repository;

import com.simpleodonto.estudio.domain.Estudio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstudioRepository extends JpaRepository<Estudio, Long> {
    Page<Estudio> findByProfesionalId(Long profesionalId, Pageable pageable);
    List<Estudio> findByProfesionalIdAndPacienteIdOrderByLastUpdatedDesc(Long profesionalId, Long pacienteId);
    Optional<Estudio> findByIdAndProfesionalId(Long id, Long profesionalId);

    @Query("""
        SELECT e FROM Estudio e
        WHERE e.profesional.id = :profesionalId
          AND (LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(e.paciente.apellido) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(e.paciente.nombre)   LIKE LOWER(CONCAT('%', :buscar, '%')))
        """)
    Page<Estudio> buscar(@Param("profesionalId") Long profesionalId,
                         @Param("buscar") String buscar,
                         Pageable pageable);
}
