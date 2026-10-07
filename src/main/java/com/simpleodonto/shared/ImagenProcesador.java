package com.simpleodonto.shared;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * Redimensiona/recomprime imágenes subidas por el usuario (fotos de consulta, estudios) antes de
 * guardarlas en la DB. Las fotos de cámara/tablet llegan sin procesar — se vieron casos reales en
 * producción de ~1.8-3.2MB por imagen a resolución nativa (3264x2448 o más).
 *
 * Usado por ConsultaService (adjuntos de consulta) y EstudioController (estudios con calibración/
 * trazado) — ambos comparten el mismo tope y calidad para que el comportamiento sea consistente
 * en toda la app.
 */
@Component
@Slf4j
public class ImagenProcesador {

    // Tope del lado más largo y calidad JPEG al recomprimir. 1920px + 82% es indistinguible a
    // simple vista en pantalla y alcanza de sobra para calibración/medición manual (el click del
    // usuario ya limita la precisión más que la resolución de la imagen), con una reducción de
    // peso de ~80-90% vs. una foto de celular/tablet sin procesar.
    private static final int    MAX_DIMENSION = 1920;
    private static final double JPEG_QUALITY  = 0.82;
    // Solo procesamos formatos que ImageIO decodifica de forma confiable. PDFs, GIFs (podrían ser
    // animados) y cualquier otro tipo se guardan tal cual llegan, sin tocar.
    private static final Set<String> TIPOS_PROCESABLES = Set.of("image/jpeg", "image/png");

    /**
     * Si el archivo es una imagen de un formato procesable, la redimensiona (nunca agranda) y
     * recomprime, aplicando la rotación real según el EXIF de la cámara (así una foto tomada con
     * el dispositivo en horizontal no queda "de costado" al mostrarla — el flag de orientación se
     * resuelve acá y se descarta). Otros formatos (PDF, GIF, etc.) se devuelven sin procesar. Si
     * el procesamiento falla por cualquier motivo, cae al archivo original en vez de bloquear la
     * carga.
     */
    public byte[] procesarSiEsImagen(MultipartFile archivo) throws IOException {
        String tipo = archivo.getContentType();
        if (tipo == null || !TIPOS_PROCESABLES.contains(tipo)) {
            return archivo.getBytes();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(archivo.getInputStream())
                    .size(MAX_DIMENSION, MAX_DIMENSION)
                    .outputQuality(JPEG_QUALITY)
                    .useExifOrientation(true)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("No se pudo procesar la imagen ({}), se guarda sin cambios: {}",
                    archivo.getOriginalFilename(), e.getMessage());
            return archivo.getBytes();
        }
    }
}
