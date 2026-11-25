package com.tpouade.multiteatro.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.exception.SolicitudInvalidaException;
import com.tpouade.multiteatro.service.OcupacionService;

/**
 * Coordinación en vivo de butacas entre sesiones.
 *
 * GET  /api/ocupacion?funcionId=1&sessionId=abc
 * POST /api/ocupacion { sessionId, funcionId, butacas, accion? }
 *      accion: "reservar" (por defecto) o "vender".
 *
 * Es público a propósito: el checkout ya exige login, pero la coordinación no
 * necesita token y así el frontend puede consultarla con el mismo cliente.
 */
@RestController
@RequestMapping("api/ocupacion")
public class OcupacionController {

    private final OcupacionService ocupacionService;

    public OcupacionController(OcupacionService ocupacionService) {
        this.ocupacionService = ocupacionService;
    }

    @GetMapping
    public Map<String, Object> getOcupacion(
            @RequestParam Long funcionId,
            @RequestParam(required = false, defaultValue = "") String sessionId) {
        return ocupacionService.snapshot(funcionId, sessionId);
    }

    @PostMapping
    public Map<String, Object> actualizarOcupacion(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("funcionId") == null) {
            throw new SolicitudInvalidaException("Falta funcionId");
        }

        Long funcionId;
        try {
            funcionId = Long.valueOf(body.get("funcionId").toString());
        } catch (NumberFormatException ex) {
            throw new SolicitudInvalidaException("funcionId inválido");
        }

        String sessionId = Objects.toString(body.get("sessionId"), "").trim();
        if (sessionId.isEmpty()) {
            throw new SolicitudInvalidaException("Falta sessionId");
        }

        List<String> butacas = new ArrayList<>();
        Object butacasRaw = body.get("butacas");
        if (butacasRaw instanceof List<?> lista) {
            for (Object item : lista) {
                if (item != null) butacas.add(item.toString());
            }
        }

        String accion = Objects.toString(body.get("accion"), "reservar");
        return ocupacionService.actualizar(funcionId, sessionId, butacas, accion);
    }
}
