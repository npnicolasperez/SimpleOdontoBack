package com.simpleodonto.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;

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
        Double promedioConsultasPorDia,

        // Consultas
        long consultasMes,

        // Turnos próximos 7 días (siempre desde mañana, independiente del mes)
        long turnosPendientesManana,

        // Cobros OS pendientes histórico (independiente del mes) — nombre → cantidad
        Map<String, Long> pendientesOsNombres
) {}
