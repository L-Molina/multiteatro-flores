package com.tpouade.multiteatro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCodigo(String codigo);

    Optional<Ticket> findByEntrada_Id(Long entradaId);

    /**
     * El ticket NO tiene usuario_id, asi que para traer los de un cliente hay
     * que cruzar entrada -> compra -> usuario. Esa es la consecuencia concreta
     * de no guardar el id del comprador en el comprobante.
     */
    List<Ticket> findByEntrada_Compra_Usuario_IdOrderByFechaEmisionDesc(Long usuarioId);
}
