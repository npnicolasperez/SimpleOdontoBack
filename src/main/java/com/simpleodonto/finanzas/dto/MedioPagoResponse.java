package com.simpleodonto.finanzas.dto;

import java.time.LocalDateTime;

public record MedioPagoResponse(Long id, String nombre, boolean sistema, LocalDateTime dateCreated) {}
