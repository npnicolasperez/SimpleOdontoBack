package com.simpleodonto.consultorio.dto;

import java.time.LocalDateTime;

public record ConsultorioResponse(
        Long          id,
        String        nombre,
        String        direccion,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
