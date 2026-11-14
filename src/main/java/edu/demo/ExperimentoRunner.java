package edu.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class ExperimentoRunner implements CommandLineRunner {
    private final ConsultaService servicio;
    private final MedicionAspect aspecto;
    public ExperimentoRunner(ConsultaService servicio, MedicionAspect aspecto) {
        this.servicio = servicio; this.aspecto = aspecto;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("JAVA=" + System.getProperty("java.version")
                + " | demora=800ms | executor=2 hilos | IDs ficticios INC-001 INC-002");
        int antes = aspecto.llamadas();
        verificar(servicio.excluida("INC-001"), "INC-001");
        if (aspecto.llamadas() != antes) throw new AssertionError("Exclusion incorrecta");
        System.out.println("POINTCUT excluida: 0 intercepciones (verificado)");
        var filas = new ArrayList<String>();
        filas.add("variante,medicion,retorno1_ms,retorno2_ms,retorno_par_ms,total_ms");
        for (String variante : List.of("BLOQUEANTE", "FUTURE", "MONO")) {
            medir(variante, 0); // calentamiento completo, no se incluye en la tabla
            for (int i = 1; i <= 3; i++) filas.add(medir(variante, i));
        }
        if (aspecto.llamadas() != antes + 24) throw new AssertionError("Se esperaban 24 llamadas AOP");
        Files.createDirectories(Path.of("evidencias"));
        Files.write(Path.of("evidencias/mediciones.csv"), filas);
        System.out.println("VALIDACION OK: 24 consultas equivalentes; exclusion OK; 9 mediciones y 3 calentamientos");
    }

    private String medir(String variante, int numero) {
        System.out.printf("%nINICIO %s %s hilo=%s%n", variante,
                numero == 0 ? "CALENTAMIENTO" : "MEDICION " + numero, Thread.currentThread().getName());
        long inicio = System.nanoTime();
        long t1, t2, retorno;
        Incidencia a, b;
        switch (variante) {
            case "BLOQUEANTE" -> {
                a = servicio.bloqueante("INC-001"); t1 = System.nanoTime();
                b = servicio.bloqueante("INC-002"); t2 = System.nanoTime(); retorno = t2;
            }
            case "FUTURE" -> {
                var f1 = servicio.asincrona("INC-001"); t1 = System.nanoTime();
                var f2 = servicio.asincrona("INC-002"); t2 = System.nanoTime(); retorno = t2;
                CompletableFuture.allOf(f1, f2).join(); a = f1.join(); b = f2.join();
            }
            case "MONO" -> {
                var m1 = servicio.noBloqueante("INC-001"); t1 = System.nanoTime();
                var m2 = servicio.noBloqueante("INC-002"); t2 = System.nanoTime(); retorno = t2;
                // zip suscribe ambos; block solo mantiene vivo el runner de consola.
                var par = Objects.requireNonNull(Mono.zip(m1, m2).block());
                a = par.getT1(); b = par.getT2();
            }
            default -> throw new IllegalArgumentException(variante);
        }
        long fin = System.nanoTime();
        verificar(a, "INC-001"); verificar(b, "INC-002");
        double r1 = ms(t1 - inicio), r2 = ms(t2 - t1), rp = ms(retorno - inicio), total = ms(fin - inicio);
        System.out.printf(Locale.ROOT,
                "RESUMEN %s n=%d retorno1=%.3f retorno2=%.3f retornoPar=%.3f total=%.3f ms%n",
                variante, numero, r1, r2, rp, total);
        System.out.println("RESULTADOS " + a + " | " + b);
        return String.format(Locale.ROOT, "%s,%d,%.3f,%.3f,%.3f,%.3f", variante, numero, r1, r2, rp, total);
    }

    private static double ms(long nanos) { return nanos / 1_000_000.0; }
    private static void verificar(Incidencia valor, String id) {
        if (!new Incidencia(id, "EN_PROCESO").equals(valor))
            throw new AssertionError("Resultado diferente: " + valor);
    }
}
