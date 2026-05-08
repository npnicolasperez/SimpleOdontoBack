package com.simpleodonto.profesional.domain;

import com.simpleodonto.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "profesional")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Profesional extends BaseEntity implements UserDetails {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String googleId;

    private String matricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean perfilCompleto;

    @Column(nullable = false)
    @Builder.Default
    private boolean test = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PROFESIONAL"));
    }

    @Override public String  getUsername()             { return email; }
    @Override public boolean isAccountNonExpired()     { return true;  }
    @Override public boolean isAccountNonLocked()      { return true;  }
    @Override public boolean isCredentialsNonExpired() { return true;  }
    @Override public boolean isEnabled()               { return true;  }
}
