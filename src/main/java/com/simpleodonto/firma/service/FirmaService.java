package com.simpleodonto.firma.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consulta.repository.ConsultaRepository;
import com.simpleodonto.firma.dto.EstadoFirmaResponse;
import com.simpleodonto.firma.dto.FirmaPendienteResponse;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FirmaService {

    private final ConsultaRepository consultaRepository;

    /** Desktop solicita firma para una consulta. Cancela cualquier otra solicitud pendiente del mismo profesional. */
    @Transactional
    public void solicitar(Long consultaId, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        if (consulta.getFirmaPng() != null) {
            throw new IllegalStateException("La consulta ya está firmada");
        }
        // Cancelar otras solicitudes pendientes del mismo profesional para mantener un único slot activo.
        consultaRepository.findFirmasPendientes(profesional.getId(), PageRequest.of(0, 50)).forEach(c -> {
            if (!c.getId().equals(consultaId)) {
                c.setFirmaSolicitadaEn(null);
                consultaRepository.save(c);
            }
        });
        consulta.setFirmaSolicitadaEn(LocalDateTime.now());
        consultaRepository.save(consulta);
    }

    /** Mobile chequea si hay una firma pendiente para el profesional logueado. */
    @Transactional(readOnly = true)
    public Optional<FirmaPendienteResponse> obtenerPendiente(Profesional profesional) {
        List<Consulta> pendientes = consultaRepository.findFirmasPendientes(profesional.getId(), PageRequest.of(0, 1));
        if (pendientes.isEmpty()) return Optional.empty();
        Consulta c = pendientes.get(0);
        return Optional.of(new FirmaPendienteResponse(
                c.getId(),
                c.getPaciente() != null ? c.getPaciente().getNombre()   : null,
                c.getPaciente() != null ? c.getPaciente().getApellido() : null,
                c.getFecha(),
                c.getDescripcion(),
                c.getMontoTotal()
        ));
    }

    /** Mobile envía el PNG firmado. Limpia la solicitud y guarda la firma. */
    @Transactional
    public void firmar(Long consultaId, String pngBase64, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        if (consulta.getFirmaPng() != null) {
            throw new IllegalStateException("La consulta ya está firmada");
        }
        // Nota: no se requiere firmaSolicitadaEn != null porque el flujo touch firma directo
        // en el mismo device (sin pasar por el slot). Si vino del flujo desktop, igual lo limpiamos.

        byte[] png;
        try {
            String raw = pngBase64.contains(",") ? pngBase64.substring(pngBase64.indexOf(",") + 1) : pngBase64;
            png = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("PNG inválido");
        }
        if (png.length < 8 || png[0] != (byte)0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            throw new IllegalArgumentException("El archivo no es un PNG válido");
        }

        consulta.setFirmaPng(png);
        consulta.setFirmaFecha(LocalDateTime.now());
        consulta.setFirmaSolicitadaEn(null);
        consultaRepository.save(consulta);
    }

    @Transactional(readOnly = true)
    public EstadoFirmaResponse estado(Long consultaId, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        return new EstadoFirmaResponse(
                consulta.getFirmaPng() != null,
                consulta.getFirmaSolicitadaEn() != null,
                consulta.getFirmaFecha()
        );
    }

    /** Desktop cancela la solicitud (cerró el modal sin firmar). */
    @Transactional
    public void cancelarSolicitud(Long consultaId, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        consulta.setFirmaSolicitadaEn(null);
        consultaRepository.save(consulta);
    }

    /** Devuelve el PNG de la firma para mostrar en el front. */
    @Transactional(readOnly = true)
    public byte[] obtenerPng(Long consultaId, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        if (consulta.getFirmaPng() == null) {
            throw new EntityNotFoundException("La consulta no tiene firma");
        }
        return consulta.getFirmaPng();
    }
}
