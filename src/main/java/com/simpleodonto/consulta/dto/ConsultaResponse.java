package com.simpleodonto.consulta.dto;

import com.simpleodonto.consulta.domain.TipoPago;
import com.simpleodonto.finanzas.domain.EstadoIngreso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ConsultaResponse(
        Long            id,
        Long            pacienteId,
        String          pacienteApellido,
        String          pacienteNombre,
        Long            profesionalId,
        Long            consultorioId,
        String          consultorioNombre,
        LocalDate       fecha,
        String          descripcion,
        BigDecimal      monto,
        TipoPago        tipoPago,
        Long            medioPagoId,
        String          medioPagoNombre,
        EstadoIngreso   estadoIngreso,
        LocalDateTime   dateCreated,
        LocalDateTime   lastUpdated,
        List<ArchivoInfo> archivos
) {}
