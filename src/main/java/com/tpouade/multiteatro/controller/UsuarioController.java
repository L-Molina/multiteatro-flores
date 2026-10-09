package com.tpouade.multiteatro.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.exception.AccesoDenegadoException;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public UsuarioController(UsuarioService usuarioService, BCryptPasswordEncoder passwordEncoder,
                             ContextoSesion contextoSesion, AutorizacionService autorizacionService) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public List<Usuario> getAllUsuarios(HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.SUPERADMIN);
        return usuarioService.findAll();
    }

    // CLIENTE se autorregistra; alta de ADMIN/SUPERADMIN exige token de SuperAdmin
    @PostMapping
    public Usuario createUsuario(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String passwordRaw = body.get("password");
        if (passwordRaw == null || passwordRaw.isEmpty()) {
            throw new RuntimeException("Falta password");
        }

        Usuario.Rol rol = body.get("rol") == null ? Usuario.Rol.CLIENTE : Usuario.Rol.valueOf(body.get("rol"));
        if (rol != Usuario.Rol.CLIENTE) {
            Usuario actual = contextoSesion.usuarioActual(request);
            autorizacionService.requiereRol(actual, Usuario.Rol.SUPERADMIN);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(body.get("name"));
        usuario.setEmail(body.get("email"));
        usuario.setRol(rol);
        usuario.setPasswordHash(passwordEncoder.encode(passwordRaw));
        return usuarioService.save(usuario);
    }

    @PutMapping("{id}")
    public Usuario updateUsuario(@PathVariable Long id, @RequestBody Usuario cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Usuario usuario = usuarioService.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean esSuperadmin = actual.getRol() == Usuario.Rol.SUPERADMIN;
        boolean esSelf = actual.getId().equals(usuario.getId());
        if (!esSuperadmin && !esSelf) {
            throw new AccesoDenegadoException(
                    "Solo el propio usuario o el SuperAdmin puede editar este perfil");
        }

        usuario.setNombre(cambios.getNombre());
        usuario.setEmail(cambios.getEmail());
        if (cambios.getPasswordHash() != null && !cambios.getPasswordHash().isEmpty()) {
            usuario.setPasswordHash(passwordEncoder.encode(cambios.getPasswordHash()));
        }
        return usuarioService.save(usuario);
    }

    @DeleteMapping("{id}")
    public void deleteUsuario(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        autorizacionService.requiereRol(actual, Usuario.Rol.SUPERADMIN);
        usuarioService.deleteById(id);
    }
}