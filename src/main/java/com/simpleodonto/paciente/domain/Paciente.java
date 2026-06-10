package com.simpleodonto.paciente.domain;

import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paciente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Paciente extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    private String dni;

    @Column(name = "fecha_nac")
    private LocalDate fechaNac;

    private String telefono;
    private String email;
    private String direccion;

    /**
     * Obras sociales del paciente. La de orden=0 es la "principal" — la que se
     * pre-selecciona en consultas con tipoPago=OBRA_SOCIAL.
     */
    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<PacienteObraSocial> obrasSociales = new ArrayList<>();

    private String ocupacion;

    @Column(name = "grupo_sanguineo")
    private String grupoSanguineo;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    @Column(columnDefinition = "TEXT")
    private String medicaciones;

    @Column(columnDefinition = "TEXT")
    private String antecedentes;

    @Column(name = "antecedentes_familiares", columnDefinition = "TEXT")
    private String antecedentesFamiliares;

    private Double peso;
    private Integer altura;
}
