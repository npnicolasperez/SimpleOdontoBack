package com.simpleodonto.finanzas.dto;

import com.simpleodonto.consulta.domain.TipoPago;

import java.math.BigDecimal;

public record IngresoLibreRequest(
        String     descripcion,
        BigDecimal monto,
        TipoPago   tipoPago,
        Long       medioPagoId,
        Long       consultorioId
) {}
