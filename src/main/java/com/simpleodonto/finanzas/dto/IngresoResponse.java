package com.simpleodonto.finanzas.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import com.simpleodonto.finanzas.domain.EstadoIngreso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IngresoResponse(
        Long          id,
        Long          consultaId,
        String        pacienteNombre,
        String        pacienteApellido,
        String        descripcion,
        BigDecimal    monto,
        EstadoIngreso estado,
        TipoPago      tipoPago,
        Long          medioPagoId,
        String        medioPagoNombre,
        Long          obraSocialId,
        String        obraSocialNombre,
        Long          consultorioId,
        String        consultorioNombre,
        LocalDate     fecha,
        LocalDateTime dateCreated
) {}
