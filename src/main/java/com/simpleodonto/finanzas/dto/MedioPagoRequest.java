package com.simpleodonto.finanzas.dto;

import jakarta.validation.constraints.NotBlank;

public record MedioPagoRequest(@NotBlank String nombre) {}
