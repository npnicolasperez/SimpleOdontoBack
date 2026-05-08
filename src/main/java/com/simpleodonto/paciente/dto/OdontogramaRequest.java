package com.simpleodonto.paciente.dto;

import java.util.Map;

public record OdontogramaRequest(
        Map<String, String> superficies
) {}
