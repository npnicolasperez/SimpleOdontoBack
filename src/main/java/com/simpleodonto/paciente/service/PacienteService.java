package com.simpleodonto.paciente.service;

import com.simpleodonto.consulta.repository.ConsultaRepository;
import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.obrasocial.repository.ObraSocialRepository;
import com.simpleodonto.paciente.domain.Odontograma;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.domain.PacienteObraSocial;
import com.simpleodonto.paciente.dto.OdontogramaRequest;
import com.simpleodonto.paciente.dto.OdontogramaResponse;
import com.simpleodonto.paciente.dto.PacienteObraSocialDto;
import com.simpleodonto.paciente.dto.PacienteRequest;
import com.simpleodonto.paciente.dto.PacienteResponse;
import com.simpleodonto.paciente.dto.PacienteStatsResponse;
import com.simpleodonto.paciente.repository.OdontogramaRepository;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.turno.repository.TurnoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository    pacienteRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final PacienteStatsService  pacienteStatsService;
    private final ObraSocialRepository  obraSocialRepository;
    private final ConsultaRepository    consultaRepository;
    private final TurnoRepository       turnoRepository;

    public Page<PacienteResponse> listar(String buscar, Pageable pageable, Profesional profesional) {
        Page<Paciente> page = (buscar != null && !buscar.isBlank())
                ? pacienteRepository.buscar(profesional.getId(), buscar.trim(), pageable)
                : pacienteRepository.findByProfesionalId(profesional.getId(), pageable);

        List<Long> ids = page.getContent().stream().map(Paciente::getId).toList();
        Map<Long, LocalDate> ultimaVisitaByPacId = new HashMap<>();
        Map<Long, LocalDate> proximoTurnoByPacId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : consultaRepository.findMaxFechaByPacienteIds(profesional.getId(), ids)) {
                ultimaVisitaByPacId.put((Long) row[0], (LocalDate) row[1]);
            }
            LocalDateTime ahora = LocalDateTime.now();
            for (Object[] row : turnoRepository.findProximoTurnoByPacienteIds(profesional.getId(), ids, ahora)) {
                proximoTurnoByPacId.put((Long) row[0], ((LocalDateTime) row[1]).toLocalDate());
            }
        }
        return page.map(p -> toResponse(p, ultimaVisitaByPacId.get(p.getId()), proximoTurnoByPacId.get(p.getId())));
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
                .obrasSociales(new ArrayList<>())
                .ocupacion(req.ocupacion())
                .grupoSanguineo(req.grupoSanguineo())
                .alergias(req.alergias())
                .medicaciones(req.medicaciones())
                .antecedentes(req.antecedentes())
                .antecedentesFamiliares(req.antecedentesFamiliares())
                .peso(req.peso())
                .altura(req.altura())
                .build();
        aplicarObrasSociales(paciente, req.obrasSociales(), profesional);
        paciente = pacienteRepository.save(paciente);

        odontogramaRepository.save(Odontograma.builder()
                .paciente(paciente)
                .superficies(new HashMap<>())
                .build());

        return toResponse(paciente);
    }

    public PacienteResponse obtener(Long id, Profesional profesional) {
        Paciente p = findOwned(id, profesional);
        LocalDate proximoTurno = turnoRepository
                .findProximoTurnoPaciente(profesional.getId(), p.getId(), LocalDateTime.now())
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
        return toResponse(p, null, proximoTurno);
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
        aplicarObrasSociales(p, req.obrasSociales(), profesional);
        p.setOcupacion(req.ocupacion());
        p.setGrupoSanguineo(req.grupoSanguineo());
        p.setAlergias(req.alergias());
        p.setMedicaciones(req.medicaciones());
        p.setAntecedentes(req.antecedentes());
        p.setAntecedentesFamiliares(req.antecedentesFamiliares());
        p.setPeso(req.peso());
        p.setAltura(req.altura());
        return toResponse(pacienteRepository.save(p));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        pacienteRepository.delete(findOwned(id, profesional));
    }

    public List<OdontogramaResponse> listarOdontogramas(Long pacienteId, Profesional profesional) {
        findOwned(pacienteId, profesional);
        return odontogramaRepository.findByPacienteIdOrderByDateCreatedDesc(pacienteId)
                .stream().map(this::toOdontogramaResponse).toList();
    }

    @Transactional
    public OdontogramaResponse crearNuevoOdontograma(Long pacienteId, Profesional profesional) {
        Paciente paciente = findOwned(pacienteId, profesional);
        return toOdontogramaResponse(odontogramaRepository.save(
                Odontograma.builder().paciente(paciente).superficies(new HashMap<>()).build()));
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

    public PacienteStatsResponse getStats(Profesional profesional) {
        Long id = profesional.getId();

        CompletableFuture<Long> totalF    = pacienteStatsService.contarTotal(id);
        CompletableFuture<Long> nuevosF   = pacienteStatsService.contarNuevosEsteMes(id);
        CompletableFuture<Long> conTurnoF = pacienteStatsService.contarConTurnoProximo(id);

        CompletableFuture.allOf(totalF, nuevosF, conTurnoF).join();

        return new PacienteStatsResponse(totalF.join(), nuevosF.join(), conTurnoF.join());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Reconcilia la lista de obras sociales del paciente con la entrada del request.
     * - Filtra entradas sin obraSocialId (UI puede mandar rows vacías).
     * - Deduplica por obraSocialId (la primera ocurrencia gana).
     * - Verifica que cada obraSocial pertenezca al profesional.
     * - Asigna {@code orden} secuencial empezando en 0 (la primera de la lista = principal).
     * <p>
     * Importante: NO usa clear()+add() porque con orphanRemoval=true Hibernate ejecuta los INSERT
     * antes de los DELETE en el mismo flush, lo que choca contra la unique constraint
     * (paciente_id, obra_social_id) cuando una OS ya existente se "re-agrega". En su lugar hace un
     * merge: reusa las asociaciones existentes (sólo actualizando sus campos), elimina las que ya
     * no están y agrega únicamente las nuevas.
     */
    private void aplicarObrasSociales(Paciente paciente, List<PacienteObraSocialDto> entrada, Profesional profesional) {
        Map<Long, PacienteObraSocialDto> deseado = new LinkedHashMap<>();
        if (entrada != null) {
            for (PacienteObraSocialDto dto : entrada) {
                if (dto == null || dto.obraSocialId() == null) continue;
                deseado.putIfAbsent(dto.obraSocialId(), dto);
            }
        }

        Map<Long, PacienteObraSocial> existentes = paciente.getObrasSociales().stream()
                .collect(Collectors.toMap(pos -> pos.getObraSocial().getId(), pos -> pos, (a, b) -> a));

        // Quitar las asociaciones existentes que ya no están en la entrada → orphan removal las borra
        paciente.getObrasSociales().removeIf(pos -> !deseado.containsKey(pos.getObraSocial().getId()));

        // Actualizar las que siguen + agregar las nuevas, asignando orden secuencial
        int orden = 0;
        for (Map.Entry<Long, PacienteObraSocialDto> e : deseado.entrySet()) {
            Long osId = e.getKey();
            PacienteObraSocialDto dto = e.getValue();
            PacienteObraSocial existente = existentes.get(osId);
            if (existente != null) {
                existente.setNroAfiliado(dto.nroAfiliado());
                existente.setPlan(dto.plan());
                existente.setTitular(dto.titular());
                existente.setOrden(orden);
            } else {
                ObraSocial os = obraSocialRepository.findByIdAndProfesionalId(osId, profesional.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Obra social no encontrada: " + osId));
                paciente.getObrasSociales().add(PacienteObraSocial.builder()
                        .paciente(paciente)
                        .obraSocial(os)
                        .nroAfiliado(dto.nroAfiliado())
                        .plan(dto.plan())
                        .titular(dto.titular())
                        .orden(orden)
                        .build());
            }
            orden++;
        }
    }

    private Paciente findOwned(Long id, Profesional profesional) {
        return pacienteRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
    }

    private Odontograma findOdontograma(Long pacienteId) {
        return odontogramaRepository.findFirstByPacienteIdOrderByLastUpdatedDesc(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Odontograma no encontrado"));
    }

    private PacienteResponse toResponse(Paciente p) {
        return toResponse(p, null, null);
    }

    private PacienteResponse toResponse(Paciente p, LocalDate ultimaVisita, LocalDate proximoTurno) {
        List<PacienteObraSocialDto> osDtos = p.getObrasSociales() == null
                ? Collections.emptyList()
                : p.getObrasSociales().stream()
                    .map(pos -> new PacienteObraSocialDto(
                            pos.getObraSocial().getId(),
                            pos.getObraSocial().getNombre(),
                            pos.getNroAfiliado(),
                            pos.getPlan(),
                            pos.getTitular()))
                    .toList();
        return new PacienteResponse(
                p.getId(), p.getNombre(), p.getApellido(), p.getDni(),
                p.getFechaNac(), p.getTelefono(), p.getEmail(), p.getDireccion(),
                osDtos,
                p.getOcupacion(), p.getGrupoSanguineo(),
                p.getAlergias(), p.getMedicaciones(), p.getAntecedentes(),
                p.getAntecedentesFamiliares(), p.getPeso(), p.getAltura(),
                p.getDateCreated(), p.getLastUpdated(),
                ultimaVisita, proximoTurno
        );
    }

    private OdontogramaResponse toOdontogramaResponse(Odontograma od) {
        return new OdontogramaResponse(
                od.getId(), od.getPaciente().getId(),
                od.getSuperficies(), od.getDateCreated(), od.getLastUpdated()
        );
    }
}
