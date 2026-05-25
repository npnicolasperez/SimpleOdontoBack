package com.simpleodonto.estudio.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EstudioDetalleResponse(
        Long                        id,
        String                      nombre,
        String                      imagenTipo,
        String                      imagenBase64,
        Double                      escala,
        Long                        pacienteId,
        String                      pacienteApellido,
        String                      pacienteNombre,
        List<Map<String, Object>>   trazos,
        String                      descripcion,
        LocalDateTime               dateCreated,
        LocalDateTime               lastUpdated
) {}
