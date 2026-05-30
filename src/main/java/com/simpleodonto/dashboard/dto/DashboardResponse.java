package com.simpleodonto.dashboard.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        // Pacientes
        long    pacientesTotal,
        long    pacientesNuevosEsteMes,
        long    pacientesNoVolvieron90Dias,

        // Turnos
        long             turnosPendientesHoy,
        ProximoTurnoDto  proximoTurno,

        // Financiero
        BigDecimal facturadoMes,
        BigDecimal cobradoMes,
        BigDecimal pendienteMes,
        long       cobrosPendientesCantidad,

        // Productividad
        String diaMasConsultas,
        String obraSocialMasPacientes,
        Double promedioConsultasPorDia
) {}
