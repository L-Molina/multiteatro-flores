package com.tpouade.multiteatro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.model.TipoAsiento;
import com.tpouade.multiteatro.repository.TipoAsientoRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TipoAsientoService {

    private final TipoAsientoRepository tipoAsientoRepository;

    public TipoAsientoService(TipoAsientoRepository tipoAsientoRepository) {
        this.tipoAsientoRepository = tipoAsientoRepository;
    }

    public List<TipoAsiento> findAll() {
        return tipoAsientoRepository.findAll();
    }

    public Optional<TipoAsiento> findById(Long id) {
        return tipoAsientoRepository.findById(id);
    }

    public TipoAsiento save(TipoAsiento tipoAsiento) {
        return tipoAsientoRepository.save(tipoAsiento);
    }

    public void deleteById(Long id) {
        tipoAsientoRepository.deleteById(id);
    }
}