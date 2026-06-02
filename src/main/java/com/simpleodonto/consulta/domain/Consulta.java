package com.simpleodonto.consulta.domain;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "consulta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Consulta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Monto total de la práctica (lo que paga el paciente). */
    @Column(name = "monto_total", precision = 12, scale = 2)
    private BigDecimal montoTotal;

    /** Porcentaje del total que le corresponde al profesional (0-100). */
    @Column(name = "porcentaje_profesional")
    private Integer porcentajeProfesional;

    /** Lo que efectivamente cobra el profesional (montoTotal * porcentaje / 100). Es lo que va a Ingresos. */
    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", length = 20)
    private TipoPago tipoPago;

}
