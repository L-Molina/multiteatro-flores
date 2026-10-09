package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Asiento;
import com.tpouade.multiteatro.repository.AsientoRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class AsientoService {

    private final AsientoRepository asientoRepository;

    public AsientoService(AsientoRepository asientoRepository) {
        this.asientoRepository = asientoRepository;
    }

    public List<Asiento> findAll() {
        return asientoRepository.findAll();
    }

    public Optional<Asiento> findById(Long id) {
        return asientoRepository.findById(id);
    }

    public Asiento save(Asiento asiento) {
        return asientoRepository.save(asiento);
    }

    public void deleteById(Long id) {
        asientoRepository.deleteById(id);
    }
}