package com.tpouade.multiteatro.security;

import org.springframework.stereotype.Component;

import com.tpouade.multiteatro.exception.NoAutenticadoException;
import com.tpouade.multiteatro.model.Usuario;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ContextoSesion {

    private static final String PREFIX = "Bearer ";

    private final SesionService sesionService;

    public ContextoSesion(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    public Usuario usuarioActual(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            throw new NoAutenticadoException("Falta el header Authorization o no usa el prefijo 'Bearer '");
        }
        String token = header.substring(PREFIX.length()).trim();
        Usuario usuario = sesionService.obtenerUsuarioPorToken(token);
        if (usuario == null) {
            throw new NoAutenticadoException("Token invalido o sesion expirada: volve a iniciar sesion con POST /api/auth/login");
        }
        return usuario;
    }
}
