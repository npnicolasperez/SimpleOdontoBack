package com.simpleodonto.profesional.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EspecialidadRequest(
        @NotBlank @Size(max = 100) String nombre
) {}
