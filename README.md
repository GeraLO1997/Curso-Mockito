# Curso-Mockito

# 1. Realizando primeras pruebas con Mockito

## ¿Qué es Mockito?

Mockito es una biblioteca de Java para crear **mocks**: objetos simulados que sustituyen dependencias durante una prueba. **JUnit 5** comprueba resultados mediante assertions; **Mockito** controla las respuestas de colaboradores de la clase que probamos.

**Analogía:** queremos comprobar que un profesor sabe calificar un examen, sin depender de que la oficina administrativa le entregue exámenes reales. Un mock hace de oficina y entrega exactamente los exámenes que configuramos.

## Estructura del proyecto

```text
models/Examen.java
repositories/ExamenRepository.java
services/ExamenService.java
services/ExamenServiceImpl.java
services/ExamenServiceImplTest.java (en src/test/java)
```

- `Examen`: modelo con `Long id`, `String nombre`, `List<String> preguntas`. `@Getter` y `@Setter` de Lombok generan accesores. El constructor inicializa `preguntas` con `new ArrayList<>()`.
- `ExamenRepository`: interfaz con `List<Examen> findAll()`. Es la dependencia que simulamos.
- `ExamenService`: interfaz con `Optional<Examen> findExamenPorNombre(String nombre)`.
- `ExamenServiceImpl`: implementación real del servicio. Llama a `findAll()`, usa `stream()`, `filter(e -> e.getNombre().contains(nombre))` y `findFirst()` para devolver el primer examen coincidente.

### Diagrama

```text
Test de JUnit
   | crea
   v
ExamenServiceImpl (REAL)
   | usa
   v
ExamenRepository (MOCK)
   | findAll() configurado con thenReturn
   v
Lista de exámenes ficticios (objetos Examen REALES)
```

## Prueba completa, explicada

```java
import org.gerardo.appmockito.ejemplo.models.Examen;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepository;
import org.gerardo.appmockito.ejemplo.services.ExamenService;
import org.gerardo.appmockito.ejemplo.services.ExamenServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExamenServiceImplTest {

    @Test
    void findExamenPorNombre() {
        // 1. Crear el repositorio simulado.
        ExamenRepository repository = mock(ExamenRepository.class);

        // 2. Crear el servicio REAL e inyectarle el mock.
        ExamenService service = new ExamenServiceImpl(repository);

        // 3. Preparar datos de prueba.
        List<Examen> datos = Arrays.asList(
            new Examen(5L, "Matematicas"),
            new Examen(6L, "Lenguaje"),
            new Examen(7L, "Historia")
        );

        // 4. Configurar la respuesta del mock.
        when(repository.findAll()).thenReturn(datos);

        // 5. Ejecutar el método REAL del servicio.
        Optional<Examen> examen = service.findExamenPorNombre("Matematicas");

        // 6. Verificar el resultado con JUnit.
        assertTrue(examen.isPresent());
        Examen resultado = examen.orElseThrow();
        assertAll("Buscar examen por atributos",
            () -> assertEquals(5L, resultado.getId()),
            () -> assertEquals("Matematicas", resultado.getNombre())
        );

        // 7. Opcional: comprobar la interacción con el mock.
        verify(repository).findAll();
    }
}
```

## ¿Qué hace cada instrucción de Mockito?

| Instrucción | Significado |
|---|---|
| `mock(ExamenRepository.class)` | Crea un doble de prueba que implementa la interfaz; no ejecuta la implementación real. |
| `when(repository.findAll())` | Indica qué invocación queremos configurar. |
| `.thenReturn(datos)` | Hace que esa invocación devuelva `datos`. |
| `verify(repository).findAll()` | Comprueba que el servicio invocó una vez `findAll()` (una vez es el valor predeterminado). |
| `verify(repository, times(1)).findAll()` | Variante explícita para verificar una invocación. |

**Stubbing** es configurar respuestas (`when`/`thenReturn`). **Verification** es comprobar interacciones (`verify`). Son conceptos diferentes.

### Flujo exacto

1. El test crea un mock del repositorio.
2. El test crea un servicio real que recibe ese mock por constructor.
3. El test configura `when(repository.findAll()).thenReturn(datos)`.
4. El test llama a `service.findExamenPorNombre("Matematicas")`.
5. El servicio invoca `repository.findAll()`; Mockito devuelve `datos`.
6. El servicio ejecuta su lógica real de búsqueda y devuelve `Optional<Examen>`.
7. JUnit verifica que el examen tenga ID `5L` y nombre `Matematicas`.

