package com.tpouade.multiteatro.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.service.AvisoService;

/**
 * Novedades de cartelera para la campana del header.
 *
 * GET  /api/notificaciones -> { notificaciones: [...] }
 * POST /api/notificaciones { obraId, obraTitulo, obraImg?, genero?, fecha?, hora?, sala? }
 *
 * La lectura es pública para que cualquier visitante vea la bandeja. La
 * escritura la usa el panel de gestión al programar una función.
 */
@RestController
@RequestMapping("api/notificaciones")
public class AvisoController {

    private final AvisoService avisoService;

    public AvisoController(AvisoService avisoService) {
        this.avisoService = avisoService;
    }

    @GetMapping
    public Map<String, Object> listar() {
        return Map.of("notificaciones", avisoService.listar());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Map<String, Object> body) {
        Map<String, Object> aviso = avisoService.crear(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("notificacion", aviso));
    }
}
