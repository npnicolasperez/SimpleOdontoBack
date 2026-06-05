package com.simpleodonto.firma.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos que ve el paciente en pantalla antes de firmar. */
public record FirmaPendienteResponse(
        Long       consultaId,
        String     pacienteNombre,
        String     pacienteApellido,
        LocalDate  fecha,
        String     descripcion,
        BigDecimal montoTotal
) {}
