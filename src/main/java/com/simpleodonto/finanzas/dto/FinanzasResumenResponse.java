package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;

public record FinanzasResumenResponse(
        String     mes,
        BigDecimal totalMes,
        BigDecimal confirmadoMes,
        BigDecimal pendienteMes,
        long       cantidadTotal,
        long       cantidadConfirmada,
        long       cantidadPendiente,
        Double     variacionPct,
        BigDecimal ticketPromedio
) {}
