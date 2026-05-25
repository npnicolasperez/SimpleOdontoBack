package com.simpleodonto.finanzas.repository;

import com.simpleodonto.finanzas.domain.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {
    List<MedioPago> findByProfesionalId(Long profesionalId);
    Optional<MedioPago> findByIdAndProfesionalId(Long id, Long profesionalId);
}
