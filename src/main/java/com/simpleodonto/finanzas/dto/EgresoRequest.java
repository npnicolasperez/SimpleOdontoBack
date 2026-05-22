package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EgresoRequest(
        LocalDate  fecha,
        BigDecimal monto,
        String     descripcion,
        Long       consultorioId
) {}
