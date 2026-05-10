package com.simpleodonto.estudio.repository;

import com.simpleodonto.estudio.domain.Estudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstudioRepository extends JpaRepository<Estudio, Long> {
    List<Estudio> findByProfesionalIdOrderByLastUpdatedDesc(Long profesionalId);
    List<Estudio> findByProfesionalIdAndPacienteIdOrderByLastUpdatedDesc(Long profesionalId, Long pacienteId);
    Optional<Estudio> findByIdAndProfesionalId(Long id, Long profesionalId);
}
