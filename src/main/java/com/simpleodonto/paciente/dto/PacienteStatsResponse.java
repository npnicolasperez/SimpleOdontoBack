package com.simpleodonto.paciente.dto;

public record PacienteStatsResponse(
        long total,
        long nuevosEsteMes,
        long conTurnoProximo,
        long sinTurno
) {}
