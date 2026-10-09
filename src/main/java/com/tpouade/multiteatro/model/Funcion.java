package com.tpouade.multiteatro.model;

import java.time.LocalDateTime;

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
@Table(name = "funcion")
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Double precio;

    @ManyToOne
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne
    @JoinColumn(name = "sala_id")
    private Sala sala;
}