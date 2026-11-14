# Consulta sintética de incidencias

Java 21, Spring Boot 3.3.4, Spring AOP y Reactor. Solo datos ficticios.

## Compilar y ejecutar

Con JDK 21 y Maven 3.9 o superior en PATH:

```text
mvn clean package
java -jar target/incidencias-1.0.0.jar
```

El JAR incluye sus dependencias. También se entrega en `bin/incidencias-1.0.0.jar`.
Para guardar una nueva traza en PowerShell:

```powershell
java -jar bin/incidencias-1.0.0.jar *> evidencias/ejecucion.log
```

Ejecutar desde la raíz del proyecto. Dura aproximadamente 15 segundos más el arranque y termina automáticamente. El CSV se regenera en `evidencias/mediciones.csv`; las capturas y el informe corresponden a la ejecución entregada, no se regeneran automáticamente.

## IntelliJ

Abrir `pom.xml` como proyecto Maven, elegir JDK 21 como Project SDK y Maven Runner JRE, recargar Maven y ejecutar `edu.demo.Aplicacion`. No hace falta servidor web ni base de datos.

## Protocolo y comprobaciones

El runner es un bean distinto de ConsultaService. Invoca el proxy AOP; no usa autoinvocación. Cada variante tiene un calentamiento con dos consultas y tres rondas medidas con los mismos IDs INC-001 e INC-002. Bloqueante invoca secuencialmente; Future lanza ambas tareas antes de esperar; Mono.zip suscribe ambos publishers antes de block.

El runner valida el resultado de cada consulta y que haya 24 intercepciones. Una llamada anotada a `excluida` no atraviesa el advice por la negación explícita del pointcut; se comprueba el contador antes y después. Cualquier fallo produce una excepción. `VALIDACION OK` confirma estas comprobaciones en la ejecución.

El CSV contiene duración de cada invocación desde el runner, tiempo de retorno del par y tiempo total hasta ambos valores, en milisegundos con System.nanoTime. Las líneas AOP RETORNO miden solo proceed; FIN_FUTURE parte de la entrada al método y FIN_MONO de cada suscripción. Los puntos de medida son distintos. Los registros añaden sobrecarga. El bloqueante mantiene main esperando durante sleep; Future bloquea consulta-1 y consulta-2 y main espera en join; Mono no mantiene un worker dormido durante delay, aunque main espera en block como frontera de consola.

## Evidencias

`compilacion.log`, `ejecucion.log`, `mediciones.csv` y capturas reales del código y de las trazas vistas en IntelliJ. No son prueba de carga. No se deben extrapolar estos resultados a producción. Bitácora de IA en archivo TXT independiente.
