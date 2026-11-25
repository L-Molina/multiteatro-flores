package com.tpouade.multiteatro.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.exception.SolicitudInvalidaException;

/**
 * Bandeja de novedades en memoria (campana del header).
 *
 * Es el equivalente backend de /api/notificaciones del dev server de Vite.
 * El gestor publica un aviso cuando programa la primera función de una obra;
 * las demás sesiones lo reciben por polling. La bandeja arranca vacía y se
 * reinicia cuando se reinicia la aplicación.
 *
 * No confundir con NotificacionService, que envía el mail de confirmación de
 * una compra.
 */
@Service
public class AvisoService {

    private static final int MAX_AVISOS = 50;

    private final List<Map<String, Object>> avisos = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Map<String, Object>> listar() {
        return new ArrayList<>(avisos);
    }

    public Map<String, Object> crear(Map<String, Object> body) {
        if (body == null || body.get("obraId") == null) {
            throw new SolicitudInvalidaException("Falta obraId");
        }

        Long obraId;
        try {
            obraId = Long.valueOf(body.get("obraId").toString());
        } catch (NumberFormatException ex) {
            throw new SolicitudInvalidaException("obraId inválido");
        }

        String titulo = texto(body.get("obraTitulo"));
        if (titulo.isEmpty()) {
            throw new SolicitudInvalidaException("Falta obraTitulo");
        }

        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("id", nextId.getAndIncrement());
        aviso.put("creada", Instant.now().toString());
        aviso.put("tipo", "funcion");
        aviso.put("obraId", obraId);
        aviso.put("obraTitulo", titulo);
        aviso.put("obraImg", texto(body.get("obraImg")));
        aviso.put("genero", texto(body.get("genero")));
        aviso.put("fecha", texto(body.get("fecha")));
        aviso.put("hora", texto(body.get("hora")));
        aviso.put("sala", texto(body.get("sala")));

        avisos.add(0, aviso);
        while (avisos.size() > MAX_AVISOS) {
            avisos.remove(avisos.size() - 1);
        }
        return aviso;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }
}
