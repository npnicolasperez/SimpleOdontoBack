package com.simpleodonto.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista de lista de un cobro de OS. Incluye el "monto esperado" (suma de los ingresos cubiertos)
 * para que el front pueda mostrar la diferencia con {@code montoRecibido} sin queries adicionales.
 */
public record CobroObraSocialResponse(
        Long          id,
        Long          obraSocialId,
        String        obraSocialNombre,
        Long          consultorioId,
        String        consultorioNombre,
        LocalDate     fecha,
        BigDecimal    montoRecibido,
        BigDecimal    montoEsperado,
        long          cantidadIngresos,
        Long          medioPagoId,
        String        medioPagoNombre,
        String        descripcion,
        LocalDateTime dateCreated
) {}
