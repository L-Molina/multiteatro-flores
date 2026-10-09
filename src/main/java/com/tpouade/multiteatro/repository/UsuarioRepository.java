package com.tpouade.multiteatro.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tpouade.multiteatro.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}