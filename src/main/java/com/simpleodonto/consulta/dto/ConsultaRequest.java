package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsultaRequest(
        @NotNull(message = "Falta el paciente") Long pacienteId,
        @NotNull(message = "Seleccioná un consultorio") Long consultorioId,
        LocalDate     fecha,
        String        descripcion,
        BigDecimal    montoTotal,
        Integer       porcentajeProfesional,
        TipoPago      tipoPago,
        Long          medioPagoId,
        Boolean       pendienteCobro
) {}
