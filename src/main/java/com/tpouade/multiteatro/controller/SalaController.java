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

import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.model.Teatro;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.TeatroRepository;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.SalaService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/salas")
public class SalaController {

    private final SalaService salaService;
    private final TeatroRepository teatroRepository;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public SalaController(SalaService salaService, TeatroRepository teatroRepository,
                          ContextoSesion contextoSesion, AutorizacionService autorizacionService) {
        this.salaService = salaService;
        this.teatroRepository = teatroRepository;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public List<Sala> getAllSalas(HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return salaService.findAll();
    }

    @GetMapping("{id}")
    public Sala getSala(@PathVariable Long id, HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return salaService.findById(id).orElseThrow(() -> new RuntimeException("Sala no encontrada"));
    }

    @PostMapping
    public Sala createSala(@RequestBody Sala sala, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Teatro teatro = teatroRepository.findById(sala.getTeatro().getId())
                .orElseThrow(() -> new RuntimeException("Teatro no encontrado"));
        autorizacionService.requiereAdminDeTeatro(actual, teatro);
        sala.setTeatro(teatro);
        return salaService.save(sala);
    }

    @PutMapping("{id}")
    public Sala updateSala(@PathVariable Long id, @RequestBody Sala cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Sala sala = salaService.findById(id).orElseThrow(() -> new RuntimeException("Sala no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());

        sala.setNombre(cambios.getNombre());
        sala.setCapacidad(cambios.getCapacidad());
        return salaService.save(sala);
    }

    @DeleteMapping("{id}")
    public void deleteSala(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Sala sala = salaService.findById(id).orElseThrow(() -> new RuntimeException("Sala no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());
        salaService.deleteById(id);
    }
}