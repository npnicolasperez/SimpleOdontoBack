package com.simpleodonto.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lista de emails con rol ADMIN, configurada vía env var ADMIN_EMAILS
 * (separados por coma). Se evalúa case-insensitive.
 */
@Component
public class AdminEmails {

    private final Set<String> emails;

    public AdminEmails(@Value("${app.admin-emails:}") String raw) {
        this.emails = (raw == null || raw.isBlank())
                ? Set.of()
                : Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(String email) {
        if (email == null) return false;
        return emails.contains(email.toLowerCase(Locale.ROOT));
    }

    public Set<String> all() {
        return emails;
    }
}
