package com.simpleodonto.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Body para crear un cobro de OS. {@code ingresoIds} son los ingresos PENDIENTES que se
 * cierran con este cobro — deben ser todos del profesional, todos de la OS indicada, y todavía no estar cubiertos por otro cobro.
 */
public record CobroObraSocialRequest(
        @NotNull(message = "La obra social es obligatoria")
        Long obraSocialId,

        @NotNull(message = "El consultorio es obligatorio")
        Long consultorioId,

        @NotNull(message = "La fecha del cobro es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El monto recibido es obligatorio")
        @DecimalMin(value = "0", message = "El monto recibido no puede ser negativo")
        BigDecimal montoRecibido,

        Long medioPagoId,

        String descripcion,

        @NotEmpty(message = "Tenés que seleccionar al menos un ingreso pendiente")
        List<Long> ingresoIds
) {}
