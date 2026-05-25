package com.simpleodonto.estudio.dto;

import java.util.List;
import java.util.Map;

public record EstudioUpdateRequest(
        List<Map<String, Object>> trazos,
        Double                    escala,
        Long                      pacienteId,
        String                    descripcion
) {}
