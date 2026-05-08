package com.simpleodonto.analisis.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AnalisisDetalleResponse(
        Long                        id,
        String                      nombre,
        String                      imagenTipo,
        String                      imagenBase64,
        Double                      escala,
        Long                        pacienteId,
        String                      pacienteApellido,
        String                      pacienteNombre,
        List<Map<String, Object>>   trazos,
        LocalDateTime               dateCreated,
        LocalDateTime               lastUpdated
) {}
