package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsultaUpdateRequest(
        @NotNull(message = "Seleccioná un consultorio") Long consultorioId,
        LocalDate  fecha,
        String     motivo,
        String     descripcion,
        BigDecimal monto,
        TipoPago   tipoPago,
        Long       medioPagoId,
        Long       obraSocialId,
        Boolean    pendienteCobro
) {}
