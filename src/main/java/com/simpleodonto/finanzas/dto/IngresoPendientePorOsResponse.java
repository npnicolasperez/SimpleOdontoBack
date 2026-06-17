package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ingreso pendiente de cobro de una OS puntual. Devuelto por
 * {@code GET /api/finanzas/ingresos/pendientes?obraSocialId=...}.
 * <p>
 * Trae los campos completos de una consulta para que el front pueda renderizar el mismo
 * {@code ConsultaCard} que usa en historia clínica (sólo cambia el indicador "Cobro pendiente"
 * por un checkbox de selección).
 */
public record IngresoPendientePorOsResponse(
        Long       ingresoId,
        Long       consultaId,
        LocalDate  fecha,
        String     pacienteApellido,
        String     pacienteNombre,
        String     descripcion,
        BigDecimal monto,
        Long       consultorioId,
        String     consultorioNombre,
        String     obraSocialNombre
) {}
