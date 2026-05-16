package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaResponse(
        Long          id,
        Long          pacienteId,
        String        pacienteApellido,
        String        pacienteNombre,
        Long          profesionalId,
        Long          consultorioId,
        String        consultorioNombre,
        String        motivoConsulta,
        String        diagnostico,
        String        practicaRealizada,
        BigDecimal    monto,
        TipoPago      tipoPago,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
