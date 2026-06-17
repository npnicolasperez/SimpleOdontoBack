package com.simpleodonto.finanzas.domain;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consulta.domain.TipoPago;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ingreso")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Ingreso extends BaseEntity {

    @Column(nullable = false)
    private LocalDate fecha;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIngreso estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", length = 20)
    private TipoPago tipoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_pago_id")
    private MedioPago medioPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    /** Obra social asociada al ingreso. Obligatorio cuando tipoPago=OBRA_SOCIAL (validado a nivel app). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_social_id")
    private ObraSocial obraSocial;

    /**
     * Cobro de OS que cerró este ingreso. Null hasta que el profesional registre el cobro batch.
     * Cuando se setea, el ingreso pasa a CONFIRMADO. Si se elimina el cobro, vuelve a null y a PENDIENTE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobro_obra_social_id")
    private CobroObraSocial cobroObraSocial;
}
