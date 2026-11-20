package com.sistema.consulta.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service
public class SriService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://srienlinea.sri.gob.ec")
            .build();

    public Map<String, Object> validarYObterContribuyente(String ruc) {
        Boolean existe = restClient.get()
                .uri("/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/existePorNumeroRuc?numeroRuc={ruc}", ruc)
                .retrieve()
                .body(Boolean.class);

        if (Boolean.FALSE.equals(existe) || existe == null) {
            throw new IllegalArgumentException("El RUC no existe en el SRI.");
        }

        @SuppressWarnings("unchecked")
        List> lista = restClient.get()
                .uri("/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/obtenerPorNumerosRuc?&ruc={ruc}", ruc)
                .retrieve()
                .body(List.class);

        if (lista == null || lista.isEmpty()) {
            throw new IllegalStateException("Sin datos del contribuyente.");
        }

        Map<String, Object> datos = lista.get(0);
        String tipo = String.valueOf(datos.get("tipoContribuyente"));
        if (!"PERSONA NATURAL".equalsIgnoreCase(tipo)) {
            throw new IllegalArgumentException("Solo se admiten RUCs de Persona Natural.");
        }
        return datos;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerVehiculoPorPlaca(String placa) {
        return restClient.get()
                .uri("/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn?numeroPlacaCampvCpn={placa}", placa.trim().toUpperCase())
                .retrieve()
                .body(Map.class);
    }
}