package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.repository.SalaRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class SalaService {

    private final SalaRepository salaRepository;

    public SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    public List<Sala> findAll() {
        return salaRepository.findAll();
    }

    public Optional<Sala> findById(Long id) {
        return salaRepository.findById(id);
    }

    public Sala save(Sala sala) {
        return salaRepository.save(sala);
    }

    public void deleteById(Long id) {
        salaRepository.deleteById(id);
    }
}