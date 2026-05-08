package com.simpleodonto.analisis.dto;

import java.time.LocalDateTime;

public record AnalisisResponse(
        Long          id,
        String        nombre,
        String        imagenTipo,
        Double        escala,
        Long          pacienteId,
        String        pacienteApellido,
        String        pacienteNombre,
        int           cantidadTrazos,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
