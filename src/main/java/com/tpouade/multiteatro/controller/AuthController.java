package com.tpouade.multiteatro.controller;

import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.exception.CredencialesInvalidasException;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.UsuarioRepository;
import com.tpouade.multiteatro.security.SesionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private static final String PREFIX = "Bearer ";

    private final UsuarioRepository usuarioRepository;
    private final SesionService sesionService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, SesionService sesionService,
                          BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.sesionService = sesionService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Usuario usuario = usuarioRepository.findByEmail(body.get("email"))
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales inválidas"));

        if (!passwordEncoder.matches(body.get("password"), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }

        String token = sesionService.crearSesion(usuario);
        return Map.of("token", token, "usuario", usuario);
    }

    @PostMapping("register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        if (body.get("name") == null || body.get("email") == null || body.get("password") == null) {
            throw new RuntimeException("Faltan datos: name, email, password");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(body.get("name"));
        usuario.setEmail(body.get("email"));
        usuario.setPasswordHash(passwordEncoder.encode(body.get("password")));
        usuario.setRol(Usuario.Rol.CLIENTE);

        Usuario guardado = usuarioRepository.save(usuario);
        String token = sesionService.crearSesion(guardado);
        return Map.of("token", token, "usuario", guardado);
    }

    @PostMapping("logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            sesionService.cerrarSesion(header.substring(PREFIX.length()).trim());
        }
        return Map.of("mensaje", "Sesión cerrada");
    }
}
