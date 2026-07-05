package com.simpleodonto.consulta.domain;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Monto que cobra el profesional por la consulta. Es lo que va a Ingresos. */
    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", length = 20)
    private TipoPago tipoPago;

    /** Obra social asociada a esta consulta. Obligatorio cuando tipoPago=OBRA_SOCIAL (validado a nivel app). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_social_id")
    private ObraSocial obraSocial;

    /** Firma del paciente como PNG. Null si aún no firmó. Se guarda como BYTEA (sin @Lob, que en Postgres usa OID y rompe fuera de transacción). */
    @Column(name = "firma_png", columnDefinition = "BYTEA")
    private byte[] firmaPng;

    /** Timestamp del momento en que el paciente guardó la firma. */
    @Column(name = "firma_fecha")
    private LocalDateTime firmaFecha;

    /** Si != null, hay una solicitud de firma pendiente desde este timestamp. Se limpia al firmar o cancelar. */
    @Column(name = "firma_solicitada_en")
    private LocalDateTime firmaSolicitadaEn;
}
