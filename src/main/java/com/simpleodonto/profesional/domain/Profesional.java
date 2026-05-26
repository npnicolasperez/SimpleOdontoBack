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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    @Builder.Default
    private EstadoProfesional estado = EstadoProfesional.PENDIENTE;

    @Column(nullable = false)
    @Builder.Default
    private boolean test = false;

    @Column(name = "google_calendar_refresh_token", length = 512)
    private String googleCalendarRefreshToken;

    @Column(name = "google_calendar_channel_id")
    private String googleCalendarChannelId;

    @Column(name = "google_calendar_resource_id")
    private String googleCalendarResourceId;

    @Column(name = "google_calendar_webhook_expiry")
    private Long googleCalendarWebhookExpiry;

    @Column(name = "google_calendar_sync_token")
    private String googleCalendarSyncToken;

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
