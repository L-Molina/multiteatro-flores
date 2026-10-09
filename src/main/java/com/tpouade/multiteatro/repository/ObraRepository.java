package com.tpouade.multiteatro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Obra;

public interface ObraRepository extends JpaRepository<Obra, Long> {}