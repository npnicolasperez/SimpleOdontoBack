package com.simpleodonto.consultorio.repository;

import com.simpleodonto.consultorio.domain.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {
    List<Consultorio> findByProfesionalId(Long profesionalId);
    Optional<Consultorio> findByIdAndProfesionalId(Long id, Long profesionalId);
}
