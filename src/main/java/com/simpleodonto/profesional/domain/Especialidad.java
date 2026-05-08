package com.simpleodonto.profesional.domain;

import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "especialidad")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Especialidad extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nombre;
}
