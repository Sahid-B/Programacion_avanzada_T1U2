package edu.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class Aplicacion {
    public static void main(String[] args) {
        try (var context = SpringApplication.run(Aplicacion.class, args)) { }
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService executorConsultas() {
        var contador = new AtomicInteger();
        return Executors.newFixedThreadPool(2, tarea ->
                new Thread(tarea, "consulta-" + contador.incrementAndGet()));
    }
}
