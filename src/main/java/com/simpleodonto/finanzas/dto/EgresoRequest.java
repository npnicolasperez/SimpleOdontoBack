package com.simpleodonto.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EgresoRequest(
        LocalDate  fecha,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0", message = "El monto no puede ser negativo")
        BigDecimal monto,
        String     descripcion,
        @NotNull(message = "El consultorio es obligatorio")
        Long       consultorioId
) {}
