package edu.demo;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.*;

@Service
public class ConsultaService {
    private final ExecutorService executor;

    public ConsultaService(ExecutorService executor) { this.executor = executor; }

    @MedirOperacion
    public Incidencia bloqueante(String id) {
        esperar();
        return resultado(id);
    }

    @MedirOperacion
    public CompletableFuture<Incidencia> asincrona(String id) {
        return CompletableFuture.supplyAsync(() -> {
            esperar();
            return resultado(id);
        }, executor);
    }

    @MedirOperacion
    public Mono<Incidencia> noBloqueante(String id) {
        return Mono.delay(Duration.ofMillis(800)).map(tick -> resultado(id));
    }

    // Tiene anotacion, pero se excluye expresamente por el pointcut.
    @MedirOperacion
    public Incidencia excluida(String id) { return resultado(id); }

    private static Incidencia resultado(String id) {
        return new Incidencia(id, "EN_PROCESO");
    }

    private static void esperar() {
        try { Thread.sleep(800); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta interrumpida", e);
        }
    }
}
