package com.simpleodonto.consulta.domain;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "practica_realizada", columnDefinition = "TEXT")
    private String practicaRealizada;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 20)
    private TipoPago tipoPago;
}
