package com.tpouade.multiteatro.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.tpouade.multiteatro.model.Usuario;

/**
 * Sesiones en memoria. Al bootear la app el mapa arranca vacío.
 * El token es un UUID que se devuelve en el login y se manda luego
 * en cada request como header: Authorization: Bearer <token>
 */
@Component
public class SesionService {

    private final Map<String, Usuario> sesiones = new ConcurrentHashMap<>();

    public String crearSesion(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, usuario);
        return token;
    }

    public Usuario obtenerUsuarioPorToken(String token) {
        return token == null ? null : sesiones.get(token);
    }

    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}
