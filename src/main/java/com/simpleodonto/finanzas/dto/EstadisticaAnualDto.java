package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;

/**
 * Estadísticas mensuales para el último año (12 meses). Una fila por mes.
 *
 * @param año                año del período
 * @param mes                mes (1-12)
 * @param ingresosTotales    sumatoria de ingresos CONFIRMADOS de ese mes
 * @param consultaPromedio   promedio de monto cobrado por consulta (lo que recibe el profesional) en ese mes
 * @param cantidadConsultas  cantidad de consultas del mes
 */
public record EstadisticaAnualDto(
        int        año,
        int        mes,
        BigDecimal ingresosTotales,
        BigDecimal consultaPromedio,
        long       cantidadConsultas
) {}
