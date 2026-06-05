package com.simpleodonto.firma.controller;

import com.simpleodonto.firma.dto.EstadoFirmaResponse;
import com.simpleodonto.firma.dto.FirmaPendienteResponse;
import com.simpleodonto.firma.dto.FirmarRequest;
import com.simpleodonto.firma.dto.SolicitarFirmaRequest;
import com.simpleodonto.firma.service.FirmaService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/firmas")
@RequiredArgsConstructor
public class FirmaController {

    private final TokenService  tokenService;
    private final FirmaService  firmaService;

    /** Desktop crea el slot de firma pendiente. */
    @PostMapping("/solicitar")
    public ResponseEntity<Void> solicitar(
            @Valid @RequestBody SolicitarFirmaRequest req,
            HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        firmaService.solicitar(req.consultaId(), prof);
        return ResponseEntity.noContent().build();
    }

    /** Mobile chequea si hay una solicitud pendiente. 204 si no hay. */
    @GetMapping("/pendiente")
    public ResponseEntity<FirmaPendienteResponse> pendiente(HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        return firmaService.obtenerPendiente(prof)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Mobile envía el PNG firmado. */
    @PutMapping("/firmar")
    public ResponseEntity<Void> firmar(
            @Valid @RequestBody FirmarRequest req,
            HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        firmaService.firmar(req.consultaId(), req.pngBase64(), prof);
        return ResponseEntity.noContent().build();
    }

    /** Desktop pollea para saber si la firma ya fue completada. */
    @GetMapping("/consulta/{consultaId}/estado")
    public EstadoFirmaResponse estado(
            @PathVariable Long consultaId,
            HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        return firmaService.estado(consultaId, prof);
    }

    /** Desktop cancela una solicitud (cerró el modal). */
    @DeleteMapping("/consulta/{consultaId}/solicitud")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long consultaId,
            HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        firmaService.cancelarSolicitud(consultaId, prof);
        return ResponseEntity.noContent().build();
    }

    /** Devuelve el PNG de la firma (para verla luego en la consulta). */
    @GetMapping("/consulta/{consultaId}/png")
    public ResponseEntity<byte[]> obtenerPng(
            @PathVariable Long consultaId,
            HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        byte[] png = firmaService.obtenerPng(consultaId, prof);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
