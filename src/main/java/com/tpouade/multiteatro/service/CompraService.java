package com.tpouade.multiteatro.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.exception.AsientoNoDisponibleException;
import com.tpouade.multiteatro.model.Asiento;
import com.tpouade.multiteatro.model.Compra;
import com.tpouade.multiteatro.model.Entrada;
import com.tpouade.multiteatro.model.Funcion;
import com.tpouade.multiteatro.model.Obra;
import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.model.Teatro;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.AsientoRepository;
import com.tpouade.multiteatro.repository.CompraRepository;
import com.tpouade.multiteatro.repository.EntradaRepository;
import com.tpouade.multiteatro.repository.FuncionRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CompraService {

    private final CompraRepository compraRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final EntradaRepository entradaRepository;
    private final ApplicationEventPublisher eventoPublisher;

    public CompraService(CompraRepository compraRepository, FuncionRepository funcionRepository,
                         AsientoRepository asientoRepository, EntradaRepository entradaRepository,
                         ApplicationEventPublisher eventoPublisher) {
        this.compraRepository = compraRepository;
        this.funcionRepository = funcionRepository;
        this.asientoRepository = asientoRepository;
        this.entradaRepository = entradaRepository;
        this.eventoPublisher = eventoPublisher;
    }

    public List<Compra> findAll() {
        return compraRepository.findAll();
    }

    public List<Compra> findByUsuario(Usuario usuario) {
        return compraRepository.findByUsuario_Id(usuario.getId());
    }

    public Compra comprarEntradas(Usuario usuario, Long funcionId, List<Long> asientoIds) {
        Funcion funcion = funcionRepository.findById(funcionId)
                .orElseThrow(() -> new RuntimeException("Función no encontrada"));
        List<Asiento> asientos = asientoRepository.findAllById(asientoIds);

        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setEstado("CONFIRMADA");
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(BigDecimal.ZERO);
        compra.setMontoTotal(0D);
        compra = compraRepository.save(compra);

        BigDecimal total = BigDecimal.ZERO;
        List<Entrada> entradas = new ArrayList<>();
        for (Asiento asiento : asientos) {
            Entrada entrada = new Entrada();
            entrada.setCodigo(UUID.randomUUID().toString().substring(0, 12));
            entrada.setCompra(compra);
            entrada.setFuncion(funcion);
            entrada.setAsiento(asiento);
            entrada.setPrecioPagado(BigDecimal.valueOf(asiento.getTipoAsiento().getPrecio()));
            entrada.setEstado("ACTIVA");
            entradas.add(entrada);
            total = total.add(entrada.getPrecioPagado());
        }

        try {
            entradaRepository.saveAllAndFlush(entradas);
        } catch (DataIntegrityViolationException e) {
            throw new AsientoNoDisponibleException(
                    "Uno de los asientos elegidos ya no está disponible para esta función");
        }

        compra.setTotal(total);
        compra.setMontoTotal(total.doubleValue());
        Compra confirmada = compraRepository.saveAndFlush(compra);

        publicarConfirmacion(confirmada, usuario, funcion, total, entradas);
        return confirmada;
    }

    /**
     * Arma los datos planos de un ticket por cada butaca comprada y publica el
     * evento. Se hace aca, dentro de la transaccion, porque es el unico momento
     * en que las entidades estan mapeadas y se pueden leer las relaciones
     * (funcion -> obra, sala -> teatro, asiento -> tipo).
     */
    private void publicarConfirmacion(Compra compra, Usuario usuario, Funcion funcion,
                                     BigDecimal total, List<Entrada> entradas) {
        List<DatosTicket> datos = new ArrayList<>();
        for (Entrada entrada : entradas) {
            Asiento asiento = entrada.getAsiento();
            Sala sala = asiento.getSala();
            Teatro teatro = sala.getTeatro();
            Obra obra = funcion.getObra();
            datos.add(new DatosTicket(
                    entrada.getId(),
                    TicketService.nuevoCodigo(),
                    obra.getTitulo(),
                    obra.getClasificacion(),
                    funcion.getFechaHora(),
                    teatro.getNombre(),
                    sala.getNombre(),
                    asiento.getFila(),
                    asiento.getNumero(),
                    entrada.getPrecioPagado()));
        }
        eventoPublisher.publishEvent(new CompraConfirmadaEvent(
                compra.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                total,
                compra.getFecha(),
                datos));
    }

    public Compra save(Compra compra) {
        return compraRepository.save(compra);
    }
}