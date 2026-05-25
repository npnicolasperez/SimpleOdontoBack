package com.simpleodonto.turno.dto;

import com.simpleodonto.turno.domain.EstadoTurno;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TurnoRequest(
        Long          pacienteId,
        String        nombrePacienteLibre,
        Long          consultorioId,
        @NotNull LocalDateTime fechaHora,
        Integer       duracionMinutos,
        String        motivo,
        EstadoTurno   estado
) {}