## Explicación de clases y métodos Java

- `List<Examen>`: interfaz de colección ordenada.
- `Arrays.asList(...)`: construye una lista de tamaño fijo con los objetos indicados.
- `Optional<Examen>`: contenedor que puede tener un examen o estar vacío.
- `isPresent()`: comprueba si el `Optional` contiene un valor.
- `orElseThrow()`: obtiene el valor o lanza excepción si está vacío.
- `stream()`: permite procesar los elementos de una colección.
- `filter(...)`: conserva los elementos que satisfacen la condición.
- `String.contains(nombre)`: busca una subcadena; distingue mayúsculas de minúsculas.
- `findFirst()`: devuelve el primer elemento coincidente dentro de un `Optional`.
- `@Test`: indica a JUnit que es un método de prueba.
- `assertAll(...)`: agrupa comprobaciones.
- `assertEquals(...)`: compara esperado contra real.
- `assertTrue(...)`: exige una condición verdadera.

## Inyección de dependencias y Lombok

En tu código aparecen juntas `@RequiredArgsConstructor` y `@AllArgsConstructor`:

```java
@RequiredArgsConstructor
@AllArgsConstructor
public class ExamenServiceImpl implements ExamenService {
    private ExamenRepository examenRepository;
}
```

Como el campo no es `final` ni tiene `@NonNull`, `@RequiredArgsConstructor` genera un constructor sin parámetros; `@AllArgsConstructor` genera el constructor con `ExamenRepository`. Una opción más clara es:

```java
@RequiredArgsConstructor
public class ExamenServiceImpl implements ExamenService {
    private final ExamenRepository examenRepository;

    @Override
    public Optional<Examen> findExamenPorNombre(String nombre) {
        return examenRepository.findAll().stream()
            .filter(e -> e.getNombre().contains(nombre))
            .findFirst();
    }
}
```

Lombok genera el constructor que recibe `ExamenRepository`, por ser un campo `final`. Esto facilita la inyección por constructor y evita sustituir la dependencia después de crear el servicio.

## Caso alternativo: examen no encontrado

```java
@Test
void noEncuentraExamen() {
    ExamenRepository repository = mock(ExamenRepository.class);
    ExamenService service = new ExamenServiceImpl(repository);
    when(repository.findAll()).thenReturn(List.of(
        new Examen(5L, "Matematicas")
    ));

    Optional<Examen> resultado = service.findExamenPorNombre("Historia");

    assertTrue(resultado.isEmpty());
    verify(repository).findAll();
}
```

## Consejos y errores frecuentes

1. **Simula la dependencia, no la clase que quieres probar.** El servicio debe ejecutar su lógica real.
2. **Un mock no es una base de datos falsa completa.** Solo responde como lo configures; las llamadas no configuradas utilizan respuestas predeterminadas (por ejemplo, listas vacías para métodos que devuelven `List` en Mockito moderno).
3. **No necesitas crear mocks de objetos de datos simples** como `Examen`; puedes construirlos con `new`.
4. **Evita `assertNotNull(examen)` como única prueba:** `Optional.empty()` no es nulo. Comprueba `isPresent()` o `isEmpty()`.
5. **Cuidado con `contains`:** `"Matematicas".contains("Mate")` es verdadero. Si necesitas igualdad exacta, utiliza `equals()`; para ignorar mayúsculas/minúsculas, estudia `equalsIgnoreCase()`.
6. **Sobre métodos simulables:** no es correcto afirmar que Mockito solo puede simular métodos públicos o `default`; el soporte depende del tipo de método y la configuración de Mockito. No puede interceptar métodos `private` de manera directa como si fueran métodos normales del mock.
7. **No confundas `when(...).thenReturn(...)` con una aserción.** Configurar el mock no comprueba nada; las aserciones verifican resultados y `verify` verifica llamadas.
8. **Pruebas independientes:** cada test debe preparar sus datos y mocks para evitar dependencias entre ejecuciones.

## Resumen de bolsillo

```java
ExamenRepository repo = mock(ExamenRepository.class);  // Simular dependencia
ExamenService service = new ExamenServiceImpl(repo);   // Servicio real
when(repo.findAll()).thenReturn(datos);                // Programar respuesta
Optional<Examen> resultado = service.findExamenPorNombre("Matematicas");
assertTrue(resultado.isPresent());                     // Verificar resultado
verify(repo).findAll();                                // Verificar interacción
```

**Regla para recordar:** Mockito prepara el escenario; el servicio actúa; JUnit comprueba el resultado.
