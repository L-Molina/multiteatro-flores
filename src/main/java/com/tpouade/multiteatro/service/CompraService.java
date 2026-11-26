package com.tpouade.multiteatro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tpouade.multiteatro.dto.CompraRequest;
import com.tpouade.multiteatro.exception.AccesoDenegadoException;
import com.tpouade.multiteatro.exception.AsientoNoDisponibleException;
import com.tpouade.multiteatro.exception.RecursoNoEncontradoException;
import com.tpouade.multiteatro.exception.SolicitudInvalidaException;
import com.tpouade.multiteatro.model.Asiento;
import com.tpouade.multiteatro.model.Compra;
import com.tpouade.multiteatro.model.Entrada;
import com.tpouade.multiteatro.model.Funcion;
import com.tpouade.multiteatro.model.Obra;
import com.tpouade.multiteatro.model.Sala;
import com.tpouade.multiteatro.model.Teatro;
import com.tpouade.multiteatro.model.Usuario;
import com.tpouade.multiteatro.repository.AsientoRepository;
import com.tpouade.multiteatro.repository.CompraRepository;
import com.tpouade.multiteatro.repository.EntradaRepository;
import com.tpouade.multiteatro.repository.FuncionRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CompraService {

    private final CompraRepository compraRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final EntradaRepository entradaRepository;
    private final ApplicationEventPublisher eventoPublisher;
    private final ComprobanteStorageService comprobanteStorage;

    public CompraService(CompraRepository compraRepository, FuncionRepository funcionRepository,
            AsientoRepository asientoRepository, EntradaRepository entradaRepository,
            ApplicationEventPublisher eventoPublisher,
            ComprobanteStorageService comprobanteStorage) {
        this.compraRepository = compraRepository;
        this.funcionRepository = funcionRepository;
        this.asientoRepository = asientoRepository;
        this.entradaRepository = entradaRepository;
        this.eventoPublisher = eventoPublisher;
        this.comprobanteStorage = comprobanteStorage;
    }

    public List<Compra> findAll() {
        return compraRepository.findAll();
    }

    public List<Compra> findByUsuario(Usuario usuario) {
        return compraRepository.findByUsuario_Id(usuario.getId());
    }

    public Compra findById(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada"));
    }

    /**
     * Adjunta (o reemplaza) el comprobante de transferencia. Solo el titular de
     * la compra puede subirlo: un administrador puede consultarlo, pero no
     * modificar la operación de otra persona.
     */
    public Compra guardarComprobante(Usuario actual, Long compraId, MultipartFile archivo) {
        Compra compra = findById(compraId);
        if (!esPropietario(actual, compra)) {
            throw new AccesoDenegadoException(
                    "Solo el titular de la compra puede adjuntar el comprobante.");
        }

        ComprobanteStorageService.ArchivoGuardado guardado = comprobanteStorage.guardar(archivo);
        comprobanteStorage.eliminar(compra.getComprobanteArchivo());
        compra.setComprobanteArchivo(guardado.nombreFisico());
        compra.setComprobanteNombre(guardado.nombreOriginal());
        compra.setComprobanteUrl("/compras/" + compra.getId() + "/comprobante");
        return compraRepository.saveAndFlush(compra);
    }

    /**
     * Devuelve el archivo para el dueño de la compra o para un SUPERADMIN.
     * El DNI puede ir enmascarado en las vistas administrativas, pero el
     * comprobante se sirve únicamente por este endpoint autenticado.
     */
    public ComprobanteDescarga obtenerComprobante(Usuario actual, Long compraId) {
        Compra compra = findById(compraId);
        boolean esSuperadmin = actual.getRol() == Usuario.Rol.SUPERADMIN;
        if (!esSuperadmin && !esPropietario(actual, compra)) {
            throw new AccesoDenegadoException("No tenés acceso al comprobante de esta compra.");
        }
        if (compra.getComprobanteArchivo() == null) {
            throw new RecursoNoEncontradoException("La compra no tiene un comprobante adjunto.");
        }

        Resource recurso = comprobanteStorage.cargar(compra.getComprobanteArchivo());
        String nombre = compra.getComprobanteNombre() != null
                ? compra.getComprobanteNombre()
                : "comprobante";
        return new ComprobanteDescarga(
                recurso,
                nombre,
                comprobanteStorage.tipoContenido(compra.getComprobanteArchivo()));
    }

    private static boolean esPropietario(Usuario actual, Compra compra) {
        return actual != null
                && compra.getUsuario() != null
                && compra.getUsuario().getId().equals(actual.getId());
    }

    public record ComprobanteDescarga(Resource recurso, String nombre, String tipoContenido) {
    }

    public Compra comprarEntradas(Usuario usuario, CompraRequest request) {
        Funcion funcion = funcionRepository.findById(request.funcionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada"));

        List<Long> asientoIds = request.asientoIds();
        if (asientoIds == null || asientoIds.isEmpty()) {
            throw new SolicitudInvalidaException("Tenés que elegir al menos un asiento");
        }
        List<Long> asientoIdsUnicos = asientoIds.stream().distinct().toList();
        if (asientoIdsUnicos.size() != asientoIds.size()) {
            throw new SolicitudInvalidaException("No se puede comprar la misma butaca dos veces");
        }

        List<Asiento> asientos = asientoRepository.findAllById(asientoIdsUnicos);
        if (asientos.size() != asientoIdsUnicos.size()) {
            throw new RecursoNoEncontradoException("Alguno de los asientos no existe");
        }
        for (Asiento asiento : asientos) {
            if (!asiento.getSala().getId().equals(funcion.getSala().getId())) {
                throw new SolicitudInvalidaException(
                        "Todos los asientos deben pertenecer a la sala de la función");
            }
        }

        Compra compra = new Compra();
        compra.setUsuario(usuario);
        aplicarTitular(compra, usuario, request.titular());
        aplicarPago(compra, request.pago());
        compra.setEstado("CONFIRMADA");
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(BigDecimal.ZERO);
        compra.setMontoTotal(0D);
        compra = compraRepository.save(compra);

        BigDecimal total = BigDecimal.ZERO;
        List<Entrada> entradas = new ArrayList<>();
        for (Asiento asiento : asientos) {
            Entrada entrada = new Entrada();
            entrada.setCodigo(UUID.randomUUID().toString().substring(0, 12));
            entrada.setCompra(compra);
            entrada.setFuncion(funcion);
            entrada.setAsiento(asiento);
            // Precio base de la función (heredado de la obra) por el factor
            // del tipo de butaca: Normal x1.0, VIP x1.3.
            BigDecimal precioUnitario = BigDecimal.valueOf(funcion.getPrecio())
                    .multiply(BigDecimal.valueOf(asiento.getTipoAsiento().getMultiplicador()))
                    .setScale(2, RoundingMode.HALF_UP);
            entrada.setPrecioPagado(precioUnitario);
            entrada.setEstado("ACTIVA");
            entradas.add(entrada);
            total = total.add(entrada.getPrecioPagado());
        }

        try {
            entradaRepository.saveAllAndFlush(entradas);
        } catch (DataIntegrityViolationException e) {
            throw new AsientoNoDisponibleException(
                    "Uno de los asientos elegidos ya no está disponible para esta función");
        }

        compra.setTotal(total);
        compra.setMontoTotal(total.doubleValue());
        Compra confirmada = compraRepository.saveAndFlush(compra);

        publicarConfirmacion(confirmada, usuario, funcion, total, entradas);
        return confirmada;
    }

    /**
     * Arma los datos planos de un ticket por cada butaca comprada y publica el
     * evento. Se hace aca, dentro de la transaccion, porque es el unico momento
     * en que las entidades estan mapeadas y se pueden leer las relaciones
     * (funcion -> obra, sala -> teatro, asiento -> tipo).
     */
    private void publicarConfirmacion(Compra compra, Usuario usuario, Funcion funcion,
            BigDecimal total, List<Entrada> entradas) {
        List<DatosTicket> datos = new ArrayList<>();
        for (Entrada entrada : entradas) {
            Asiento asiento = entrada.getAsiento();
            Sala sala = asiento.getSala();
            Teatro teatro = sala.getTeatro();
            Obra obra = funcion.getObra();
            datos.add(new DatosTicket(
                    entrada.getId(),
                    TicketService.nuevoCodigo(),
                    obra.getTitulo(),
                    obra.getClasificacion(),
                    funcion.getFechaHora(),
                    teatro.getNombre(),
                    sala.getNombre(),
                    asiento.getFila(),
                    asiento.getNumero(),
                    entrada.getPrecioPagado()));
        }

        String email = compra.getTitularEmail() != null
                ? compra.getTitularEmail()
                : usuario.getEmail();
        String nombre = compra.getTitularNombre() != null
                ? compra.getTitularNombre()
                : usuario.getNombre();

        eventoPublisher.publishEvent(new CompraConfirmadaEvent(
                compra.getId(),
                email,
                nombre,
                total,
                compra.getFecha(),
                datos));
    }

    private void aplicarTitular(Compra compra, Usuario usuario,
            CompraRequest.TitularRequest titular) {
        String nombre = limpiar(titular != null ? titular.nombre() : null);
        if (nombre == null) {
            nombre = usuario.getNombre();
        }
        if (nombre == null || nombre.length() > 150) {
            throw new SolicitudInvalidaException("El nombre del titular es obligatorio.");
        }

        String email = normalizarEmail(titular != null ? titular.email() : null);
        if (email == null) {
            email = usuario.getEmail();
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new SolicitudInvalidaException("El correo del titular no es válido.");
        }

        String dni = soloDigitos(titular != null ? titular.dni() : null);
        if (dni != null && !dni.matches("\\d{7,8}")) {
            throw new SolicitudInvalidaException("El DNI debe tener 7 u 8 dígitos.");
        }

        String telefono = limpiar(titular != null ? titular.telefono() : null);
        if (telefono != null) {
            String digitos = soloDigitos(telefono);
            if (digitos.length() < 8 || digitos.length() > 15) {
                throw new SolicitudInvalidaException("El teléfono debe tener entre 8 y 15 dígitos.");
            }
        }

        String localidad = limpiar(titular != null ? titular.localidad() : null);

        compra.setTitularNombre(nombre);
        compra.setTitularDni(dni);
        compra.setTitularEmail(email);
        compra.setTitularTelefono(telefono);
        compra.setTitularLocalidad(localidad);
    }

    private void aplicarPago(Compra compra, CompraRequest.PagoRequest pago) {
        String metodo = limpiar(pago != null ? pago.metodo() : null);
        if (metodo == null) {
            metodo = "tarjeta";
        }
        if (!Set.of("tarjeta", "mercadopago", "transferencia").contains(metodo)) {
            throw new SolicitudInvalidaException("Método de pago inválido.");
        }

        compra.setMetodoPago(metodo);
        compra.setPagoLabel(switch (metodo) {
            case "mercadopago" ->
                "Mercado Pago";
            case "transferencia" ->
                "Transferencia bancaria";
            default ->
                "Tarjeta de crédito (3 cuotas)";
        });

        if ("transferencia".equals(metodo)) {
            String nombre = limpiar(pago != null ? pago.comprobanteNombre() : null);
            if (nombre == null) {
                throw new SolicitudInvalidaException(
                        "Adjuntá el comprobante de la transferencia para confirmar la compra.");
            }
            if (nombre.length() > 255) {
                throw new SolicitudInvalidaException("El nombre del comprobante es demasiado largo.");
            }
            compra.setComprobanteNombre(nombre);
        }
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("[\\r\\n\\t]", " ").trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static String soloDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    private static String normalizarEmail(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toLowerCase(java.util.Locale.ROOT);
    }

    public Compra save(Compra compra) {
        return compraRepository.save(compra);
    }
}
