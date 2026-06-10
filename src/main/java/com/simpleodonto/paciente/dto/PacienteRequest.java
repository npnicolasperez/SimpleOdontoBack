package com.simpleodonto.paciente.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record PacienteRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        String dni,
        LocalDate fechaNac,
        String telefono,
        String email,
        String direccion,
        List<PacienteObraSocialDto> obrasSociales,
        String ocupacion,
        String grupoSanguineo,
        String alergias,
        String medicaciones,
        String antecedentes,
        String antecedentesFamiliares,
        Double peso,
        Integer altura
) {}
