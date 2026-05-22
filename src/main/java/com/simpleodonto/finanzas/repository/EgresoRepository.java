package com.simpleodonto.finanzas.repository;

import com.simpleodonto.finanzas.domain.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EgresoRepository extends JpaRepository<Egreso, Long> {

    @Query("SELECT e FROM Egreso e " +
           "WHERE e.profesional.id = :profesionalId " +
           "AND e.fecha >= :desde AND e.fecha < :hasta " +
           "ORDER BY e.fecha DESC")
    List<Egreso> findByProfesionalIdAndMes(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
