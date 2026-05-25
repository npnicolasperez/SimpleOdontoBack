package com.simpleodonto.consultorio.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultorioRequest(@NotBlank String nombre) {}
