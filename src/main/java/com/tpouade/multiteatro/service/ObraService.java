package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Obra;
import com.tpouade.multiteatro.repository.ObraRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ObraService {

    private final ObraRepository obraRepository;

    public ObraService(ObraRepository obraRepository) {
        this.obraRepository = obraRepository;
    }

    public List<Obra> findAll() {
        return obraRepository.findAll();
    }

    public Optional<Obra> findById(Long id) {
        return obraRepository.findById(id);
    }

    public Obra save(Obra obra) {
        return obraRepository.save(obra);
    }

    public void deleteById(Long id) {
        obraRepository.deleteById(id);
    }
}