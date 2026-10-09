package com.tpouade.multiteatro.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.model.Asiento;
import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.model.TipoAsiento;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.SalaRepository;
import com.tpouade.multiteatro.repository.TipoAsientoRepository;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.AsientoService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/asientos")
public class AsientoController {

    private final AsientoService asientoService;
    private final SalaRepository salaRepository;
    private final TipoAsientoRepository tipoAsientoRepository;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public AsientoController(AsientoService asientoService, SalaRepository salaRepository,
                             TipoAsientoRepository tipoAsientoRepository, ContextoSesion contextoSesion,
                             AutorizacionService autorizacionService) {
        this.asientoService = asientoService;
        this.salaRepository = salaRepository;
        this.tipoAsientoRepository = tipoAsientoRepository;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public List<Asiento> getAllAsientos(HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return asientoService.findAll();
    }

    @GetMapping("{id}")
    public Asiento getAsiento(@PathVariable Long id, HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return asientoService.findById(id).orElseThrow(() -> new RuntimeException("Asiento no encontrado"));
    }

    @PostMapping
    public Asiento createAsiento(@RequestBody Asiento asiento, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Sala sala = salaRepository.findById(asiento.getSala().getId())
                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());

        TipoAsiento tipoAsiento = tipoAsientoRepository.findById(asiento.getTipoAsiento().getId())
                .orElseThrow(() -> new RuntimeException("Tipo de asiento no encontrado"));
        asiento.setSala(sala);
        asiento.setTipoAsiento(tipoAsiento);
        return asientoService.save(asiento);
    }

    @PutMapping("{id}")
    public Asiento updateAsiento(@PathVariable Long id, @RequestBody Asiento cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Asiento asiento = asientoService.findById(id).orElseThrow(() -> new RuntimeException("Asiento no encontrado"));
        autorizacionService.requiereAdminDeTeatro(actual, asiento.getSala().getTeatro());

        asiento.setFila(cambios.getFila());
        asiento.setNumero(cambios.getNumero());
        if (cambios.getTipoAsiento() != null && cambios.getTipoAsiento().getId() != null) {
            TipoAsiento tipoAsiento = tipoAsientoRepository.findById(cambios.getTipoAsiento().getId())
                    .orElseThrow(() -> new RuntimeException("Tipo de asiento no encontrado"));
            asiento.setTipoAsiento(tipoAsiento);
        }
        return asientoService.save(asiento);
    }

    @DeleteMapping("{id}")
    public void deleteAsiento(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Asiento asiento = asientoService.findById(id).orElseThrow(() -> new RuntimeException("Asiento no encontrado"));
        autorizacionService.requiereAdminDeTeatro(actual, asiento.getSala().getTeatro());
        asientoService.deleteById(id);
    }
}