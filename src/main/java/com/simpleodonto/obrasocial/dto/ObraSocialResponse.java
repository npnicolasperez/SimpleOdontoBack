package com.simpleodonto.obrasocial.dto;

import java.time.LocalDateTime;

public record ObraSocialResponse(
        Long          id,
        String        nombre,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
