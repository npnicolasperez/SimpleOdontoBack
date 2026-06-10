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
    boolean existsByConsultorioId(Long consultorioId);
    boolean existsByMedioPagoId(Long medioPagoId);

    @Query("SELECT i FROM Ingreso i " +
           "WHERE i.profesional.id = :profesionalId " +
           "AND i.fecha >= :desde AND i.fecha < :hasta")
    List<Ingreso> findByProfesionalIdAndMes(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("SELECT i FROM Ingreso i " +
           "LEFT JOIN i.consulta c LEFT JOIN c.paciente p " +
           "WHERE i.profesional.id = :profesionalId " +
           "AND i.fecha >= :desde AND i.fecha < :hasta " +
           "AND (LOWER(i.descripcion)  LIKE LOWER(CONCAT('%', :buscar, '%')) " +
           "  OR LOWER(p.apellido)     LIKE LOWER(CONCAT('%', :buscar, '%')) " +
           "  OR LOWER(p.nombre)       LIKE LOWER(CONCAT('%', :buscar, '%')))")
    List<Ingreso> findByProfesionalIdAndMesAndBuscar(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("buscar") String buscar);

    @Query("SELECT i FROM Ingreso i " +
           "WHERE i.profesional.id = :profesionalId " +
           "AND i.estado = com.simpleodonto.finanzas.domain.EstadoIngreso.CONFIRMADO " +
           "AND i.fecha >= :desde AND i.fecha < :hasta")
    List<Ingreso> findConfirmadosByProfesionalIdAndRango(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("SELECT i FROM Ingreso i " +
           "WHERE i.profesional.id = :profesionalId " +
           "AND i.estado = com.simpleodonto.finanzas.domain.EstadoIngreso.CONFIRMADO " +
           "AND i.fecha >= :desde AND i.fecha < :hasta " +
           "AND (:consultorioId IS NULL OR i.consultorio.id = :consultorioId)")
    List<Ingreso> findConfirmadosByProfesionalIdAndRangoAndConsultorio(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("consultorioId") Long consultorioId);
}
