package com.simpleodonto.paciente.dto;

/**
 * Una obra social asociada al paciente. Se usa en {@code PacienteRequest} (entrada)
 * y en {@code PacienteResponse} (salida).
 * <p>
 * - En la entrada: {@code obraSocialId} es obligatorio; los demás pueden ser null.
 * - En la salida: {@code obraSocialNombre} viene cargado para evitar lookups en el front.
 */
public record PacienteObraSocialDto(
        Long   obraSocialId,
        String obraSocialNombre,
        String nroAfiliado,
        String plan,
        String titular
) {
    public PacienteObraSocialDto(Long obraSocialId, String nroAfiliado, String plan, String titular) {
        this(obraSocialId, null, nroAfiliado, plan, titular);
    }
}
