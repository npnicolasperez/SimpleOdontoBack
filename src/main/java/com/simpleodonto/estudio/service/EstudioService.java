package com.simpleodonto.estudio.service;

import com.simpleodonto.estudio.domain.Estudio;
import com.simpleodonto.estudio.dto.EstudioDetalleResponse;
import com.simpleodonto.estudio.dto.EstudioResponse;
import com.simpleodonto.estudio.dto.EstudioUpdateRequest;
import com.simpleodonto.estudio.repository.EstudioRepository;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstudioService {

    private final EstudioRepository estudioRepository;
    private final PacienteRepository pacienteRepository;

    public Page<EstudioResponse> listar(String buscar, Pageable pageable, Profesional profesional) {
        Page<Estudio> page = (buscar != null && !buscar.isBlank())
                ? estudioRepository.buscar(profesional.getId(), buscar, pageable)
                : estudioRepository.findByProfesionalId(profesional.getId(), pageable);
        return page.map(this::toResponse);
    }

    public List<EstudioResponse> listarPorPaciente(Long pacienteId, Profesional profesional) {
        return estudioRepository.findByProfesionalIdAndPacienteIdOrderByLastUpdatedDesc(profesional.getId(), pacienteId)
                .stream().map(this::toResponse).toList();
    }

    public EstudioDetalleResponse obtener(Long id, Profesional profesional) {
        return toDetalleResponse(findOwned(id, profesional));
    }

    @Transactional
    public EstudioResponse crear(String nombre, String imagenTipo, byte[] imagenBytes,
                                 Double escala, Long pacienteId, Profesional profesional) {
        if (pacienteId == null) {
            throw new IllegalArgumentException("El estudio debe estar asociado a un paciente.");
        }
        Paciente paciente = resolverPaciente(pacienteId, profesional);
        Estudio e = Estudio.builder()
                .profesional(profesional)
                .paciente(paciente)
                .nombre(nombre)
                .imagenTipo(imagenTipo)
                .imagen(imagenBytes)
                .trazos(new ArrayList<>())
                .escala(escala != null ? escala : 1.0)
                .build();
        return toResponse(estudioRepository.save(e));
    }

    @Transactional
    public EstudioDetalleResponse actualizar(Long id, EstudioUpdateRequest req, Profesional profesional) {
        Estudio e = findOwned(id, profesional);
        e.setTrazos(req.trazos());
        e.setEscala(req.escala());
        if (req.pacienteId() != null) {
            e.setPaciente(resolverPaciente(req.pacienteId(), profesional));
        }
        if (req.descripcion() != null) {
            e.setDescripcion(req.descripcion());
        }
        return toDetalleResponse(estudioRepository.save(e));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        estudioRepository.delete(findOwned(id, profesional));
    }

    private Paciente resolverPaciente(Long pacienteId, Profesional profesional) {
        if (pacienteId == null) return null;
        return pacienteRepository.findByIdAndProfesionalId(pacienteId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
    }

    private Estudio findOwned(Long id, Profesional profesional) {
        return estudioRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Estudio no encontrado"));
    }

    private EstudioResponse toResponse(Estudio e) {
        return new EstudioResponse(
                e.getId(), e.getNombre(), e.getImagenTipo(), e.getEscala(),
                e.getPaciente() != null ? e.getPaciente().getId()        : null,
                e.getPaciente() != null ? e.getPaciente().getApellido()  : null,
                e.getPaciente() != null ? e.getPaciente().getNombre()    : null,
                e.getTrazos().size(),
                e.getDateCreated(), e.getLastUpdated()
        );
    }

    private EstudioDetalleResponse toDetalleResponse(Estudio e) {
        return new EstudioDetalleResponse(
                e.getId(), e.getNombre(), e.getImagenTipo(),
                Base64.getEncoder().encodeToString(e.getImagen()),
                e.getEscala(),
                e.getPaciente() != null ? e.getPaciente().getId()        : null,
                e.getPaciente() != null ? e.getPaciente().getApellido()  : null,
                e.getPaciente() != null ? e.getPaciente().getNombre()    : null,
                e.getTrazos(),
                e.getDescripcion(),
                e.getDateCreated(), e.getLastUpdated()
        );
    }
}
