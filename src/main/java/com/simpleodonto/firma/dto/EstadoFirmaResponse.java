package com.simpleodonto.firma.dto;

import java.time.LocalDateTime;

public record EstadoFirmaResponse(
        boolean       firmada,
        boolean       solicitada,
        LocalDateTime fechaFirma
) {}
