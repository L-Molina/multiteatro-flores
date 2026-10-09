package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Entrada;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.EntradaRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class EntradaService {

    private final EntradaRepository entradaRepository;

    public EntradaService(EntradaRepository entradaRepository) {
        this.entradaRepository = entradaRepository;
    }

    public List<Entrada> findAll() {
        return entradaRepository.findAll();
    }

    public List<Entrada> findByUsuario(Usuario usuario) {
        return entradaRepository.findByCompra_Usuario_Id(usuario.getId());
    }

    public Optional<Entrada> findById(Long id) {
        return entradaRepository.findById(id);
    }

    public Entrada save(Entrada entrada) {
        return entradaRepository.save(entrada);
    }
}