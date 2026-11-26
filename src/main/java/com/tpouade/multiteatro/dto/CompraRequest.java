package com.tpouade.multiteatro.dto;

import java.util.List;

public record CompraRequest(
        Long funcionId,
        List<Long> asientoIds,
        TitularRequest titular,
        PagoRequest pago) {

    public record TitularRequest(
            String nombre,
            String dni,
            String email,
            String telefono,
            String localidad) {

    }

    public record PagoRequest(
            String metodo,
            String comprobanteNombre) {

    }
}
