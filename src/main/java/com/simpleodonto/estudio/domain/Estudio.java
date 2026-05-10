package com.simpleodonto.estudio.domain;

import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "estudios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Estudio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "imagen_tipo", nullable = false)
    private String imagenTipo;

    @Column(name = "imagen", nullable = false, columnDefinition = "bytea")
    private byte[] imagen;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> trazos = new ArrayList<>();

    @Column(nullable = false)
    private Double escala;

    @Column(columnDefinition = "text")
    private String descripcion;
}
