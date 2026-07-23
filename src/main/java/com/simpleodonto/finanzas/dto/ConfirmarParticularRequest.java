package com.simpleodonto.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmarParticularRequest(
        @NotNull(message = "La fecha del cobro es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El monto cobrado es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        Long medioPagoId
) {}
