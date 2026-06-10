package com.simpleodonto.finanzas.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record IngresoLibreRequest(
        String     descripcion,
        @DecimalMin(value = "0", message = "El monto no puede ser negativo")
        BigDecimal monto,
        TipoPago   tipoPago,
        Long       medioPagoId,
        Long       obraSocialId,
        @NotNull(message = "El consultorio es obligatorio")
        Long       consultorioId
) {}
