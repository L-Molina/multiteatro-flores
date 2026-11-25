package com.tpouade.multiteatro.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "obra")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 20)
    private String clasificacion;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private Integer duracionMinutos;

    /**
     * Las tres categorias comerciales que comparten la tabla obra.
     * El precio base de cada obra sale de aca: teatral $30.000,
     * stand-up $20.000 y concierto $12.500 (ver script.sql).
     */
    public enum Tipo {
        TEATRAL, STAND_UP, CONCIERTO
    }
}
