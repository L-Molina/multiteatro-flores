package com.tpouade.multiteatro.dto;

import java.math.BigDecimal;

/**
 * Fila del mapa de butacas de una funcion. El frontend no necesita conocer
 * la entidad Asiento completa: con estos datos puede dibujar la sala y marcar
 * las butacas ocupadas.
 */
public record AsientoFuncionDTO(
        Long id,
        String fila,
        Integer numero,
        Long tipoAsientoId,
        String tipoAsiento,
        Double multiplicador,
        BigDecimal precio,
        boolean ocupado) {
}
