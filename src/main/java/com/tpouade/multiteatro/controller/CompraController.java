package com.tpouade.multiteatro.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tpouade.multiteatro.dto.CompraDTO;
import com.tpouade.multiteatro.dto.CompraRequest;
import com.tpouade.multiteatro.model.Compra;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.security.ContextoSesion;
import com.tpouade.multiteatro.service.CompraService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/compras")
public class CompraController {

    private final CompraService compraService;
    private final ContextoSesion contextoSesion;

    public CompraController(CompraService compraService, ContextoSesion contextoSesion) {
        this.compraService = compraService;
        this.contextoSesion = contextoSesion;
    }

    @GetMapping
    public List<CompraDTO> getAllCompras(HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        List<Compra> compras = actual.getRol() == Usuario.Rol.SUPERADMIN
                ? compraService.findAll()
                : compraService.findByUsuario(actual);
        return compras.stream()
                .map(compra -> CompraDTO.from(compra, debeEnmascararDni(actual, compra)))
                .toList();
    }

    @PostMapping
    public Compra comprarEntradas(@RequestBody CompraRequest body, HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        return compraService.comprarEntradas(actual, body);
    }

    @PostMapping("{id}/comprobante")
    public CompraDTO subirComprobante(@PathVariable Long id,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        Compra compra = compraService.guardarComprobante(actual, id, archivo);
        return CompraDTO.from(compra, false);
    }

    @GetMapping("{id}/comprobante")
    public ResponseEntity<Resource> descargarComprobante(@PathVariable Long id,
            HttpServletRequest request) {
        Usuario actual = contextoSesion.usuarioActual(request);
        CompraService.ComprobanteDescarga archivo = compraService.obtenerComprobante(actual, id);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(archivo.nombre(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(archivo.recurso());
    }

    /**
     * El dueño ve su DNI completo. Un administrador que consulta compras de
     * terceros lo ve enmascarado (****1234), aunque no pueda abrir el detalle
     * de esa compra desde la UI.
     */
    private static boolean debeEnmascararDni(Usuario actual, Compra compra) {
        if (actual.getRol() == Usuario.Rol.CLIENTE) {
            return false;
        }
        return compra.getUsuario() == null
                || !compra.getUsuario().getId().equals(actual.getId());
    }
}
