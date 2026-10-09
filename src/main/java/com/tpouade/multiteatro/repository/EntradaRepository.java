package com.tpouade.multiteatro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Entrada;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    List<Entrada> findByCompra_Usuario_Id(Long usuarioId);
}