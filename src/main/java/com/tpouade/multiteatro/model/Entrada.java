package com.tpouade.multiteatro.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "entrada")
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(name = "precio_pagado", nullable = false)
    private BigDecimal precioPagado;

    @Column(nullable = false, length = 20)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne
    @JoinColumn(name = "funcion_id")
    private Funcion funcion;

    @ManyToOne
    @JoinColumn(name = "asiento_id")
    private Asiento asiento;
}