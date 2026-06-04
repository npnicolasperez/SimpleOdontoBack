package com.simpleodonto.profesional.repository;

import com.simpleodonto.profesional.domain.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    Optional<Profesional> findByEmail(String email);
    Optional<Profesional> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    boolean existsByEspecialidadId(Long especialidadId);
    java.util.Optional<Profesional> findByGoogleCalendarChannelId(String channelId);
}
