package com.simpleodonto.obrasocial.repository;

import com.simpleodonto.obrasocial.domain.ObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObraSocialRepository extends JpaRepository<ObraSocial, Long> {
    List<ObraSocial> findByProfesionalId(Long profesionalId);
    Optional<ObraSocial> findByIdAndProfesionalId(Long id, Long profesionalId);
}
