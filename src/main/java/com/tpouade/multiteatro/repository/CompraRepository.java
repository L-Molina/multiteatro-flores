package com.tpouade.multiteatro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findByUsuario_Id(Long usuarioId);
}