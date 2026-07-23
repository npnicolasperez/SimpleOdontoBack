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
    boolean existsByObraSocialId(Long obraSocialId);

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

    /**
     * Ingresos PENDIENTES de una obra social puntual en un consultorio puntual (para el flow de
     * "Registrar cobro de OS"). Cada cobro es por un único consultorio porque las OS pagan
     * separado a cada consultorio del profesional.
     */
    @Query("""
        SELECT i FROM Ingreso i
        LEFT JOIN i.consulta c LEFT JOIN c.paciente p
        WHERE i.profesional.id = :profesionalId
          AND i.estado = com.simpleodonto.finanzas.domain.EstadoIngreso.PENDIENTE
          AND i.obraSocial.id = :obraSocialId
          AND i.consultorio.id = :consultorioId
          AND i.cobroObraSocial IS NULL
        ORDER BY i.fecha ASC, i.id ASC
        """)
    List<Ingreso> findPendientesByObraSocialYConsultorio(@Param("profesionalId") Long profesionalId,
                                                         @Param("obraSocialId") Long obraSocialId,
                                                         @Param("consultorioId") Long consultorioId);

    List<Ingreso> findByCobroObraSocialId(Long cobroObraSocialId);

    /** Todos los ingresos PENDIENTES del profesional, sin filtro de fecha (para el widget histórico del dashboard). */
    @Query("""
        SELECT i FROM Ingreso i
        LEFT JOIN FETCH i.obraSocial
        WHERE i.profesional.id = :profesionalId
          AND i.estado = com.simpleodonto.finanzas.domain.EstadoIngreso.PENDIENTE
        ORDER BY i.fecha DESC
        """)
    List<Ingreso> findPendientesByProfesional(@Param("profesionalId") Long profesionalId);

    /** Ingresos PENDIENTES con tipoPago=PARTICULAR (para la pestaña Particular del flow de registrar cobros). */
    @Query("""
        SELECT i FROM Ingreso i
        LEFT JOIN i.consulta c LEFT JOIN c.paciente p
        WHERE i.profesional.id = :profesionalId
          AND i.estado = com.simpleodonto.finanzas.domain.EstadoIngreso.PENDIENTE
          AND i.tipoPago = com.simpleodonto.consulta.domain.TipoPago.PARTICULAR
        ORDER BY i.fecha ASC, i.id ASC
        """)
    List<Ingreso> findPendientesParticulares(@Param("profesionalId") Long profesionalId);

    /** Todos los ingresos PENDIENTES de OS del profesional (todas las OS y consultorios). */
    @Query("""
        SELECT i FROM Ingreso i
        LEFT JOIN i.consulta c LEFT JOIN c.paciente p
        WHERE i.profesional.id = :profesionalId
          AND i.estado = com.simpleodonto.finanzas.domain.EstadoIngreso.PENDIENTE
          AND i.tipoPago = com.simpleodonto.consulta.domain.TipoPago.OBRA_SOCIAL
          AND i.cobroObraSocial IS NULL
        ORDER BY i.obraSocial.nombre ASC, i.fecha ASC, i.id ASC
        """)
    List<Ingreso> findPendientesObraSocial(@Param("profesionalId") Long profesionalId);
}
