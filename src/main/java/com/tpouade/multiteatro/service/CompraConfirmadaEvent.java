package com.tpouade.multiteatro.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de dominio: una compra se confirmo.
 *
 * Se publica dentro de la transaccion de compra, pero lo consume un listener con
 * AFTER_COMMIT, que corre recien cuando la transaccion se commiteo de verdad.
 *
 * El motivo: si el mail se enviara DENTRO de la transaccion, y la transaccion
 * fallara despues (por ejemplo, si el ultimo asiento ya estaba vendido), el
 * usuario recibiria un mail de confirmacion de una compra que nunca existio.
 */
public record CompraConfirmadaEvent(
        Long compraId,
        String emailDestinatario,
        String nombreDestinatario,
        BigDecimal montoTotal,
        LocalDateTime fecha,
        List<DatosTicket> tickets) {
}
