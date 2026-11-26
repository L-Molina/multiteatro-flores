package com.tpouade.multiteatro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @Column(name = "titular_nombre", length = 150)
    private String titularNombre;

    /**
     * El DNI no se serializa desde la entidad: GET /api/compras devuelve un
     * CompraDTO que lo incluye completo para el dueño y enmascarado para las
     * vistas administrativas. Así tampoco se filtra por la compra anidada en
     * GET /api/entradas.
     */
    @JsonIgnore
    @Column(name = "titular_dni", length = 20)
    private String titularDni;

    @Column(name = "titular_email", length = 150)
    private String titularEmail;

    @Column(name = "titular_telefono", length = 30)
    private String titularTelefono;

    @Column(name = "titular_localidad", length = 100)
    private String titularLocalidad;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(name = "pago_label", length = 60)
    private String pagoLabel;

    /** Nombre original del archivo, para mostrarlo en la UI. */
    @Column(name = "comprobante_nombre", length = 255)
    private String comprobanteNombre;

    /** Ruta relativa a la API para descargar el comprobante. */
    @Column(name = "comprobante_url", length = 255)
    private String comprobanteUrl;

    /** Nombre físico en disco (UUID + extensión). No se expone por JSON. */
    @JsonIgnore
    @Column(name = "comprobante_archivo", length = 255)
    private String comprobanteArchivo;
}
