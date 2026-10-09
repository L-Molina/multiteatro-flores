package com.tpouade.multiteatro.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Ticket;

/**
 * Envia el mail de confirmacion de compra.
 *
 * Si no hay SMTP configurado (mail.enabled=false), NO intenta enviar: imprime el
 * mail en el log. Asi el flujo se puede demostrar sin credenciales, y fallar el
 * envio nunca rompe la compra.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final String remitente;
    private final boolean habilitado;

    public NotificacionService(JavaMailSender mailSender,
                               @Value("${mail.from}") String remitente,
                               @Value("${mail.enabled:false}") boolean habilitado) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.habilitado = habilitado;
    }

    public void enviarConfirmacion(CompraConfirmadaEvent evento) {
        String asunto = "Confirmacion de compra #" + evento.compraId();
        String cuerpo = construirCuerpo(evento);
        if (!habilitado) {
            log.info("""

                    ===== MAIL (modo consola: mail.enabled=false) =====
                    Para:      {}
                    Asunto:    {}
                    {}
                    =================================================
                    """, evento.emailDestinatario(), asunto, cuerpo);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(evento.emailDestinatario());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            log.info("Mail de confirmacion enviado a {} (compra {})",
                    evento.emailDestinatario(), evento.compraId());
        } catch (MailException e) {
            // La compra ya esta confirmada: un mail fallido no la debe revertir.
            log.error("No se pudo enviar el mail de la compra {} a {}: {}",
                    evento.compraId(), evento.emailDestinatario(), e.getMessage());
        }
    }

    private String construirCuerpo(CompraConfirmadaEvent evento) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola ").append(evento.nombreDestinatario()).append(",\n\n");
        sb.append("Tu compra se realizo con exito. A continuacion el detalle de tus tickets:\n\n");
        for (DatosTicket d : evento.tickets()) {
            sb.append("  - Codigo: ").append(d.codigo()).append('\n');
            sb.append("    Obra: ").append(d.obraTitulo())
              .append(d.obraClasificacion() != null ? " (" + d.obraClasificacion() + ")" : "")
              .append('\n');
            sb.append("    Funcion: ").append(d.funcionFechaHora().format(FORMATO_FECHA)).append('\n');
            sb.append("    Teatro: ").append(d.teatroNombre())
              .append(" - Sala ").append(d.salaNombre()).append('\n');
            sb.append("    Butaca: ").append(d.butacaFila()).append(d.butacaNumero()).append('\n');
            sb.append("    Precio: $").append(d.precioPagado()).append("\n\n");
        }
        sb.append("Total pagado: $").append(evento.montoTotal()).append('\n');
        sb.append("Fecha de compra: ").append(evento.fecha().format(FORMATO_FECHA)).append('\n');
        sb.append("\nGracias por tu compra.\n");
        return sb.toString();
    }
}
