package com.simpleodonto.finanzas.repository;

import com.simpleodonto.finanzas.domain.Ingreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {
    Optional<Ingreso> findByConsultaId(Long consultaId);
    List<Ingreso> findByProfesionalIdOrderByDateCreatedDesc(Long profesionalId);

    @Query("SELECT i FROM Ingreso i " +
           "WHERE i.profesional.id = :profesionalId " +
           "AND i.fecha >= :desde AND i.fecha < :hasta")
    List<Ingreso> findByProfesionalIdAndMes(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
