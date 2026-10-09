package com.tpouade.multiteatro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Sala;

public interface SalaRepository extends JpaRepository<Sala, Long> {}