package com.simpleodonto.paciente.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        String dni,
        LocalDate fechaNac,
        String telefono,
        String email,
        String direccion,
        String obraSocial,
        String nroAfiliado,
        String ocupacion,
        String grupoSanguineo,
        String alergias,
        String medicaciones,
        String antecedentes,
        String antecedentesFamiliares,
        Double peso,
        Integer altura
) {}
