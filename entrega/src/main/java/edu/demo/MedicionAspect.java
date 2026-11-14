package edu.demo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class MedicionAspect {
    private final AtomicInteger llamadas = new AtomicInteger();
    public int llamadas() { return llamadas.get(); }

    @Around("@annotation(edu.demo.MedirOperacion) && !execution(* edu.demo.ConsultaService.excluida(..))")
    public Object medir(ProceedingJoinPoint punto) throws Throwable {
        llamadas.incrementAndGet();
        String metodo = punto.getSignature().getName();
        String id = String.valueOf(punto.getArgs()[0]);
        long inicio = System.nanoTime();
        evento("ENTRADA", metodo, id, inicio);
        Object resultado;
        try { resultado = punto.proceed(); }
        catch (Throwable e) { evento("ERROR", metodo, id, inicio); throw e; }
        evento("RETORNO", metodo, id, inicio);
        if (resultado instanceof CompletableFuture<?> futuro) {
            // Retornar la etapa observada garantiza que join espera tambien el registro.
            return futuro.whenComplete((valor, error) ->
                    evento(error == null ? "FIN_FUTURE" : "ERROR_FUTURE", metodo, id, inicio));
        }
        if (resultado instanceof Mono<?> mono) {
            // Mono es frio: medir desde la suscripcion, por cada suscriptor.
            return Mono.defer(() -> {
                long suscripcion = System.nanoTime();
                evento("SUSCRIPCION", metodo, id, suscripcion);
                return mono.doOnSuccess(valor -> evento("FIN_MONO", metodo, id, suscripcion))
                        .doOnError(error -> evento("ERROR_MONO", metodo, id, suscripcion));
            });
        }
        return resultado;
    }

    private static void evento(String evento, String metodo, String id, long inicio) {
        System.out.printf(java.util.Locale.ROOT,
                "AOP %-12s metodo=%s id=%s hilo=%s ms=%.3f%n",
                evento, metodo, id, Thread.currentThread().getName(),
                (System.nanoTime() - inicio) / 1_000_000.0);
    }
}
