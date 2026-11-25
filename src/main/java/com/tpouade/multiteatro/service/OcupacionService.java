package com.tpouade.multiteatro.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Registro en memoria de butacas reservadas o vendidas por sesión.
 *
 * Es el equivalente backend de /api/ocupacion del dev server de Vite: permite
 * que dos pestañas o cuentas distintas vean en tiempo real qué butacas está
 * eligiendo la otra. Las reservas vencen solas si la pestaña deja de renovarlas.
 *
 * No reemplaza la verdad de la base: al confirmar la compra, POST /api/compras
 * sigue validando contra la tabla entrada. Este registro es solo una capa de
 * coordinación temporal para la UI.
 */
@Service
public class OcupacionService {

    private static final long TTL_MS = 20_000;

    private record Reserva(Long funcionId, String sessionId, Set<String> butacas, long expira) {
    }

    private final Map<String, Reserva> reservas = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> ventas = new ConcurrentHashMap<>();

    /** Normaliza igual que el frontend: "B-1", "b 1" y "B1" son la misma butaca. */
    private static String normalizar(String valor) {
        if (valor == null) return "";
        return valor.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private static String clave(Long funcionId, String sessionId) {
        return funcionId + "|" + sessionId;
    }

    private synchronized void limpiar() {
        long ahora = System.currentTimeMillis();
        reservas.entrySet().removeIf(entry -> entry.getValue().expira() < ahora);
    }

    public synchronized Map<String, Object> snapshot(Long funcionId, String sessionId) {
        limpiar();

        List<String> ventasFuncion = new ArrayList<>(ventas.getOrDefault(funcionId, Set.of()));
        List<Map<String, Object>> reservasFuncion = new ArrayList<>();
        for (Reserva reserva : reservas.values()) {
            if (!reserva.funcionId().equals(funcionId)) continue;
            if (reserva.sessionId().equals(sessionId)) continue;
            reservasFuncion.add(Map.of(
                    "sessionId", reserva.sessionId(),
                    "butacas", new ArrayList<>(reserva.butacas())));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("ventas", ventasFuncion);
        resultado.put("reservas", reservasFuncion);
        return resultado;
    }

    public synchronized Map<String, Object> actualizar(Long funcionId, String sessionId,
            List<String> butacas, String accion) {
        limpiar();

        List<String> pedidas = butacas == null
                ? List.of()
                : butacas.stream()
                        .map(OcupacionService::normalizar)
                        .filter(valor -> !valor.isBlank())
                        .distinct()
                        .toList();

        Set<String> ocupadasPorOtros = new HashSet<>(ventas.getOrDefault(funcionId, Set.of()));
        for (Reserva reserva : reservas.values()) {
            if (!reserva.funcionId().equals(funcionId)) continue;
            if (reserva.sessionId().equals(sessionId)) continue;
            ocupadasPorOtros.addAll(reserva.butacas());
        }

        List<String> rechazadas = pedidas.stream()
                .filter(ocupadasPorOtros::contains)
                .toList();
        List<String> aceptadas = pedidas.stream()
                .filter(butaca -> !ocupadasPorOtros.contains(butaca))
                .toList();

        if ("vender".equalsIgnoreCase(accion)) {
            Set<String> vendidas = ventas.computeIfAbsent(
                    funcionId, id -> ConcurrentHashMap.newKeySet());
            vendidas.addAll(aceptadas);
            reservas.remove(clave(funcionId, sessionId));
        } else {
            String key = clave(funcionId, sessionId);
            if (aceptadas.isEmpty()) {
                reservas.remove(key);
            } else {
                reservas.put(key, new Reserva(
                        funcionId,
                        sessionId,
                        new LinkedHashSet<>(aceptadas),
                        System.currentTimeMillis() + TTL_MS));
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>(snapshot(funcionId, sessionId));
        resultado.put("rechazadas", rechazadas);
        return resultado;
    }
}
