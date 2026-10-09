package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.Teatro;
import com.tpouade.multiteatro.repository.TeatroRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TeatroService {

    private final TeatroRepository teatroRepository;

    public TeatroService(TeatroRepository teatroRepository) {
        this.teatroRepository = teatroRepository;
    }

    public List<Teatro> findAll() {
        return teatroRepository.findAll();
    }

    public Optional<Teatro> findById(Long id) {
        return teatroRepository.findById(id);
    }

    public Teatro save(Teatro teatro) {
        return teatroRepository.save(teatro);
    }

    public void deleteById(Long id) {
        teatroRepository.deleteById(id);
    }
}