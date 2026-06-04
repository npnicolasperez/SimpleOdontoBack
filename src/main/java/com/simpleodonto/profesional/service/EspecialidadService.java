package com.simpleodonto.profesional.service;

import com.simpleodonto.profesional.domain.Especialidad;
import com.simpleodonto.profesional.dto.EspecialidadRequest;
import com.simpleodonto.profesional.dto.EspecialidadResponse;
import com.simpleodonto.profesional.repository.EspecialidadRepository;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final ProfesionalRepository  profesionalRepository;

    public List<EspecialidadResponse> listar() {
        return especialidadRepository.findAll().stream()
                .map(e -> new EspecialidadResponse(e.getId(), e.getNombre()))
                .toList();
    }

    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest req) {
        String nombre = req.nombre().trim();
        if (especialidadRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre");
        }
        Especialidad e = Especialidad.builder().nombre(nombre).build();
        e = especialidadRepository.save(e);
        return new EspecialidadResponse(e.getId(), e.getNombre());
    }

    @Transactional
    public void eliminar(Long id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada"));
        if (profesionalRepository.existsByEspecialidadId(id)) {
            throw new IllegalStateException("No se puede eliminar: hay profesionales que la usan");
        }
        especialidadRepository.delete(e);
    }
}
