package com.simpleodonto.paciente.domain;

import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Asociación entre un paciente y una obra social. Cada paciente puede tener N obras sociales,
 * cada una con su propio nro de afiliado, plan y titular. El campo {@code orden} define cuál es
 * la "principal" (orden=0) — esa es la que se pre-selecciona en consultas con tipoPago=OBRA_SOCIAL.
 */
@Entity
@Table(name = "paciente_obra_social", uniqueConstraints = {
        @UniqueConstraint(name = "uk_paciente_obrasocial", columnNames = {"paciente_id", "obra_social_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PacienteObraSocial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_social_id", nullable = false)
    private ObraSocial obraSocial;

    @Column(name = "nro_afiliado")
    private String nroAfiliado;

    @Column(name = "plan_obra_social")
    private String plan;

    @Column(name = "titular_obra_social")
    private String titular;

    @Column(nullable = false)
    private Integer orden;
}
