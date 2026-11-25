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

import com.tpouade.multiteatro.exception.RecursoNoEncontradoException;
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

    // Publico: la cartelera se puede ver sin iniciar sesion.
    @GetMapping
    public List<Obra> getAllObras() {
        return obraService.findAll();
    }

    // Publico: detalle de la obra para la portada / cartelera.
    @GetMapping("{id}")
    public Obra getObra(@PathVariable Long id) {
        return obraService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
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
        Obra obra = obraService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));

        obra.setTitulo(cambios.getTitulo());
        obra.setTipo(cambios.getTipo());
        obra.setPrecio(cambios.getPrecio());
        obra.setClasificacion(cambios.getClasificacion());
        obra.setDescripcion(cambios.getDescripcion());
        obra.setDuracionMinutos(cambios.getDuracionMinutos());
        return obraService.save(obra);
    }

    @DeleteMapping("{id}")
    public void deleteObra(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.ADMIN);
        if (obraService.findById(id).isEmpty()) {
            throw new RecursoNoEncontradoException("Obra no encontrada");
        }
        obraService.deleteById(id);
    }
}
