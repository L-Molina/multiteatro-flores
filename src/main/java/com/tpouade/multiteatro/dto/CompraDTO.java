package com.tpouade.multiteatro.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tpouade.multiteatro.model.Compra;
import com.tpouade.multiteatro.model.Usuario;

/**
 * Vista de una compra para la API.
 *
 * Existe para poder enmascarar el DNI sin mutar la entidad JPA: si se modificara
 * el campo de una entidad administrada por Hibernate, el valor enmascarado
 * podría terminar escribiéndose en la base al cerrar la transacción.
 */
public record CompraDTO(
        Long id,
        LocalDateTime fecha,
        Double montoTotal,
        String estado,
        BigDecimal total,
        Usuario usuario,
        String titularNombre,
        String titularDni,
        String titularEmail,
        String titularTelefono,
        String titularLocalidad,
        String metodoPago,
        String pagoLabel,
        String comprobanteNombre,
        String comprobanteUrl) {

    public static CompraDTO from(Compra compra, boolean enmascararDni) {
        return new CompraDTO(
                compra.getId(),
                compra.getFecha(),
                compra.getMontoTotal(),
                compra.getEstado(),
                compra.getTotal(),
                compra.getUsuario(),
                compra.getTitularNombre(),
                enmascararDni ? enmascarar(compra.getTitularDni()) : compra.getTitularDni(),
                compra.getTitularEmail(),
                compra.getTitularTelefono(),
                compra.getTitularLocalidad(),
                compra.getMetodoPago(),
                compra.getPagoLabel(),
                compra.getComprobanteNombre(),
                compra.getComprobanteUrl());
    }

    /**
     * Conserva solo los últimos cuatro dígitos. Si el DNI es muy corto o no
     * tiene dígitos, se oculta completo.
     */
    private static String enmascarar(String dni) {
        if (dni == null || dni.isBlank()) {
            return dni;
        }
        String digitos = dni.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return "****";
        }
        return "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 4);
    }
}
