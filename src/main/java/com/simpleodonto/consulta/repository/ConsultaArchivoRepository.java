package com.simpleodonto.consulta.repository;

import com.simpleodonto.consulta.domain.ConsultaArchivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultaArchivoRepository extends JpaRepository<ConsultaArchivo, Long> {
    List<ConsultaArchivo>    findByConsultaId(Long consultaId);
    Optional<ConsultaArchivo> findByIdAndConsultaId(Long id, Long consultaId);
}
