package com.tpouade.multiteatro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Asiento;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    List<Asiento> findBySala_IdOrderByFilaAscNumeroAsc(Long salaId);
}
