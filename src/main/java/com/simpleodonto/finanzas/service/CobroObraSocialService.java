package com.simpleodonto.finanzas.service;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.finanzas.domain.CobroObraSocial;
import com.simpleodonto.finanzas.domain.EstadoIngreso;
import com.simpleodonto.finanzas.domain.Ingreso;
import com.simpleodonto.finanzas.domain.MedioPago;
import com.simpleodonto.finanzas.dto.CobroObraSocialDetalleResponse;
import com.simpleodonto.finanzas.dto.CobroObraSocialDetalleResponse.IngresoCubiertoDto;
import com.simpleodonto.finanzas.dto.CobroObraSocialRequest;
import com.simpleodonto.finanzas.dto.CobroObraSocialResponse;
import com.simpleodonto.finanzas.dto.IngresoPendientePorOsResponse;
import com.simpleodonto.finanzas.repository.CobroObraSocialRepository;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.finanzas.repository.MedioPagoRepository;
import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.obrasocial.repository.ObraSocialRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CobroObraSocialService {

    private final CobroObraSocialRepository cobroRepository;
    private final IngresoRepository         ingresoRepository;
    private final ObraSocialRepository      obraSocialRepository;
    private final ConsultorioRepository     consultorioRepository;
    private final MedioPagoRepository       medioPagoRepository;

    /** Lista los ingresos PENDIENTES con tipoPago=PARTICULAR del profesional (pestaña Particular). */
    public List<IngresoPendientePorOsResponse> listarPendientesParticulares(Profesional profesional) {
        return ingresoRepository.findPendientesParticulares(profesional.getId())
                .stream()
                .map(this::toPendienteResponse)
                .toList();
    }

    /**
     * Lista los ingresos pendientes de una OS en un consultorio puntual. Cada OS paga por separado
     * a cada consultorio del profesional, así que el flow es siempre OS + consultorio.
     */
    public List<IngresoPendientePorOsResponse> listarPendientesPorOs(Long obraSocialId,
                                                                     Long consultorioId,
                                                                     Profesional profesional) {
        obraSocialRepository.findByIdAndProfesionalId(obraSocialId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Obra social no encontrada"));
        consultorioRepository.findByIdAndProfesionalId(consultorioId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        return ingresoRepository.findPendientesByObraSocialYConsultorio(profesional.getId(), obraSocialId, consultorioId)
                .stream()
                .map(this::toPendienteResponse)
                .toList();
    }

    private IngresoPendientePorOsResponse toPendienteResponse(Ingreso i) {
        var consulta = i.getConsulta();
        return new IngresoPendientePorOsResponse(
                i.getId(),
                consulta != null ? consulta.getId() : null,
                i.getFecha(),
                consulta != null ? consulta.getPaciente().getApellido() : null,
                consulta != null ? consulta.getPaciente().getNombre()   : null,
                consulta != null && consulta.getDescripcion() != null ? consulta.getDescripcion() : i.getDescripcion(),
                i.getMonto(),
                i.getConsultorio() != null ? i.getConsultorio().getId()     : null,
                i.getConsultorio() != null ? i.getConsultorio().getNombre() : null,
                i.getObraSocial()  != null ? i.getObraSocial().getNombre()  : null
        );
    }

    /**
     * Crea el cobro y cierra los ingresos seleccionados (PENDIENTE → CONFIRMADO).
     * El cobro pertenece a un único consultorio (las OS pagan por separado a cada consultorio).
     * No distribuimos el monto entre los ingresos: el cobro ES el evento de ingreso en finanzas
     * (con monto único), y los ingresos individuales quedan sin monto. Finanzas trata al cobro
     * como un ingreso virtual y excluye los ingresos cubiertos para no contar doble.
     */
    @Transactional
    public CobroObraSocialResponse crear(CobroObraSocialRequest req, Profesional profesional) {
        ObraSocial os = obraSocialRepository.findByIdAndProfesionalId(req.obraSocialId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Obra social no encontrada"));

        Consultorio consultorio = consultorioRepository.findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));

        MedioPago medioPago = req.medioPagoId() != null
                ? medioPagoRepository.findByIdAndProfesionalId(req.medioPagoId(), profesional.getId()).orElse(null)
                : null;

        CobroObraSocial cobro = CobroObraSocial.builder()
                .profesional(profesional)
                .obraSocial(os)
                .consultorio(consultorio)
                .fecha(req.fecha())
                .montoRecibido(req.montoRecibido())
                .medioPago(medioPago)
                .descripcion(req.descripcion())
                .build();
        cobro = cobroRepository.save(cobro);

        List<Ingreso> ingresos = ingresoRepository.findAllById(req.ingresoIds());
        if (ingresos.size() != req.ingresoIds().size()) {
            throw new EntityNotFoundException("Alguno de los ingresos seleccionados no existe.");
        }
        for (Ingreso ingreso : ingresos) {
            if (!ingreso.getProfesional().getId().equals(profesional.getId())) {
                throw new IllegalArgumentException("Ingreso fuera del profesional: " + ingreso.getId());
            }
            if (ingreso.getEstado() != EstadoIngreso.PENDIENTE) {
                throw new IllegalArgumentException("El ingreso " + ingreso.getId() + " no está pendiente.");
            }
            if (ingreso.getObraSocial() == null || !ingreso.getObraSocial().getId().equals(os.getId())) {
                throw new IllegalArgumentException("El ingreso " + ingreso.getId() + " no es de la obra social seleccionada.");
            }
            if (ingreso.getConsultorio() == null || !ingreso.getConsultorio().getId().equals(consultorio.getId())) {
                throw new IllegalArgumentException("El ingreso " + ingreso.getId() + " no es del consultorio del cobro.");
            }
            if (ingreso.getCobroObraSocial() != null) {
                throw new IllegalArgumentException("El ingreso " + ingreso.getId() + " ya fue cubierto por otro cobro.");
            }
        }

        for (Ingreso ingreso : ingresos) {
            ingreso.setEstado(EstadoIngreso.CONFIRMADO);
            ingreso.setCobroObraSocial(cobro);
            // ingreso.monto queda null a propósito: el monto vive a nivel cobro. Finanzas suma el cobro
            // (como ingreso virtual) y excluye estos ingresos para no contar doble.
        }
        ingresoRepository.saveAll(ingresos);

        return toResponse(cobro, req.montoRecibido(), ingresos.size());
    }

    /** Lista cobros del mes (yyyy-MM). */
    @Transactional(readOnly = true)
    public List<CobroObraSocialResponse> listar(Profesional profesional, YearMonth ym) {
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.plusMonths(1).atDay(1);
        return cobroRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta)
                .stream()
                .map(c -> {
                    long cantidad = ingresoRepository.findByCobroObraSocialId(c.getId()).size();
                    // montoEsperado = montoRecibido para que la UI no muestre diferencia: el cobro es el ingreso real.
                    return toResponse(c, c.getMontoRecibido(), cantidad);
                })
                .toList();
    }

    /** Detalle de un cobro: incluye la lista de ingresos cubiertos. */
    @Transactional(readOnly = true)
    public CobroObraSocialDetalleResponse obtener(Long id, Profesional profesional) {
        CobroObraSocial c = cobroRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cobro no encontrado"));
        List<Ingreso> ings = ingresoRepository.findByCobroObraSocialId(c.getId());

        List<IngresoCubiertoDto> dtos = ings.stream()
                .map(i -> new IngresoCubiertoDto(
                        i.getId(),
                        i.getConsulta() != null ? i.getConsulta().getId() : null,
                        i.getFecha(),
                        i.getConsulta() != null ? i.getConsulta().getPaciente().getApellido() : null,
                        i.getConsulta() != null ? i.getConsulta().getPaciente().getNombre()   : null,
                        i.getDescripcion(),
                        i.getMonto()
                ))
                .toList();

        return new CobroObraSocialDetalleResponse(
                c.getId(),
                c.getObraSocial().getId(),
                c.getObraSocial().getNombre(),
                c.getConsultorio() != null ? c.getConsultorio().getId()     : null,
                c.getConsultorio() != null ? c.getConsultorio().getNombre() : null,
                c.getFecha(),
                c.getMontoRecibido(),
                c.getMontoRecibido(),
                c.getMedioPago() != null ? c.getMedioPago().getId()     : null,
                c.getMedioPago() != null ? c.getMedioPago().getNombre() : null,
                c.getDescripcion(),
                dtos,
                c.getDateCreated()
        );
    }

    /**
     * Elimina un cobro: los ingresos que cerraba vuelven a PENDIENTE (sin cobroObraSocial).
     */
    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        CobroObraSocial c = cobroRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cobro no encontrado"));
        List<Ingreso> ings = ingresoRepository.findByCobroObraSocialId(c.getId());
        for (Ingreso i : ings) {
            i.setEstado(EstadoIngreso.PENDIENTE);
            i.setCobroObraSocial(null);
        }
        ingresoRepository.saveAll(ings);
        cobroRepository.delete(c);
    }

    private CobroObraSocialResponse toResponse(CobroObraSocial c, BigDecimal montoEsperado, long cantidad) {
        return new CobroObraSocialResponse(
                c.getId(),
                c.getObraSocial().getId(),
                c.getObraSocial().getNombre(),
                c.getConsultorio() != null ? c.getConsultorio().getId()     : null,
                c.getConsultorio() != null ? c.getConsultorio().getNombre() : null,
                c.getFecha(),
                c.getMontoRecibido(),
                montoEsperado,
                cantidad,
                c.getMedioPago() != null ? c.getMedioPago().getId()     : null,
                c.getMedioPago() != null ? c.getMedioPago().getNombre() : null,
                c.getDescripcion(),
                c.getDateCreated()
        );
    }
}
