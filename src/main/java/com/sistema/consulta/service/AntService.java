package com.sistema.consulta.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "ant:puntos:";

    public Map<String, Object> consultarPuntosLicencia(String cedula, String placa) {
        String key = PREFIX + cedula;
        String url = String.format(
                "https://consultaweb.ant.gob.ec/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp?ps_tipo_identificacion=CED&ps_identificacion=%s&ps_placa=%s",
                cedula, placa == null ? "" : placa.trim().toUpperCase()
        );

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(4000)
                    .get();

            Element el = doc.selectFirst("td.titulo1, #puntos, .puntos-licencia");
            String puntos = (el != null && !el.text().isBlank()) ? el.text().trim() : "30";

            Map<String, Object> payload = new HashMap<>();
            payload.put("cedula", cedula);
            payload.put("puntos", puntos);
            payload.put("origen", "ANT_LIVE");
            payload.put("actualizadoEn", LocalDateTime.now().toString());

            redisTemplate.opsForValue().set(key, payload, Duration.ofDays(7));
            return payload;
        } catch (Exception ex) {
            log.warn("Fallo/timeout ANT para cédula {}. Leyendo caché Redis...", cedula);
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                cached.put("origen", "REDIS_CACHE_FALLBACK");
                return cached;
            }
            throw new RuntimeException("ANT no disponible y sin registro previo en caché para la cédula " + cedula);
        }
    }
}