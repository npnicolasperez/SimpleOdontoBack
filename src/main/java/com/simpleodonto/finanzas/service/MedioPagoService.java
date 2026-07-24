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

    /** Nombres de los medios de pago que se crean automáticamente para cada profesional. */
    private static final List<String> DEFECTO = List.of("Efectivo", "Transferencia");

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
                .sistema(false)
                .build();
        return toResponse(medioPagoRepository.save(mp));
    }

    @Transactional
    public MedioPagoResponse actualizar(Long id, MedioPagoRequest req, Profesional profesional) {
        MedioPago mp = medioPagoRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Medio de pago no encontrado"));
        if (mp.isSistema())
            throw new IllegalStateException("Este medio de pago viene por defecto y no puede modificarse.");
        mp.setNombre(req.nombre());
        return toResponse(medioPagoRepository.save(mp));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        MedioPago mp = medioPagoRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Medio de pago no encontrado"));
        if (mp.isSistema())
            throw new IllegalStateException("Este medio de pago viene por defecto y no puede eliminarse.");
        if (ingresoRepository.existsByMedioPagoId(id))
            throw new IllegalStateException("No se puede eliminar el medio de pago porque tiene ingresos asociados.");
        if (cobroObraSocialRepository.existsByMedioPagoId(id))
            throw new IllegalStateException("No se puede eliminar el medio de pago porque tiene cobros de OS asociados.");
        medioPagoRepository.delete(mp);
    }

    /**
     * Asegura que el profesional tenga los medios de pago de sistema ({@link #DEFECTO}). Si ya
     * existe un medio con ese nombre (case-insensitive), lo marca como sistema. Si no existe, lo
     * crea con sistema=true. Idempotente: correr varias veces no crea duplicados.
     */
    @Transactional
    public void bootstrapSistema(Profesional profesional) {
        List<MedioPago> existentes = medioPagoRepository.findByProfesionalId(profesional.getId());
        for (String nombre : DEFECTO) {
            MedioPago match = existentes.stream()
                    .filter(m -> m.getNombre() != null && m.getNombre().equalsIgnoreCase(nombre))
                    .findFirst().orElse(null);
            if (match == null) {
                medioPagoRepository.save(MedioPago.builder()
                        .profesional(profesional)
                        .nombre(nombre)
                        .sistema(true)
                        .build());
            } else if (!match.isSistema()) {
                match.setSistema(true);
                medioPagoRepository.save(match);
            }
        }
    }

    private MedioPagoResponse toResponse(MedioPago mp) {
        return new MedioPagoResponse(mp.getId(), mp.getNombre(), mp.isSistema(), mp.getDateCreated());
    }
}
