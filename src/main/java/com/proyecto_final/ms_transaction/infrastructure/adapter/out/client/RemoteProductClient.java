package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client;

import com.proyecto_final.ms_transaction.domain.exception.ProductServiceUnavailableException;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Ejecuta consultas GET a ms-account y ms-credit traduciendo sus errores a excepciones del dominio.
 * Centraliza el manejo de errores para no repetirlo en cada adaptador (DRY).
 */
@Slf4j
@Component
public class RemoteProductClient {

    /**
     * Consulta un producto por su id.
     *
     * @param webClient         cliente del servicio remoto
     * @param uriTemplate       ruta con el marcador del id, por ejemplo /accounts/{id}
     * @param productId         identificador del producto
     * @param responseType      tipo de la respuesta
     * @param serviceName       nombre del servicio remoto, para los logs y mensajes
     * @param notFoundException fábrica de la excepción a lanzar si el servicio responde 404
     * @param <T>               tipo de la respuesta
     * @return la respuesta del servicio, o un error del dominio
     */
    public <T> Single<T> get(
            WebClient webClient,
            String uriTemplate,
            String productId,
            Class<T> responseType,
            String serviceName,
            Supplier<RuntimeException> notFoundException
    ) {
        Mono<T> response = webClient.get()
                .uri(uriTemplate, productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), clientResponse -> {
                    log.warn("{} reported that the product does not exist, id {}", serviceName, productId);
                    return Mono.error(notFoundException.get());
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    log.error("{} answered with server error {}", serviceName, clientResponse.statusCode().value());
                    return Mono.error(new ProductServiceUnavailableException(
                            serviceName, "received status " + clientResponse.statusCode().value()));
                })
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, exception -> {
                    log.error("{} is not reachable", serviceName, exception);
                    return new ProductServiceUnavailableException(serviceName, exception.getMessage());
                });

        return Single.fromPublisher(response);
    }
}