package com.tpouade.multiteatro.service;

/**
 * Datos planos (sin entidades JPA) de un ticket a crear.
 *
 * Se genera DENTRO de la transaccion de compra, donde las entidades todavia
 * estan mapeadas, y viaja al listener que corre DESPUES del commit. Al cruzar
 * ese limite las entidades quedan desligadas de la sesion, por eso el evento
 * lleva datos planos y no objetos @Entity.
 */
public record DatosTicket(
        Long entradaId,
        String codigo,
        String obraTitulo,
        String obraClasificacion,
        java.time.LocalDateTime funcionFechaHora,
        String teatroNombre,
        String salaNombre,
        String butacaFila,
        Integer butacaNumero,
        java.math.BigDecimal precioPagado) {
}
