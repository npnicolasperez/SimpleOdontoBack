package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoResponse(
        Long       id,                    // id del Ingreso o Egreso (para acciones)
        String     origen,                // "consulta" | "libre" | "egreso"
        String     tipo,                  // "ingreso" | "egreso" | "pendiente"
        LocalDate  fecha,
        String     descripcion,
        BigDecimal monto,                 // lo que entra al profesional
        BigDecimal montoTotal,            // total de la práctica, null si no aplica
        Integer    porcentajeProfesional, // %, null si no aplica o es 100
        String     estado,                // "CONFIRMADO" | "PENDIENTE" | null
        Long       consultaId,            // id de la consulta origen (solo si origen="consulta")
        Long       pacienteId             // id del paciente de la consulta (solo si origen="consulta")
) {}
