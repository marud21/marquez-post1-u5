# Post-contenido — Unidad 5: Integración en Aplicaciones Web

## Descripción
Repositorio del post-contenido de la Unidad 5 de Patrones de Diseño de Software. Un único proyecto
Spring Boot (`reservas-labs-api`) para la reserva de laboratorios de cómputo de la universidad, con una
API REST en capas (Entity, Repository, Service, Controller) sobre H2.

*(La Parte 2 —vista Thymeleaf sobre el mismo Service— se documenta al completarla.)*

## Cómo ejecutar
```
$ mvn clean package
$ mvn spring-boot:run        # o ./mvnw spring-boot:run
$ mvn test
```
- API REST: http://localhost:8080/api/reservas y http://localhost:8080/api/laboratorios
- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:reservas_labs_db`, usuario `sa`, sin contraseña)

Al arrancar se cargan tres laboratorios de ejemplo (`config/DatosIniciales`).

## Decisiones de diseño

### Punto de decisión 1 — Ubicación de la validación de solapamiento
La regla "no reservar un laboratorio en un horario que se solapa con otra reserva activa" se divide en
dos responsabilidades:

- **Repository: la pregunta de datos.** `ReservaRepository.buscarSolapamientos(labId, inicio, fin)`
  responde *¿qué reservas activas de este laboratorio se cruzan con este rango?* La consulta JPQL
  (`r.inicio < :fin AND r.fin > :inicio AND r.estado <> CANCELADA`) se resuelve en el motor de base de
  datos. Así viajan a memoria solo las filas en conflicto (normalmente cero) y no todo el historial del
  laboratorio, que crece sin límite semestre a semestre. Usar `<` y `>` estrictos hace que un rango
  contiguo (11:00–12:00 después de 09:00–11:00) no cuente como solapamiento. Lo verifica
  `ReservaRepositoryTest`.
- **Service: la decisión de negocio.** `ReservaService.crear()` decide qué significa ese resultado. Si
  la lista no está vacía, rechaza la reserva con `ReservaConflictException` y un mensaje que nombra el
  laboratorio. El Repository nunca lanza excepciones de negocio ni conoce el concepto de "conflicto":
  solo devuelve filas.

**¿Qué pasaría si el Controller llamara directamente a `buscarSolapamientos()`?**
- La regla quedaría en la capa HTTP, y cada punto de entrada tendría que repetirla (la vista MVC de la
  Parte 2, un futuro job de importación…). Basta con que uno lo olvide para guardar reservas
  solapadas.
- El Controller también tendría que coordinar en el orden correcto la carga del laboratorio, la
  validación de horario, la consulta y el `save`. Esa secuencia necesita una transacción que hoy
  aporta `@Transactional` en el Service.

**Alternativa descartada:** traer `findByLaboratorioId()` completo y comparar rangos en Java. Es
correcta para pocos datos, pero su costo crece con todas las reservas históricas del laboratorio,
aunque solo interesen las de un día.

### Punto de decisión 2 — Reglas con y sin apoyo del Repository
`validarHorarioYDuracion()` vive entera en `ReservaService`, con Java puro. Cubre cuatro reglas:
- el rango no puede estar invertido;
- la duración debe estar entre 30 minutos y 3 horas;
- la reserva debe estar dentro del horario 07:00–21:00;
- la reserva debe empezar y terminar el mismo día.

Ninguna de estas reglas necesita otra fila de la base: se deciden solo con los campos de la reserva
que se está creando.

**Criterio general:**
- Si la regla necesita comparar contra datos que **solo la base conoce** (otras reservas existentes),
  el Service se apoya en una consulta del Repository, pero conserva la decisión.
- Si la regla **solo depende del propio objeto**, involucrar al Repository sería una consulta
  innecesaria y acoplaría una regla de dominio pura a la persistencia.

Por eso además se valida primero lo barato (horario y duración) y después lo que cuesta una consulta
(solapamiento): una reserva a las 22:00 se rechaza sin tocar la base.

La diferencia también se refleja en el resultado HTTP, y aquí hubo una decisión adicional:

| Excepción de dominio | Cuándo | API REST |
|---|---|---|
| `ReservaInvalidaException` | La reserva es inválida **por sí misma** (rango, duración, horario) | **400** Bad Request |
| `ReservaConflictException` | La reserva es válida pero **choca con el estado del sistema** (horario ocupado, cancelar una reserva ya iniciada o ya cancelada) | **409** Conflict |
| `RecursoNoEncontradoException` | El laboratorio o la reserva no existen | **404** Not Found |

El código de la guía lanzaba `ReservaConflictException` también en `validarHorarioYDuracion`, lo que
habría convertido una reserva a las 22:00 en un 409. Eso contradice el checkpoint que exige un 400.
Se agregó `ReservaInvalidaException` porque un 409 indica "reintenta cuando cambie el estado", y una
reserva fuera del horario de atención nunca será válida, sin importar qué otras reservas existan.

### Nota — `LaboratorioController` usa el Repository directamente
El catálogo de laboratorios es un CRUD sin reglas propias. Un `LaboratorioService` que solo
delegara `findAll()`/`save()` sería el antipatrón de **Service anémico** que se pide evitar. La capa
Service se introduce cuando hay una regla que la justifique. `ReservaService` sí la tiene (horario,
duración, solapamiento, cancelación tardía o repetida), y por eso `ReservaController` nunca toca un
Repository.

## Herramientas utilizadas
- Java 17, Spring Boot 3.2, Spring Data JPA, H2, Lombok, Bean Validation
- Apache Maven, curl, Git, GitHub
