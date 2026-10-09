package com.tpouade.multiteatro.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.TicketService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;
    private final TicketService ticketService;

    public TicketController(ContextoSesion contextoSesion,
                            AutorizacionService autorizacionService,
                            TicketService ticketService) {
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
        this.ticketService = ticketService;
    }

    /** Tickets del cliente autenticado. */
    @GetMapping("/mios")
    public List<?> getMisTickets(HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        return ticketService.findByUsuario(actual);
    }

    /** Todos los tickets de la plataforma. Solo SUPERADMIN. */
    @GetMapping
    public List<?> getAllTickets(HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.SUPERADMIN);
        return ticketService.findAll();
    }
}
