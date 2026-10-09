package com.tpouade.multiteatro.model;

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
@Table(name = "asiento")
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String fila;

    @Column(nullable = false)
    private Integer numero;

    @ManyToOne
    @JoinColumn(name = "sala_id")
    private Sala sala;

    @ManyToOne
    @JoinColumn(name = "tipo_asiento_id")
    private TipoAsiento tipoAsiento;
}