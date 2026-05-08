package com.simpleodonto.paciente.service;

import com.simpleodonto.paciente.domain.Odontograma;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.dto.OdontogramaRequest;
import com.simpleodonto.paciente.dto.OdontogramaResponse;
import com.simpleodonto.paciente.dto.PacienteRequest;
import com.simpleodonto.paciente.dto.PacienteResponse;
import com.simpleodonto.paciente.repository.OdontogramaRepository;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository    pacienteRepository;
    private final OdontogramaRepository odontogramaRepository;

    public Page<PacienteResponse> listar(String buscar, Pageable pageable, Profesional profesional) {
        if (buscar != null && !buscar.isBlank()) {
            return pacienteRepository
                    .buscar(profesional.getId(), buscar.trim(), pageable)
                    .map(this::toResponse);
        }
        return pacienteRepository
                .findByProfesionalId(profesional.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest req, Profesional profesional) {
        Paciente paciente = Paciente.builder()
                .profesional(profesional)
                .nombre(req.nombre())
                .apellido(req.apellido())
                .dni(req.dni())
                .fechaNac(req.fechaNac())
                .telefono(req.telefono())
                .email(req.email())
                .direccion(req.direccion())
                .obraSocial(req.obraSocial())
                .nroAfiliado(req.nroAfiliado())
                .build();
        paciente = pacienteRepository.save(paciente);

        odontogramaRepository.save(Odontograma.builder()
                .paciente(paciente)
                .superficies(new HashMap<>())
                .build());

        return toResponse(paciente);
    }

    public PacienteResponse obtener(Long id, Profesional profesional) {
        return toResponse(findOwned(id, profesional));
    }

    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest req, Profesional profesional) {
        Paciente p = findOwned(id, profesional);
        p.setNombre(req.nombre());
        p.setApellido(req.apellido());
        p.setDni(req.dni());
        p.setFechaNac(req.fechaNac());
        p.setTelefono(req.telefono());
        p.setEmail(req.email());
        p.setDireccion(req.direccion());
        p.setObraSocial(req.obraSocial());
        p.setNroAfiliado(req.nroAfiliado());
        return toResponse(pacienteRepository.save(p));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        pacienteRepository.delete(findOwned(id, profesional));
    }

    public OdontogramaResponse obtenerOdontograma(Long pacienteId, Profesional profesional) {
        findOwned(pacienteId, profesional);
        return toOdontogramaResponse(findOdontograma(pacienteId));
    }

    @Transactional
    public OdontogramaResponse guardarOdontograma(Long pacienteId, OdontogramaRequest req, Profesional profesional) {
        findOwned(pacienteId, profesional);
        Odontograma od = findOdontograma(pacienteId);
        od.setSuperficies(req.superficies());
        return toOdontogramaResponse(odontogramaRepository.save(od));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Paciente findOwned(Long id, Profesional profesional) {
        return pacienteRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
    }

    private Odontograma findOdontograma(Long pacienteId) {
        return odontogramaRepository.findFirstByPacienteIdOrderByLastUpdatedDesc(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Odontograma no encontrado"));
    }

    private PacienteResponse toResponse(Paciente p) {
        return new PacienteResponse(
                p.getId(), p.getNombre(), p.getApellido(), p.getDni(),
                p.getFechaNac(), p.getTelefono(), p.getEmail(), p.getDireccion(),
                p.getObraSocial(), p.getNroAfiliado(),
                p.getDateCreated(), p.getLastUpdated()
        );
    }

    private OdontogramaResponse toOdontogramaResponse(Odontograma od) {
        return new OdontogramaResponse(
                od.getId(), od.getPaciente().getId(),
                od.getSuperficies(), od.getDateCreated(), od.getLastUpdated()
        );
    }
}
