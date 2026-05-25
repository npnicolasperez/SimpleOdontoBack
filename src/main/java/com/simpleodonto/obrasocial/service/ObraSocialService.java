package com.simpleodonto.obrasocial.service;

import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.obrasocial.dto.ObraSocialRequest;
import com.simpleodonto.obrasocial.dto.ObraSocialResponse;
import com.simpleodonto.obrasocial.repository.ObraSocialRepository;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ObraSocialService {

    private final ObraSocialRepository obraSocialRepository;
    private final PacienteRepository   pacienteRepository;

    public List<ObraSocialResponse> listar(Profesional profesional) {
        return obraSocialRepository.findByProfesionalId(profesional.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ObraSocialResponse crear(ObraSocialRequest req, Profesional profesional) {
        ObraSocial obraSocial = ObraSocial.builder()
                .profesional(profesional)
                .nombre(req.nombre())
                .build();
        return toResponse(obraSocialRepository.save(obraSocial));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        ObraSocial obraSocial = obraSocialRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Obra social no encontrada"));
        if (pacienteRepository.existsByObraSocialId(id))
            throw new IllegalStateException("No se puede eliminar la obra social porque tiene pacientes asociados.");
        obraSocialRepository.delete(obraSocial);
    }

    private ObraSocialResponse toResponse(ObraSocial o) {
        return new ObraSocialResponse(
                o.getId(), o.getNombre(),
                o.getDateCreated(), o.getLastUpdated()
        );
    }
}
