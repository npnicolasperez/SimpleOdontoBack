package com.simpleodonto.estudio.dto;

import java.time.LocalDateTime;

public record EstudioResponse(
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
