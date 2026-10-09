package com.tpouade.multiteatro.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.model.Compra;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.CompraService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/compras")
public class CompraController {

    private final CompraService compraService;
    private final ContextoSesion contextoSesion;

    public CompraController(CompraService compraService, ContextoSesion contextoSesion) {
        this.compraService = compraService;
        this.contextoSesion = contextoSesion;
    }

    @GetMapping
    public List<Compra> getAllCompras(HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        if (actual.getRol() == Usuario.Rol.SUPERADMIN) {
            return compraService.findAll();
        }
        return compraService.findByUsuario(actual);
    }

    @PostMapping
    public Compra comprarEntradas(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Long funcionId = Long.valueOf(body.get("funcionId").toString());
        List<Long> asientoIds = ((List<?>) body.get("asientoIds")).stream()
                .map(o -> Long.valueOf(o.toString()))
                .toList();
        return compraService.comprarEntradas(actual, funcionId, asientoIds);
    }
}