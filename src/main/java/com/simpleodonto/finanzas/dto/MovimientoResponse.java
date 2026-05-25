package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoResponse(
        String     tipo,         // "ingreso" | "egreso" | "pendiente"
        LocalDate  fecha,
        String     descripcion,
        BigDecimal monto,
        String     estado        // "CONFIRMADO" | "PENDIENTE" | null
) {}
