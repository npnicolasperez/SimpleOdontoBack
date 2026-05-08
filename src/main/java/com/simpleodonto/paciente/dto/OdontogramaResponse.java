package com.simpleodonto.paciente.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record OdontogramaResponse(
        Long                id,
        Long                pacienteId,
        Map<String, String> superficies,
        LocalDateTime       dateCreated,
        LocalDateTime       lastUpdated
) {}
