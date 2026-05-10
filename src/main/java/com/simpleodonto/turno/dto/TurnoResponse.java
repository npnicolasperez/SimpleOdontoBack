package com.simpleodonto.turno.dto;

import com.simpleodonto.turno.domain.EstadoTurno;

import java.time.LocalDateTime;

public record TurnoResponse(
        Long          id,
        Long          pacienteId,
        String        pacienteNombre,
        String        pacienteApellido,
        String        nombrePacienteLibre,
        Long          consultorioId,
        String        consultorioNombre,
        LocalDateTime fechaHora,
        int           duracionMinutos,
        String        motivo,
        EstadoTurno   estado,
        String        googleEventId,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
