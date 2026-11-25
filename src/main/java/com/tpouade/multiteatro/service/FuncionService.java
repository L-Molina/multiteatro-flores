package com.tpouade.multiteatro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tpouade.multiteatro.dto.AsientoFuncionDTO;
import com.tpouade.multiteatro.exception.RecursoNoEncontradoException;
import com.tpouade.multiteatro.model.Asiento;
import com.tpouade.multiteatro.model.Funcion;
import com.tpouade.multiteatro.repository.AsientoRepository;
import com.tpouade.multiteatro.repository.EntradaRepository;
import com.tpouade.multiteatro.repository.FuncionRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class FuncionService {

    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final EntradaRepository entradaRepository;

    public FuncionService(FuncionRepository funcionRepository, AsientoRepository asientoRepository,
                          EntradaRepository entradaRepository) {
        this.funcionRepository = funcionRepository;
        this.asientoRepository = asientoRepository;
        this.entradaRepository = entradaRepository;
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

    /**
     * Mapa de butacas de una funcion (CU-04).
     *
     * Devuelve todos los asientos de la sala de la funcion, marcando como
     * ocupados los que tienen una Entrada ACTIVA para esa funcion. Las
     * entradas CANCELADAS no cuentan: liberan la butaca.
     */
    public List<AsientoFuncionDTO> findAsientosDeFuncion(Long funcionId) {
        Funcion funcion = funcionRepository.findById(funcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada"));

        Set<Long> asientosOcupados = entradaRepository
                .findByFuncion_IdAndEstado(funcionId, "ACTIVA")
                .stream()
                .map(entrada -> entrada.getAsiento().getId())
                .collect(Collectors.toSet());

        BigDecimal precioBase = BigDecimal.valueOf(funcion.getPrecio());

        return asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(funcion.getSala().getId())
                .stream()
                .map(asiento -> toDTO(asiento, precioBase, asientosOcupados.contains(asiento.getId())))
                .toList();
    }

    private AsientoFuncionDTO toDTO(Asiento asiento, BigDecimal precioBase, boolean ocupado) {
        Double multiplicador = asiento.getTipoAsiento().getMultiplicador();
        BigDecimal precio = precioBase
                .multiply(BigDecimal.valueOf(multiplicador))
                .setScale(2, RoundingMode.HALF_UP);

        return new AsientoFuncionDTO(
                asiento.getId(),
                asiento.getFila(),
                asiento.getNumero(),
                asiento.getTipoAsiento().getId(),
                asiento.getTipoAsiento().getNombre(),
                multiplicador,
                precio,
                ocupado);
    }
}
