package com.tpouade.multiteatro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Teatro;

public interface TeatroRepository extends JpaRepository<Teatro, Long> {}