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
import com.tpouade.multiteatro.model.Teatro;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.UsuarioRepository;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.TeatroService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/teatros")
public class TeatroController {

    private final TeatroService teatroService;
    private final UsuarioRepository usuarioRepository;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public TeatroController(TeatroService teatroService, UsuarioRepository usuarioRepository,
                            ContextoSesion contextoSesion, AutorizacionService autorizacionService) {
        this.teatroService = teatroService;
        this.usuarioRepository = usuarioRepository;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    // Publico: el visitante puede elegir el teatro desde la cartelera.
    @GetMapping
    public List<Teatro> getAllTeatros() {
        return teatroService.findAll();
    }

    // Publico: detalle del teatro antes de iniciar sesion.
    @GetMapping("{id}")
    public Teatro getTeatro(@PathVariable Long id) {
        return teatroService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Teatro no encontrado"));
    }

    @PostMapping
    public Teatro createTeatro(@RequestBody Teatro teatro, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.SUPERADMIN);

        if (teatro.getAdmin() != null && teatro.getAdmin().getId() != null) {
            Usuario admin = usuarioRepository.findById(teatro.getAdmin().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario admin no encontrado"));
            teatro.setAdmin(admin);
        }
        return teatroService.save(teatro);
    }

    @PutMapping("{id}")
    public Teatro updateTeatro(@PathVariable Long id, @RequestBody Teatro cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Teatro teatro = teatroService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Teatro no encontrado"));
        autorizacionService.requiereAdminDeTeatro(actual, teatro);

        teatro.setNombre(cambios.getNombre());
        teatro.setDireccion(cambios.getDireccion());
        return teatroService.save(teatro);
    }

    @DeleteMapping("{id}")
    public void deleteTeatro(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.SUPERADMIN);
        if (teatroService.findById(id).isEmpty()) {
            throw new RecursoNoEncontradoException("Teatro no encontrado");
        }
        teatroService.deleteById(id);
    }
}
