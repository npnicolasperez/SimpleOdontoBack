package com.simpleodonto.obrasocial.dto;

import jakarta.validation.constraints.NotBlank;

public record ObraSocialRequest(
        @NotBlank String nombre
) {}
