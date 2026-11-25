package com.tpouade.multiteatro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Entrada;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    List<Entrada> findByCompra_Usuario_Id(Long usuarioId);

    /**
     * Entradas de una funcion que todavia ocupan butaca. Se usa para armar
     * el mapa de asientos: una entrada CANCELADA libera el asiento.
     */
    List<Entrada> findByFuncion_IdAndEstado(Long funcionId, String estado);
}
