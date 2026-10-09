package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Funcion;
import com.tpouade.multiteatro.repository.FuncionRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class FuncionService {

    private final FuncionRepository funcionRepository;

    public FuncionService(FuncionRepository funcionRepository) {
        this.funcionRepository = funcionRepository;
    }

    public List<Funcion> findAll() {
        return funcionRepository.findAll();
    }

    public Optional<Funcion> findById(Long id) {
        return funcionRepository.findById(id);
    }

    public Funcion save(Funcion funcion) {
        return funcionRepository.save(funcion);
    }

    public void deleteById(Long id) {
        funcionRepository.deleteById(id);
    }
}