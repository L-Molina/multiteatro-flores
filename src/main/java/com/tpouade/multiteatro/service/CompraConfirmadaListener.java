package com.tpouade.multiteatro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacciona a CompraConfirmadaEvent DESPUES del commit.
 *
 * AFTER_COMMIT es la garantia de que la compra quedo persistida de verdad antes
 * de generar los tickets y mandar el mail. Con BEFORE_COMMIT (o mandandolo
 * directo desde el Service) un rollback posterior dejaria al usuario con un mail
 * de confirmacion de una compra que no existe.
 */
@Component
public class CompraConfirmadaListener {

    private static final Logger log = LoggerFactory.getLogger(CompraConfirmadaListener.class);

    private final TicketService ticketService;
    private final NotificacionService notificacionService;

    public CompraConfirmadaListener(TicketService ticketService, NotificacionService notificacionService) {
        this.ticketService = ticketService;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompraConfirmada(CompraConfirmadaEvent evento) {
        try {
            ticketService.crearTickets(evento.tickets());
            notificacionService.enviarConfirmacion(evento);
        } catch (RuntimeException e) {
            // La compra ya esta confirmada y no se toca. Si falla la emision del
            // comprobante, queda registrado pero no se propaga.
            log.error("Post-compra fallida para la compra {}: {}",
                    evento.compraId(), e.getMessage());
        }
    }
}
