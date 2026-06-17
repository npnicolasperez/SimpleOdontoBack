package com.simpleodonto.finanzas.service;

import com.simpleodonto.finanzas.domain.MedioPago;
import com.simpleodonto.finanzas.dto.MedioPagoRequest;
import com.simpleodonto.finanzas.dto.MedioPagoResponse;
import com.simpleodonto.finanzas.repository.CobroObraSocialRepository;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.finanzas.repository.MedioPagoRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedioPagoService {

    private final MedioPagoRepository         medioPagoRepository;
    private final IngresoRepository           ingresoRepository;
    private final CobroObraSocialRepository   cobroObraSocialRepository;

    public List<MedioPagoResponse> listar(Profesional profesional) {
        return medioPagoRepository.findByProfesionalId(profesional.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MedioPagoResponse crear(MedioPagoRequest req, Profesional profesional) {
        MedioPago mp = MedioPago.builder()
                .profesional(profesional)
                .nombre(req.nombre())
                .build();
        return toResponse(medioPagoRepository.save(mp));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        MedioPago mp = medioPagoRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Medio de pago no encontrado"));
        if (ingresoRepository.existsByMedioPagoId(id))
            throw new IllegalStateException("No se puede eliminar el medio de pago porque tiene ingresos asociados.");
        if (cobroObraSocialRepository.existsByMedioPagoId(id))
            throw new IllegalStateException("No se puede eliminar el medio de pago porque tiene cobros de OS asociados.");
        medioPagoRepository.delete(mp);
    }

    private MedioPagoResponse toResponse(MedioPago mp) {
        return new MedioPagoResponse(mp.getId(), mp.getNombre(), mp.getDateCreated());
    }
}
