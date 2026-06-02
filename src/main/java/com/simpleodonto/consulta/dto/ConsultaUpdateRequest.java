package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsultaUpdateRequest(
        Long       consultorioId,
        LocalDate  fecha,
        String     descripcion,
        BigDecimal montoTotal,
        Integer    porcentajeProfesional,
        TipoPago   tipoPago,
        Long       medioPagoId,
        Boolean    pendienteCobro
) {}
