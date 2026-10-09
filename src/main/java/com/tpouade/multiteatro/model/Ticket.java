package com.tpouade.multiteatro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * Comprobante de una butaca vendida.
 *
 * Se genera uno por cada Entrada de la Compra. Guarda un SNAPSHOT de los datos
 * comerciales de la compra (obra, funcion, teatro, sala y butaca) en lugar de
 * solo referenciar a Compra, para que el comprobante no cambie si despues se
 * renombra la obra, se le cambia el precio o se cancela la funcion.
 *
 * No guarda usuario_id ni rol: la identidad del comprador es dato de la Compra,
 * no del comprobante. Eso tambien evita duplicar el dato personal del usuario.
 *
 * La FK a Entrada si se mantiene, pero con otro proposito: integridad referencial
 * para garantizar un unico ticket por butaca vendida.
 */
@Data
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @OneToOne
    @JoinColumn(name = "entrada_id", unique = true)
    @JsonIgnore
    private Entrada entrada;

    @Column(name = "obra_titulo", nullable = false, length = 150)
    private String obraTitulo;

    @Column(name = "obra_clasificacion", length = 20)
    private String obraClasificacion;

    @Column(name = "funcion_fecha_hora", nullable = false)
    private LocalDateTime funcionFechaHora;

    @Column(name = "teatro_nombre", nullable = false, length = 100)
    private String teatroNombre;

    @Column(name = "sala_nombre", nullable = false, length = 50)
    private String salaNombre;

    @Column(name = "butaca_fila", length = 10)
    private String butacaFila;

    @Column(name = "butaca_numero")
    private Integer butacaNumero;

    @Column(name = "precio_pagado", nullable = false)
    private BigDecimal precioPagado;
}
