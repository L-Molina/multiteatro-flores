package com.tpouade.multiteatro.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tpouade.multiteatro.model.Ticket;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.EntradaRepository;
import com.tpouade.multiteatro.repository.TicketRepository;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EntradaRepository entradaRepository;

    public TicketService(TicketRepository ticketRepository, EntradaRepository entradaRepository) {
        this.ticketRepository = ticketRepository;
        this.entradaRepository = entradaRepository;
    }

    /**
     * Persiste los tickets.
     *
     * REQUIRES_NEW es obligatorio: lo invoca un listener AFTER_COMMIT, o sea
     * cuando la transaccion de compra ya cerro y no hay ninguna activa. Sin una
     * transaccion nueva, el save falla con "No active transaction".
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Ticket> crearTickets(List<DatosTicket> datos) {
        if (datos == null || datos.isEmpty()) {
            return List.of();
        }
        List<Long> entradaIds = new ArrayList<>();
        for (DatosTicket d : datos) {
            entradaIds.add(d.entradaId());
        }
        var entradas = entradaRepository.findAllById(entradaIds);

        LocalDateTime emision = LocalDateTime.now();
        List<Ticket> tickets = new ArrayList<>();
        for (DatosTicket d : datos) {
            Ticket t = new Ticket();
            t.setCodigo(d.codigo());
            t.setFechaEmision(emision);
            entradas.stream()
                    .filter(e -> e.getId().equals(d.entradaId()))
                    .findFirst()
                    .ifPresent(t::setEntrada);
            t.setObraTitulo(d.obraTitulo());
            t.setObraClasificacion(d.obraClasificacion());
            t.setFuncionFechaHora(d.funcionFechaHora());
            t.setTeatroNombre(d.teatroNombre());
            t.setSalaNombre(d.salaNombre());
            t.setButacaFila(d.butacaFila());
            t.setButacaNumero(d.butacaNumero());
            t.setPrecioPagado(d.precioPagado());
            tickets.add(t);
        }
        return ticketRepository.saveAllAndFlush(tickets);
    }

    /** Codigo de ticket legible, unico. */
    public static String nuevoCodigo() {
        return "TCK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public List<Ticket> findByUsuario(Usuario usuario) {
        return ticketRepository.findByEntrada_Compra_Usuario_IdOrderByFechaEmisionDesc(usuario.getId());
    }
}
