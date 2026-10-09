package com.tpouade.multiteatro.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.exception.AccesoDenegadoException;
import com.tpouade.multiteatro.model.Entrada;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.EntradaService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/entradas")
public class EntradaController {

    private final EntradaService entradaService;
    private final ContextoSesion contextoSesion;

    public EntradaController(EntradaService entradaService, ContextoSesion contextoSesion) {
        this.entradaService = entradaService;
        this.contextoSesion = contextoSesion;
    }

    @GetMapping
    public List<Entrada> getAllEntradas(HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        if (actual.getRol() == Usuario.Rol.SUPERADMIN) {
            return entradaService.findAll();
        }
        return entradaService.findByUsuario(actual);
    }

    // Cancela una entrada: solo el Cliente dueño de la compra, o SuperAdmin
    @DeleteMapping("{id}")
    public void cancelarEntrada(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Entrada entrada = entradaService.findById(id).orElseThrow(() -> new RuntimeException("Entrada no encontrada"));

        boolean esSuperadmin = actual.getRol() == Usuario.Rol.SUPERADMIN;
        boolean esPropietario = entrada.getCompra() != null
                && entrada.getCompra().getUsuario() != null
                && entrada.getCompra().getUsuario().getId().equals(actual.getId());
        if (!esSuperadmin && !esPropietario) {
            throw new AccesoDenegadoException(
                    "Solo el dueño de la compra o el SuperAdmin puede cancelar la entrada");
        }

        entrada.setEstado("CANCELADA");
        entradaService.save(entrada);
    }
}