package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConsultaRequest(
        @NotNull Long      pacienteId,
        Long               consultorioId,
        String             motivoConsulta,
        String             practicaRealizada,
        @NotNull BigDecimal monto,
        @NotNull TipoPago   tipoPago
) {}
