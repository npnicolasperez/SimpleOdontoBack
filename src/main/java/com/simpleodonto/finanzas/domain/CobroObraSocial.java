package com.simpleodonto.finanzas.domain;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa el evento concreto del cobro de una transferencia/pago de una obra social.
 * Cubre N ingresos pendientes de esa misma OS. Al crearse, los ingresos vinculados pasan
 * a CONFIRMADO; al eliminarse, vuelven a PENDIENTE.
 */
@Entity
@Table(name = "cobro_obra_social")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CobroObraSocial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_social_id", nullable = false)
    private ObraSocial obraSocial;

    /**
     * Consultorio al que pertenece este cobro. Las OS pagan por separado a cada consultorio
     * del profesional, así que cada cobro es para un único consultorio. Los ingresos que cubre
     * deben ser todos del mismo consultorio.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    /** Fecha del cobro (cuando entró la transferencia / pago). */
    @Column(nullable = false)
    private LocalDate fecha;

    /** Monto realmente recibido (puede diferir del esperado sumando los ingresos cubiertos). */
    @Column(name = "monto_recibido", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_pago_id")
    private MedioPago medioPago;

    /** Notas opcionales del profesional (ej: "comprobante 1234", "incluye ajuste por rechazos"). */
    @Column(length = 1000)
    private String descripcion;
}
