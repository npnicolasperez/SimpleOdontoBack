package com.simpleodonto.analisis.dto;

import java.util.List;
import java.util.Map;

public record AnalisisUpdateRequest(
        List<Map<String, Object>> trazos,
        Double                    escala,
        Long                      pacienteId
) {}
