package com.simpleodonto.consulta.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consulta.domain.ConsultaArchivo;
import com.simpleodonto.consulta.dto.ArchivoInfo;
import com.simpleodonto.consulta.dto.ConsultaRequest;
import com.simpleodonto.consulta.dto.ConsultaResponse;
import com.simpleodonto.consulta.dto.ConsultaUpdateRequest;
import com.simpleodonto.consulta.repository.ConsultaArchivoRepository;
import com.simpleodonto.consulta.repository.ConsultaRepository;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.finanzas.service.IngresoService;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository        consultaRepository;
    private final ConsultaArchivoRepository consultaArchivoRepository;
    private final PacienteRepository        pacienteRepository;
    private final ConsultorioRepository     consultorioRepository;
    private final IngresoService            ingresoService;
    private final IngresoRepository         ingresoRepository;

    public List<ConsultaResponse> listarPorPaciente(Long pacienteId, Profesional profesional) {
        var sort = Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "dateCreated"));
        return consultaRepository.findByPacienteIdAndProfesionalId(pacienteId, profesional.getId(), sort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConsultaResponse> listarUltimas(Profesional profesional) {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dateCreated"));
        return consultaRepository.findByProfesionalId(profesional.getId(), pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConsultaResponse crear(ConsultaRequest req, Profesional profesional) {
        Paciente paciente = pacienteRepository
                .findByIdAndProfesionalId(req.pacienteId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));

        Consultorio consultorio = null;
        if (req.consultorioId() != null) {
            consultorio = consultorioRepository
                    .findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        }

        Consulta consulta = Consulta.builder()
                .paciente(paciente)
                .profesional(profesional)
                .consultorio(consultorio)
                .fecha(req.fecha() != null ? req.fecha() : LocalDate.now())
                .descripcion(req.descripcion())
                .monto(req.monto())
                .tipoPago(req.tipoPago())
                .build();

        Consulta saved = consultaRepository.save(consulta);
        ingresoService.crearDesdeConsulta(saved, req.medioPagoId());
        return toResponse(saved);
    }

    @Transactional
    public ConsultaResponse actualizar(Long consultaId, ConsultaUpdateRequest req, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));

        Consultorio consultorio = null;
        if (req.consultorioId() != null) {
            consultorio = consultorioRepository
                    .findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        }

        consulta.setConsultorio(consultorio);
        if (req.fecha() != null) consulta.setFecha(req.fecha());
        consulta.setDescripcion(req.descripcion());
        consulta.setMonto(req.monto());
        consulta.setTipoPago(req.tipoPago());

        Consulta saved = consultaRepository.save(consulta);
        ingresoService.actualizarDesdeConsulta(saved, req.medioPagoId());
        return toResponse(saved);
    }

    @Transactional
    public void eliminar(Long consultaId, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        ingresoService.eliminarPorConsulta(consultaId);
        consultaArchivoRepository.deleteAll(consultaArchivoRepository.findByConsultaId(consultaId));
        consultaRepository.delete(consulta);
    }

    @Transactional
    public ArchivoInfo agregarArchivo(Long consultaId, MultipartFile archivo, Profesional profesional) {
        Consulta consulta = consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        try {
            ConsultaArchivo ca = ConsultaArchivo.builder()
                    .consulta(consulta)
                    .nombre(archivo.getOriginalFilename())
                    .tipo(archivo.getContentType())
                    .data(archivo.getBytes())
                    .build();
            ConsultaArchivo saved = consultaArchivoRepository.save(ca);
            return new ArchivoInfo(saved.getId(), saved.getNombre(), saved.getTipo());
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo adjunto", e);
        }
    }

    public ResponseEntity<byte[]> descargarArchivo(Long consultaId, Long archivoId, Profesional profesional) {
        consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        ConsultaArchivo ca = consultaArchivoRepository.findByIdAndConsultaId(archivoId, consultaId)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ca.getTipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + ca.getNombre() + "\"")
                .body(ca.getData());
    }

    @Transactional
    public void eliminarArchivo(Long consultaId, Long archivoId, Profesional profesional) {
        consultaRepository.findByIdAndProfesionalId(consultaId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada"));
        ConsultaArchivo ca = consultaArchivoRepository.findByIdAndConsultaId(archivoId, consultaId)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado"));
        consultaArchivoRepository.delete(ca);
    }

    private ConsultaResponse toResponse(Consulta c) {
        List<ArchivoInfo> archivos = consultaArchivoRepository.findByConsultaId(c.getId())
                .stream()
                .map(a -> new ArchivoInfo(a.getId(), a.getNombre(), a.getTipo()))
                .toList();
        return ingresoRepository.findByConsultaId(c.getId())
                .map(ingreso -> new ConsultaResponse(
                        c.getId(),
                        c.getPaciente().getId(),
                        c.getPaciente().getApellido(),
                        c.getPaciente().getNombre(),
                        c.getProfesional().getId(),
                        c.getConsultorio() != null ? c.getConsultorio().getId()     : null,
                        c.getConsultorio() != null ? c.getConsultorio().getNombre() : null,
                        c.getFecha(),
                        c.getDescripcion(),
                        c.getMonto(),
                        c.getTipoPago(),
                        ingreso.getMedioPago() != null ? ingreso.getMedioPago().getId()     : null,
                        ingreso.getMedioPago() != null ? ingreso.getMedioPago().getNombre() : null,
                        ingreso.getEstado(),
                        c.getDateCreated(),
                        c.getLastUpdated(),
                        archivos
                ))
                .orElseGet(() -> new ConsultaResponse(
                        c.getId(),
                        c.getPaciente().getId(),
                        c.getPaciente().getApellido(),
                        c.getPaciente().getNombre(),
                        c.getProfesional().getId(),
                        c.getConsultorio() != null ? c.getConsultorio().getId()     : null,
                        c.getConsultorio() != null ? c.getConsultorio().getNombre() : null,
                        c.getFecha(),
                        c.getDescripcion(),
                        c.getMonto(),
                        c.getTipoPago(),
                        null, null, null,
                        c.getDateCreated(),
                        c.getLastUpdated(),
                        archivos
                ));
    }
}
