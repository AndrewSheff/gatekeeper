package com.ashevtsov.gatekeeper.gateway;

import com.ashevtsov.gatekeeper.gateway.filter.GatewayContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;

/**
 * Сервис проксирования — берет контекст, собирает целевой URL,
 * копирует хедеры и тело, стреляет запрос через RestClient и возвращает ответ.
 * Тут без магии — честный HTTP-прокси.
 */
@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    /** Хедеры которые не надо пробрасывать — они либо hop-by-hop, либо RestClient сам разберется */
    private static final Set<String> SKIP_HEADERS = Set.of(
            "host", "connection", "content-length", "transfer-encoding",
            "keep-alive", "upgrade", "proxy-connection"
    );

    /** HTTP-методы с телом запроса */
    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");

    private final RestClient restClient;

    public ProxyService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Проксируем запрос на target-сервис.
     * Собираем URL (target + путь без prefix-сегментов), копируем хедеры, тело — и вперед.
     */
    public ResponseEntity<byte[]> forward(GatewayContext context) {
        HttpServletRequest request = context.getRequest();
        GatewayRoute route = context.getRoute();

        String targetUrl = buildTargetUrl(request, route);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        log.debug("Проксируем {} {} -> {}", method, request.getRequestURI(), targetUrl);

        var spec = restClient.method(method)
                .uri(targetUrl);

        // копируем хедеры от клиента
        copyRequestHeaders(request, spec);

        // копируем тело для POST/PUT/PATCH
        if (METHODS_WITH_BODY.contains(request.getMethod())) {
            try {
                byte[] body = request.getInputStream().readAllBytes();
                if (body.length > 0) {
                    spec.body(body);
                }
            } catch (IOException e) {
                log.error("Не смогли прочитать тело запроса: {}", e.getMessage());
                throw new RuntimeException("Ошибка чтения тела запроса", e);
            }
        }

        // стреляем и получаем ответ
        return spec.exchange((req, res) -> {
            HttpHeaders responseHeaders = new HttpHeaders();
            res.getHeaders().forEach((headerName, headerValues) -> {
                // не пробрасываем hop-by-hop хедеры обратно
                if (!SKIP_HEADERS.contains(headerName.toLowerCase())) {
                    responseHeaders.addAll(headerName, headerValues);
                }
            });

            byte[] responseBody = res.getBody().readAllBytes();

            return ResponseEntity
                    .status(res.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);
        });
    }

    /**
     * Собираем целевой URL: targetUrl + остаток пути после strip-а prefix-сегментов.
     * Например: путь=/users-service/api/users, stripPrefix=1 -> targetUrl + /api/users
     */
    private String buildTargetUrl(HttpServletRequest request, GatewayRoute route) {
        String path = request.getRequestURI();
        String strippedPath = stripPrefixSegments(path, route.getStripPrefix());

        // убираем trailing slash у targetUrl и добавляем strippedPath
        String baseUrl = route.getTargetUrl().replaceAll("/+$", "");
        String targetPath = strippedPath.isEmpty() ? "" : strippedPath;

        // если есть query string — приклеиваем
        String queryString = request.getQueryString();
        String fullUrl = baseUrl + targetPath;
        if (queryString != null && !queryString.isEmpty()) {
            fullUrl += "?" + queryString;
        }

        return fullUrl;
    }

    /**
     * Отрезаем первые N сегментов пути.
     * /a/b/c с stripPrefix=1 -> /b/c
     * /a/b/c с stripPrefix=2 -> /c
     */
    private String stripPrefixSegments(String path, int stripPrefix) {
        if (stripPrefix <= 0) {
            return path;
        }

        String[] segments = path.split("/");
        // segments[0] пустая строка (путь начинается с /)
        if (stripPrefix >= segments.length - 1) {
            return "";
        }

        String[] remaining = Arrays.copyOfRange(segments, stripPrefix + 1, segments.length);
        return "/" + String.join("/", remaining);
    }

    /**
     * Копируем хедеры из оригинального запроса, пропуская hop-by-hop всякое
     */
    private void copyRequestHeaders(HttpServletRequest request, RestClient.RequestBodySpec spec) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (SKIP_HEADERS.contains(headerName.toLowerCase())) {
                continue;
            }

            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                spec.header(headerName, headerValues.nextElement());
            }
        }
    }
}
