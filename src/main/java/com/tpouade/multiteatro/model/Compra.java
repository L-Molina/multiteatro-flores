package com.tpouade.multiteatro.model;

import java.math.BigDecimal;
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
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "monto_total", nullable = false)
    private Double montoTotal;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private BigDecimal total;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}