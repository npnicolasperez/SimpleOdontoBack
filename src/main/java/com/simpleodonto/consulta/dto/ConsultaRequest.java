package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsultaRequest(
        @NotNull Long pacienteId,
        Long          consultorioId,
        LocalDate     fecha,
        String        descripcion,
        BigDecimal    montoTotal,
        Integer       porcentajeProfesional,
        TipoPago      tipoPago,
        Long          medioPagoId,
        Boolean       pendienteCobro
) {}
