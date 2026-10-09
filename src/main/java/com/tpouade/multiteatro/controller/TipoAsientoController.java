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

import com.tpouade.multiteatro.model.TipoAsiento;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.TipoAsientoService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/tipos-asiento")
public class TipoAsientoController {

    private final TipoAsientoService tipoAsientoService;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public TipoAsientoController(TipoAsientoService tipoAsientoService, ContextoSesion contextoSesion,
                                 AutorizacionService autorizacionService) {
        this.tipoAsientoService = tipoAsientoService;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public List<TipoAsiento> getAllTiposAsiento(HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return tipoAsientoService.findAll();
    }

    @GetMapping("{id}")
    public TipoAsiento getTipoAsiento(@PathVariable Long id, HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return tipoAsientoService.findById(id).orElseThrow(() -> new RuntimeException("Tipo de asiento no encontrado"));
    }

    @PostMapping
    public TipoAsiento createTipoAsiento(@RequestBody TipoAsiento tipoAsiento, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        return tipoAsientoService.save(tipoAsiento);
    }

    @PutMapping("{id}")
    public TipoAsiento updateTipoAsiento(@PathVariable Long id, @RequestBody TipoAsiento cambios,
                                         HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        TipoAsiento tipoAsiento = tipoAsientoService.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de asiento no encontrado"));
        tipoAsiento.setNombre(cambios.getNombre());
        tipoAsiento.setPrecio(cambios.getPrecio());
        return tipoAsientoService.save(tipoAsiento);
    }

    @DeleteMapping("{id}")
    public void deleteTipoAsiento(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        tipoAsientoService.deleteById(id);
    }
}