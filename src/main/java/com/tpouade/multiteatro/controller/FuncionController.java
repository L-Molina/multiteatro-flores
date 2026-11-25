package com.tpouade.multiteatro.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tpouade.multiteatro.dto.AsientoFuncionDTO;
import com.tpouade.multiteatro.exception.RecursoNoEncontradoException;
import com.tpouade.multiteatro.model.Funcion;
import com.tpouade.multiteatro.model.Obra;
import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.ObraRepository;
import com.tpouade.multiteatro.repository.SalaRepository;
import com.tpouade.multiteatro.security.AutorizacionService;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.FuncionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/funciones")
public class FuncionController {

    private final FuncionService funcionService;
    private final SalaRepository salaRepository;
    private final ObraRepository obraRepository;
    private final ContextoSesion contextoSesion;
    private final AutorizacionService autorizacionService;

    public FuncionController(FuncionService funcionService, SalaRepository salaRepository,
                             ObraRepository obraRepository, ContextoSesion contextoSesion,
                             AutorizacionService autorizacionService) {
        this.funcionService = funcionService;
        this.salaRepository = salaRepository;
        this.obraRepository = obraRepository;
        this.contextoSesion = contextoSesion;
        this.autorizacionService = autorizacionService;
    }

    // Publico: un visitante puede ver la cartelera antes de registrarse.
    @GetMapping
    public List<Funcion> getAllFunciones() {
        return funcionService.findAll();
    }

    // Publico: detalle de una funcion para decidir si comprar.
    @GetMapping("{id}")
    public Funcion getFuncion(@PathVariable Long id) {
        return funcionService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada"));
    }

    /**
     * CU-04: mapa de butacas de la funcion. Requiere login porque a partir de
     * aca el visitante ya elige sala, tipo de asiento y ubicacion.
     */
    @GetMapping("{id}/asientos")
    public List<AsientoFuncionDTO> getAsientosDeFuncion(@PathVariable Long id, HttpServletRequest request) {
        contextoSesion.usuarioActual(request);
        return funcionService.findAsientosDeFuncion(id);
    }

    @PostMapping
    public Funcion createFuncion(@RequestBody Funcion funcion, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Sala sala = salaRepository.findById(funcion.getSala().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sala no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());

        Obra obra = obraRepository.findById(funcion.getObra().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        funcion.setSala(sala);
        funcion.setObra(obra);

        // Si no se informa precio, la funcion hereda el precio base de la obra.
        if (funcion.getPrecio() == null) {
            funcion.setPrecio(obra.getPrecio().doubleValue());
        }
        return funcionService.save(funcion);
    }

    @PutMapping("{id}")
    public Funcion updateFuncion(@PathVariable Long id, @RequestBody Funcion cambios, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Funcion funcion = funcionService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, funcion.getSala().getTeatro());

        funcion.setFechaHora(cambios.getFechaHora());
        funcion.setPrecio(cambios.getPrecio());
        if (cambios.getSala() != null && cambios.getSala().getId() != null) {
            Sala sala = salaRepository.findById(cambios.getSala().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sala no encontrada"));
            autorizacionService.requiereAdminDeTeatro(actual, sala.getTeatro());
            funcion.setSala(sala);
        }
        if (cambios.getObra() != null && cambios.getObra().getId() != null) {
            Obra obra = obraRepository.findById(cambios.getObra().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
            funcion.setObra(obra);
        }
        return funcionService.save(funcion);
    }

    @DeleteMapping("{id}")
    public void deleteFuncion(@PathVariable Long id, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Funcion funcion = funcionService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada"));
        autorizacionService.requiereAdminDeTeatro(actual, funcion.getSala().getTeatro());
        funcionService.deleteById(id);
    }
}
