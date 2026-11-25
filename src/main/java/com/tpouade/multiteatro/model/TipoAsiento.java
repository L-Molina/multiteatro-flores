package com.tpouade.multiteatro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tipo_asiento")
public class TipoAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    /**
     * Factor que se aplica al precio base de la funcion.
     * Normal = 1.0, VIP = 1.3 (30% mas). El precio final NO vive aca:
     * vive en la obra/funcion, porque una misma butaca puede usarse
     * en funciones de distinto precio.
     */
    @Column(nullable = false)
    private Double multiplicador;
}
