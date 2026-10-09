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

import com.tpouade.multiteatro.model.Funcion;
import com.tpouade.multiteatro.model.Obra;
import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.ObraRepository;
import com.tpouade.multiteatro.repository.SalaRepository;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.FuncionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/funciones")
public class FuncionController {

    private final FuncionService funcionService;
    private final SalaRepository salaRepository;
    private final ObraRepository obraRepository;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public FuncionController(FuncionService funcionService, SalaRepository salaRepository,
                             ObraRepository obraRepository, ContextoSesion contextoSesion,
                             AutorizacionService autorizacionService) {
        this.funcionService = funcionService;
        this.salaRepository = salaRepository;
        this.obraRepository = obraRepository;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public List<Funcion> getAllFunciones(HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return funcionService.findAll();
    }

    @GetMapping("{id}")
    public Funcion getFuncion(@PathVariable Long id, HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return funcionService.findById(id).orElseThrow(() -> new RuntimeException("Función no encontrada"));
    }

    @PostMapping
    public Funcion createFuncion(@RequestBody Funcion funcion, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Sala sala = salaRepository.findById(funcion.getSala().getId())
                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());

        Obra obra = obraRepository.findById(funcion.getObra().getId())
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));
        funcion.setSala(sala);
        funcion.setObra(obra);
        return funcionService.save(funcion);
    }

    @PutMapping("{id}")
    public Funcion updateFuncion(@PathVariable Long id, @RequestBody Funcion cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Funcion funcion = funcionService.findById(id).orElseThrow(() -> new RuntimeException("Función no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, funcion.getSala().getTeatro());

        funcion.setFechaHora(cambios.getFechaHora());
        funcion.setPrecio(cambios.getPrecio());
        if (cambios.getSala() != null && cambios.getSala().getId() != null) {
            Sala sala = salaRepository.findById(cambios.getSala().getId())
                    .orElseThrow(() -> new RuntimeException("Sala no encontrada"));
            autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());
            funcion.setSala(sala);
        }
        return funcionService.save(funcion);
    }

    @DeleteMapping("{id}")
    public void deleteFuncion(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Funcion funcion = funcionService.findById(id).orElseThrow(() -> new RuntimeException("Función no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, funcion.getSala().getTeatro());
        funcionService.deleteById(id);
    }
}