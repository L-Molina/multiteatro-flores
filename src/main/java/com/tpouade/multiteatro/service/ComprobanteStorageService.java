package com.tpouade.multiteatro.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tpouade.multiteatro.exception.RecursoNoEncontradoException;
import com.tpouade.multiteatro.exception.SolicitudInvalidaException;

/**
 * Guarda los comprobantes de transferencia en disco.
 *
 * El archivo nunca se expone directamente: se guarda fuera de los recursos
 * estáticos y se sirve por un endpoint autenticado. El nombre físico es un
 * UUID, así que adivinarlo no alcanza para descargarlo.
 */
@Service
public class ComprobanteStorageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final Set<String> TIPOS = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf", "application/octet-stream");
    private static final Map<String, String> TIPO_POR_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "pdf", "application/pdf");
    private static final Pattern NOMBRE_FISICO = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|jpeg|png|webp|pdf)$");

    private final Path baseDir;

    public ComprobanteStorageService(
            @Value("${app.uploads.comprobantes-dir:uploads/comprobantes}") String directorio) {
        this.baseDir = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public ArchivoGuardado guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new SolicitudInvalidaException("Adjuntá el comprobante de la transferencia.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new SolicitudInvalidaException("El comprobante no puede superar los 2 MB.");
        }

        String tipo = archivo.getContentType() == null
                ? null
                : archivo.getContentType().toLowerCase(Locale.ROOT).trim();
        if (tipo != null && !tipo.isEmpty() && !TIPOS.contains(tipo)) {
            throw new SolicitudInvalidaException("El comprobante debe ser una imagen o un PDF.");
        }

        String nombreOriginal = limpiarNombre(archivo.getOriginalFilename());
        String extension = extension(nombreOriginal, tipo);
        String nombreFisico = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(baseDir);
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, baseDir.resolve(nombreFisico), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No pudimos guardar el comprobante.", ex);
        }

        return new ArchivoGuardado(nombreOriginal, nombreFisico);
    }

    public Resource cargar(String nombreFisico) {
        if (nombreFisico == null || !NOMBRE_FISICO.matcher(nombreFisico).matches()) {
            throw new RecursoNoEncontradoException("El comprobante no está disponible.");
        }
        Path archivo = baseDir.resolve(nombreFisico).normalize();
        if (!archivo.startsWith(baseDir) || !Files.isRegularFile(archivo)) {
            throw new RecursoNoEncontradoException("El comprobante no está disponible.");
        }
        try {
            return new UrlResource(archivo.toUri());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("No pudimos leer el comprobante.", ex);
        }
    }

    public String tipoContenido(String nombreFisico) {
        if (nombreFisico == null) {
            return "application/octet-stream";
        }
        int punto = nombreFisico.lastIndexOf('.');
        if (punto < 0) {
            return "application/octet-stream";
        }
        String extension = nombreFisico.substring(punto + 1).toLowerCase(Locale.ROOT);
        return TIPO_POR_EXTENSION.getOrDefault(extension, "application/octet-stream");
    }

    /** Borra el archivo anterior cuando el usuario reemplaza el comprobante. */
    public void eliminar(String nombreFisico) {
        if (nombreFisico == null || !NOMBRE_FISICO.matcher(nombreFisico).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(baseDir.resolve(nombreFisico).normalize());
        } catch (IOException ignored) {
            // El archivo anterior es basura recuperable: no bloquea la compra.
        }
    }

    private static String limpiarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.replace('\\', '/');
        int barra = limpio.lastIndexOf('/');
        if (barra >= 0) {
            limpio = limpio.substring(barra + 1);
        }
        limpio = limpio.replaceAll("[\\r\\n\\t]", " ").trim();
        if (limpio.isEmpty()) {
            limpio = "comprobante";
        }
        return limpio.length() > 255 ? limpio.substring(limpio.length() - 255) : limpio;
    }

    private static String extension(String nombreOriginal, String tipo) {
        int punto = nombreOriginal.lastIndexOf('.');
        String extension = punto >= 0 && punto < nombreOriginal.length() - 1
                ? nombreOriginal.substring(punto + 1).toLowerCase(Locale.ROOT)
                : "";
        if (EXTENSIONES.contains(extension)) {
            return extension;
        }
        return switch (tipo == null ? "" : tipo) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            default -> throw new SolicitudInvalidaException(
                    "El comprobante debe ser una imagen JPG, PNG, WEBP o un PDF.");
        };
    }

    public record ArchivoGuardado(String nombreOriginal, String nombreFisico) {
    }
}
