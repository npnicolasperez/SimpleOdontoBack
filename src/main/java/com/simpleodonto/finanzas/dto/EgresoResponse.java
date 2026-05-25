package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EgresoResponse(
        Long          id,
        LocalDate     fecha,
        BigDecimal    monto,
        String        descripcion,
        Long          consultorioId,
        String        consultorioNombre,
        LocalDateTime dateCreated
) {}
