package com.simpleodonto.consultorio.dto;

import java.time.LocalDateTime;

public record ConsultorioResponse(
        Long          id,
        String        nombre,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
