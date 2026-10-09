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

import com.tpouade.multiteatro.model.Obra;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.ObraService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/obras")
public class ObraController {

    private final ObraService obraService;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public ObraController(ObraService obraService, ContextoSesion contextoSesion,
                          AutorizacionService autorizacionService) {
        this.obraService = obraService;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public List<Obra> getAllObras(HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return obraService.findAll();
    }

    @GetMapping("{id}")
    public Obra getObra(@PathVariable Long id, HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return obraService.findById(id).orElseThrow(() -> new RuntimeException("Obra no encontrada"));
    }

    @PostMapping
    public Obra createObra(@RequestBody Obra obra, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        return obraService.save(obra);
    }

    @PutMapping("{id}")
    public Obra updateObra(@PathVariable Long id, @RequestBody Obra cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        Obra obra = obraService.findById(id).orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        obra.setTitulo(cambios.getTitulo());
        obra.setClasificacion(cambios.getClasificacion());
        obra.setDescripcion(cambios.getDescripcion());
        obra.setDuracionMinutos(cambios.getDuracionMinutos());
        return obraService.save(obra);
    }

    @DeleteMapping("{id}")
    public void deleteObra(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        obraService.deleteById(id);
    }
}