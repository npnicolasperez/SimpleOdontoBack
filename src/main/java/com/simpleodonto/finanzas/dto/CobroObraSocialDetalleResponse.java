package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de un cobro de OS, con la lista de ingresos que cubre.
 * Usado por {@code GET /api/cobros-os/{id}}.
 */
public record CobroObraSocialDetalleResponse(
        Long          id,
        Long          obraSocialId,
        String        obraSocialNombre,
        Long          consultorioId,
        String        consultorioNombre,
        LocalDate     fecha,
        BigDecimal    montoRecibido,
        BigDecimal    montoEsperado,
        Long          medioPagoId,
        String        medioPagoNombre,
        String        descripcion,
        List<IngresoCubiertoDto> ingresos,
        LocalDateTime dateCreated
) {
    public record IngresoCubiertoDto(
            Long       ingresoId,
            Long       consultaId,
            LocalDate  fecha,
            String     pacienteApellido,
            String     pacienteNombre,
            String     descripcion,
            BigDecimal monto
    ) {}
}
