package com.simpleodonto.paciente.domain;

import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "paciente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_social_id")
    private ObraSocial obraSocial;

    @Column(name = "nro_afiliado")
    private String nroAfiliado;

    @Column(name = "plan_obra_social")
    private String planObraSocial;

    @Column(name = "titular_obra_social")
    private String titularObraSocial;

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
