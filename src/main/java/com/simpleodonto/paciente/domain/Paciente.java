package com.simpleodonto.paciente.domain;

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

    @Column(name = "obra_social")
    private String obraSocial;

    @Column(name = "nro_afiliado")
    private String nroAfiliado;
}
