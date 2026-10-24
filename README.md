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

# 2. Agregando nuevas dependencias Mock y probándolas

## Idea principal

Mockito permite simular **varias dependencias** de un servicio real. En este ejemplo, `ExamenServiceImpl` utiliza dos repositorios: `ExamenRepository` para buscar exámenes y `PreguntaRepository` para consultar sus preguntas. Los repositorios se sustituyen por mocks, mientras que la lógica del servicio se ejecuta realmente.

## Estructura

- `Examen`: modelo con `Long id`, `String nombre`, `List<String> preguntas`. Lombok `@Getter`/`@Setter` genera accesores; el constructor inicializa `preguntas` como `ArrayList` vacía.
- `ExamenRepository`: interfaz con `List<Examen> findAll()`.
- `PreguntaRepository`: interfaz con `List<String> findPreguntasPorExamenId(Long id)`.
- `ExamenService`: interfaz con `Optional<Examen> findExamenPorNombre(String nombre)` y `Examen findExamenPorNombreConPreguntas(String nombre)`.
- `ExamenServiceImpl`: implementación real que coordina ambos repositorios.
- `Datos`: clase de apoyo con exámenes y preguntas ficticios.
- `ExamenServiceImplTest`: pruebas JUnit 5 con Mockito.

## Flujo del método nuevo

```java
@Override
public Examen findExamenPorNombreConPreguntas(String nombre) {
    Optional<Examen> examenOptional = findExamenPorNombre(nombre);
    Examen examen = null;
    if (examenOptional.isPresent()) {
        examen = examenOptional.orElseThrow();
        List<String> preguntas = preguntaRepository.findPreguntasPorExamenId(examen.getId());
        examen.setPreguntas(preguntas);
    }
    return examen;
}
```

1. `findExamenPorNombre` consulta `examenRepository.findAll()` y usa `stream()`, `filter()` y `findFirst()`.
2. `isPresent()` comprueba si existe un examen.
3. `orElseThrow()` extrae el examen del `Optional`.
4. `getId()` obtiene su identificador.
5. `findPreguntasPorExamenId(id)` consulta el segundo repositorio.
6. `setPreguntas()` agrega las preguntas al objeto y devuelve el examen.
7. Si no existe, devuelve `null` y **no llama** al repositorio de preguntas.

## Crear mocks e inyectarlos con `@BeforeEach`

```java
private ExamenRepository repository;
private PreguntaRepository preguntaRepository;
private ExamenService service;

@BeforeEach
void setUp() {
    repository = mock(ExamenRepository.class);
    preguntaRepository = mock(PreguntaRepository.class);
    service = new ExamenServiceImpl(repository, preguntaRepository);
}
```

- `mock(Interfaz.class)` crea un doble de prueba del tipo indicado.
- `new ExamenServiceImpl(...)` construye el **servicio real** e inyecta los mocks por constructor.
- `@BeforeEach` crea mocks nuevos antes de cada prueba; evita que se compartan configuraciones e interacciones.

## Configurar ambos mocks con `when().thenReturn()`

```java
when(repository.findAll()).thenReturn(Datos.EXAMENES);
when(preguntaRepository.findPreguntasPorExamenId(5L))
        .thenReturn(Datos.PREGUNTAS);
```

- La primera línea indica que `findAll()` debe devolver tres exámenes.
- La segunda devuelve cinco preguntas **solo si se solicita el ID `5L`**.
- `when(...)` identifica la llamada a simular; `thenReturn(...)` establece la respuesta.

## `anyLong()` y coincidencia de argumentos

```java
when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
        .thenReturn(Datos.PREGUNTAS);
```

`anyLong()` es un **argument matcher**: coincide con cualquier argumento de tipo `long` o `Long` no nulo, en lugar de exigir un identificador concreto. **No significa “cualquier tipo de dato”**. Para validar la relación entre examen y preguntas, suele ser mejor usar `eq(5L)` o el literal `5L`, ya que detecta IDs incorrectos.

## Test completo de búsqueda con preguntas

```java
@Test
void testPreguntasExamen() {
    when(repository.findAll()).thenReturn(List.of(
        new Examen(5L, "Matematicas"),
        new Examen(6L, "Lenguaje"),
        new Examen(7L, "Historia")
    ));
    when(preguntaRepository.findPreguntasPorExamenId(5L))
        .thenReturn(List.of("aritmetica", "integrales", "derivadas", "trigonometria", "geometria"));

    Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

    assertNotNull(examen);
    assertAll("Examen con preguntas",
        () -> assertEquals(5L, examen.getId()),
        () -> assertEquals("Matematicas", examen.getNombre()),
        () -> assertEquals(5, examen.getPreguntas().size()),
        () -> assertTrue(examen.getPreguntas().contains("aritmetica"))
    );

    verify(repository).findAll();
    verify(preguntaRepository).findPreguntasPorExamenId(5L);
}
```

**Por qué comprobar `assertNotNull(examen)` primero:** el servicio puede devolver `null`. Si es `null`, llamar a `getPreguntas()` provocaría `NullPointerException`.

## Probar lista vacía y examen inexistente

```java
@Test
void testSinExamenes() {
    when(repository.findAll()).thenReturn(Collections.emptyList());
    Optional<Examen> resultado = service.findExamenPorNombre("Matematicas");
    assertTrue(resultado.isEmpty());
    verify(repository).findAll();
    verifyNoInteractions(preguntaRepository);
}

@Test
void testSinExamenNoConsultaPreguntas() {
    when(repository.findAll()).thenReturn(Collections.emptyList());
    Examen resultado = service.findExamenPorNombreConPreguntas("Matematicas");
    assertNull(resultado);
    verify(repository).findAll();
    verifyNoInteractions(preguntaRepository);
}
```

`Collections.emptyList()` crea una lista vacía. `verifyNoInteractions(...)` comprueba que el mock de preguntas no fue utilizado.

## `verify()` frente a `assert...`

- `assertEquals`, `assertTrue`, `assertNotNull` y `assertNull` son **aserciones de JUnit** que verifican resultados.
- `verify(mock).metodo(...)` es de **Mockito** y verifica llamadas.
- `verify(mock, times(1))` comprueba una llamada; una llamada es el valor predeterminado de `verify(mock)`.
- `verifyNoInteractions(mock)` comprueba que no hubo interacciones.

## Precaución con la clase `Datos`

```java
public static final List<Examen> EXAMENES = Arrays.asList(...);
```

`static final` evita reasignar la referencia, **pero no vuelve inmutables los objetos `Examen`**. Como `findExamenPorNombreConPreguntas` modifica el examen con `setPreguntas`, una prueba puede afectar a otras si comparten instancias. Conviene crear nuevos objetos en cada prueba o usar un método fábrica:

```java
public static List<Examen> crearExamenes() {
    return List.of(
        new Examen(5L, "Matematicas"),
        new Examen(6L, "Lenguaje"),
        new Examen(7L, "Historia")
    );
}
```

Cada llamada crea nuevos exámenes, aunque la lista devuelta no permita agregar o quitar elementos.

## Lombok y diseño

En la clase original, `@RequiredArgsConstructor` y `@AllArgsConstructor` aparecen juntos. Dado que los campos no son `final` ni `@NonNull`, `@RequiredArgsConstructor` genera un constructor sin argumentos, mientras que `@AllArgsConstructor` genera el constructor con ambos repositorios. Una opción más clara es:

```java
@RequiredArgsConstructor
public class ExamenServiceImpl implements ExamenService {
    private final ExamenRepository examenRepository;
    private final PreguntaRepository preguntaRepository;
    // métodos de negocio...
}
```

Así Lombok genera el constructor con ambas dependencias obligatorias.

## Tips y errores comunes

1. **Simula las dependencias, no el servicio que deseas probar.**
2. **`anyLong()` acepta IDs de tipo `long`/`Long` no nulos, no cualquier tipo de dato.**
3. Prefiere configurar el ID específico `5L` cuando sea importante verificar la relación correcta.
4. Comprueba tanto el contenido del examen como las interacciones necesarias con los repositorios.
5. Incluye casos sin resultados y verifica que no se consulten preguntas innecesariamente.
6. Evita compartir objetos mutables entre pruebas mediante constantes `static final`.
7. Considera devolver `Optional<Examen>` también en `findExamenPorNombreConPreguntas` para evitar `null` (mejora de diseño opcional, no parte del código actual).

## Importaciones principales

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
```

**Resumen:** el servicio es real, los dos repositorios son mocks; `when(...).thenReturn(...)` prepara las respuestas, JUnit verifica resultados y `verify()` comprueba las interacciones.

# 3. Probando interacciones con `verify()` en Mockito

## Concepto principal

Mockito permite **simular respuestas** mediante `when(...).thenReturn(...)` y **verificar llamadas** mediante `verify(...)`. JUnit (`assertEquals`, `assertNull`, etc.) comprueba resultados; Mockito `verify` comprueba interacciones con dependencias simuladas.

**Analogía:** además de comprobar que un empleado entregó las preguntas correctas, comprobamos si consultó al departamento correcto.

## Sintaxis

```java
verify(repository).findAll(); // Exactamente una vez
verify(repository, times(1)).findAll(); // Equivalente
verify(repository, never()).findAll(); // Ninguna vez
verify(repository, times(2)).findAll(); // Exactamente dos veces
```

`verify(mock)` devuelve un objeto que permite indicar, después del punto, **qué método y argumentos** se desean comprobar. No se coloca `verify` alrededor de una llamada real para ejecutarla: verifica el historial de invocaciones que Mockito registró.

## Primera prueba: examen con preguntas

```java
@Test
void testPreguntasExamenVerify() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);
    when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
            .thenReturn(Datos.PREGUNTAS);

    Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

    assertNotNull(examen);
    assertEquals(5, examen.getPreguntas().size());
    assertTrue(examen.getPreguntas().contains("aritmetica"));

    verify(repository).findAll();
    verify(preguntaRepository).findPreguntasPorExamenId(5L);
}
```

**Secuencia:**
1. El servicio real llama a `repository.findAll()`.
2. El mock devuelve la lista configurada.
3. El servicio encuentra el examen de Matemáticas con ID `5L`.
4. Llama a `preguntaRepository.findPreguntasPorExamenId(5L)`.
5. El segundo mock devuelve cinco preguntas.
6. JUnit verifica el resultado y Mockito comprueba las llamadas.

`anyLong()` es un *argument matcher*: acepta argumentos primitivos `long` y valores `Long` no nulos, pero no significa «cualquier tipo». `verify(...).findPreguntasPorExamenId(anyLong())` acepta cualquier ID compatible. Si la lógica requiere específicamente el ID `5L`, `verify(...).findPreguntasPorExamenId(5L)` es más preciso.

## Segunda prueba: examen inexistente

**El código original falla** porque exige que se invoque el repositorio de preguntas cuando no se encontró ningún examen:

```java
when(repository.findAll()).thenReturn(Collections.emptyList());
Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");
assertNull(examen);
verify(repository).findAll();
verify(preguntaRepository).findPreguntasPorExamenId(anyLong()); // INCORRECTO
```

En `ExamenServiceImpl`, la consulta de preguntas ocurre **solo si** `examenOptional.isPresent()` es verdadero. Con lista vacía, el servicio devuelve `null` y nunca consulta preguntas.

### Corrección recomendada

```java
@Test
void testNoExisteExamenVerify() {
    when(repository.findAll()).thenReturn(Collections.emptyList());

    Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

    assertNull(examen);
    verify(repository).findAll();
    verifyNoInteractions(preguntaRepository);
}
```

`verifyNoInteractions(preguntaRepository)` comprueba que no hubo **ninguna** interacción con ese mock. Otra alternativa:

```java
verify(preguntaRepository, never()).findPreguntasPorExamenId(anyLong());
```

Esta segunda instrucción verifica únicamente que **ese método con ese patrón de argumentos** no fue invocado; otras llamadas al mock podrían haber ocurrido. `verifyNoInteractions()` es más estricto.

**Importante:** `when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS)` no provoca que el método se ejecute desde el servicio. Solo configura qué devolvería **si** fuera llamado. En este escenario, la configuración es innecesaria y puede generar advertencias de stubbing innecesario con configuraciones estrictas de Mockito.

## Métodos relacionados

| Método | Significado |
|---|---|
| `verify(mock).metodo()` | Método llamado exactamente una vez. |
| `verify(mock, times(n)).metodo()` | Llamado exactamente `n` veces. |
| `verify(mock, never()).metodo()` | Ese método no fue llamado. |
| `verifyNoInteractions(mock)` | No hubo ninguna interacción con el mock. |
| `verifyNoMoreInteractions(mock)` | No quedan interacciones sin verificar en el mock. Úsalo con moderación. |
| `anyLong()` | Coincide con argumentos `long` o `Long` no nulos. |
| `eq(5L)` | Coincide con el valor específico `5L`, útil al combinar matchers. |
| `assertNull(objeto)` | JUnit verifica que el resultado sea `null`. |

## Errores y consejos

- **Configurar no es verificar:** `when(...).thenReturn(...)` define una respuesta; `verify(...)` comprueba una interacción que realmente ocurrió.
- **No exigir llamadas innecesarias:** si el examen no existe, consultar preguntas sería un error de lógica.
- **Preferir argumentos exactos** (`5L`) cuando interesa comprobar que se utilizó el identificador correcto.
- **No verificar cada llamada interna por costumbre:** verificar interacciones relevantes para el comportamiento, sin acoplar demasiado las pruebas a detalles de implementación.
- **Evitar datos mutables compartidos:** `Datos.EXAMENES` contiene objetos `Examen` modificables; `setPreguntas()` podría afectar pruebas posteriores. Preferir crear instancias nuevas por prueba.
- **Usar `@BeforeEach`** para crear mocks nuevos e inyectarlos al servicio real.

## Importaciones

```java
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
```

## Regla para memorizar

**`when` prepara → el servicio actúa → `assert` comprueba el resultado → `verify` comprueba la interacción.**


# 4. Inyección de dependencias con @Mock, @InjectMocks y @ExtendWith

En las lecciones anteriores creamos nuestros mocks manualmente:
```java
    repository = mock(ExamenRepository.class);
    preguntaRepository = mock(PreguntaRepository.class);
    
    service = new ExamenServiceImpl(repository, preguntaRepository);
```


Ahora vamos a conseguir exactamente el mismo objetivo, pero utilizando anotaciones de Mockito.

Estas anotaciones permiten escribir pruebas más limpias, reducir código repetitivo y preparar automáticamente las dependencias que necesita la clase que estamos probando.

## 1. ¿Qué es la inyección de dependencias?
   La inyección de dependencias consiste en proporcionar a una clase los objetos que necesita para funcionar, en lugar de obligarla a crearlos por su cuenta.

En nuestro proyecto, ExamenServiceImpl necesita dos dependencias:
- ExamenRepository, para buscar exámenes.
- PreguntaRepository, para buscar preguntas.
  Imagina que el servicio es un cocinero y los repositorios son sus proveedores de ingredientes.

El cocinero no tiene que fabricar los ingredientes: alguien se los entrega para que pueda preparar el platillo.

En una prueba unitaria, Mockito nos permite entregarle proveedores simulados.
## 2. Antes y después de utilizar anotaciones
```java
@BeforeEach
void setUp() {
    repository = mock(ExamenRepository.class);
    preguntaRepository = mock(PreguntaRepository.class);

    service = new ExamenServiceImpl(
        repository, preguntaRepository
    );
}
```

Ahora utilizando anotaciones

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {

    @Mock
    ExamenRepository repository;

    @Mock
    PreguntaRepository preguntaRepository;

    @InjectMocks
    ExamenServiceImpl service;
}
```

Las dos formas son válidas. La segunda aprovecha la integración de Mockito con JUnit 5.

## 3. ¿Qué hace `@ExtendWith(MockitoExtension.class)`?

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {
}
```

`@ExtendWith` es una anotación de JUnit 5 que registra una extensión para participar en la ejecución de las pruebas.

MockitoExtension es la extensión de Mockito para JUnit Jupiter.

Al combinarlas, Mockito puede inicializar los campos anotados con @Mock y @InjectMocks antes de cada prueba, además de proporcionar otras funciones como la validación de configuraciones innecesarias.

Sin esta extensión, las anotaciones de Mockito no se inicializan automáticamente por el simple hecho de escribirlas.

Importante: `@ExtendWith` pertenece a JUnit 5, mientras que MockitoExtension pertenece a la integración de Mockito con JUnit Jupiter.

## 4. ¿Qué hace `@Mock`?

```java
@Mock
ExamenRepository repository;

@Mock
PreguntaRepository preguntaRepository;
```

`@Mock` indica que Mockito debe crear un objeto simulado para ese campo.

Es equivalente, en cuanto a la creación del mock, a escribir:

```java
repository = mock(ExamenRepository.class);
preguntaRepository = mock(PreguntaRepository.class);
```

La diferencia es que ahora no necesitas llamar manualmente a mock().

Estos mocks siguen siendo configurables mediante when() y comprobables mediante verify().

Por ejemplo:

```java
when(repository.findAll()).thenReturn(Datos.EXAMENES);

verify(repository).findAll();
```

La anotación cambia la forma de crear el objeto, pero no cambia cómo funciona Mockito.
## 5. ¿Qué hace `@InjectMocks`?

```java
@InjectMocks
ExamenServiceImpl service;
```

Esta anotación indica a Mockito que prepare una instancia de ExamenServiceImpl e intente inyectarle los mocks disponibles.

En nuestro caso, identifica los campos:

```java
@Mock
ExamenRepository repository;

@Mock
PreguntaRepository preguntaRepository;
```

Y los utiliza para preparar el servicio.

`@InjectMocks` ExamenServiceImpl service -> `@Mock` (ExamenRepository) && `@Mock` (PreguntaRepository)

¿Por qué utilizamos la clase concreta y no la interfaz?
Tu código dice:

```java
@InjectMocks
ExamenServiceImpl service;
```

Y no:

```java
@InjectMocks
ExamenService service;
```

Esto se debe a que Mockito necesita una clase concreta que pueda instanciar. Una interfaz no tiene constructor ni una implementación propia que Mockito pueda utilizar para crear el servicio real.

Además, @InjectMocks no convierte el servicio en un mock. El servicio sigue ejecutando su código real; lo que se simula son sus dependencias.
¿Cómo inyecta Mockito las dependencias?
Mockito intenta resolver la inyección mediante tres mecanismos:
1. Constructor: intenta utilizar un constructor apropiado, preferentemente el de mayor número de parámetros.
2. Setters: si corresponde, intenta asignar mocks mediante métodos de acceso.
3. Campos: puede intentar asignarlos directamente.
   Estos mecanismos no garantizan que todas las dependencias queden correctamente inyectadas. Por eso es recomendable utilizar un constructor explícito y dependencias final.

Para tu servicio:

```java
@RequiredArgsConstructor
public class ExamenServiceImpl implements ExamenService {

    private final ExamenRepository examenRepository;
    private final PreguntaRepository preguntaRepository;

    // Métodos del servicio...
}
```

Lombok genera el constructor que recibe ambos repositorios, y Mockito puede utilizarlo para crear el servicio.
## 6. ¿Para qué sirve MockitoAnnotations.openMocks(this)?
   En tu código aparece esta instrucción comentada:

```java
MockitoAnnotations.openMocks(this);
```

Es otra forma de inicializar los campos anotados con @Mock y @InjectMocks.

this hace referencia al objeto actual de la clase de prueba. Mockito lo inspecciona para encontrar las anotaciones.

Sin embargo, hay una diferencia importante:

| Con MockitoExtension |	Con openMocks(this) |
--- | ---|
Inicialización automática. |	Inicialización manual.
Se registra con @ExtendWith. |	Se llama normalmente desde @BeforeEach.
Integra funcionalidades adicionales de Mockito con JUnit 5. |	Devuelve un AutoCloseable que debe cerrarse al terminar.
Recomendado para tus pruebas actuales. |	Útil cuando no se utiliza la extensión.

Si eliges la alternativa manual, una implementación sería:

```java
private AutoCloseable mocks;

@BeforeEach
void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
}

@AfterEach
void tearDown() throws Exception {
    mocks.close();
}
```

No necesitas combinar ambas opciones. Con @ExtendWith(MockitoExtension.class) puedes eliminar tu setUp() si solamente lo utilizabas para crear los mocks e inyectarlos.
## 7. Ejemplo completo utilizando las anotaciones

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {

    @Mock
    ExamenRepository repository;

    @Mock
    PreguntaRepository preguntaRepository;

    @InjectMocks
    ExamenServiceImpl service;

    @Test
    void testPreguntasExamen() {

        when(repository.findAll())
                .thenReturn(Datos.EXAMENES);

        when(preguntaRepository.findPreguntasPorExamenId(5L))
                .thenReturn(Datos.PREGUNTAS);

        Examen examen =
                service.findExamenPorNombreConPreguntas("Matematicas");

        assertNotNull(examen);

        assertAll(
            () -> assertEquals(5L, examen.getId()),
            () -> assertEquals(5, examen.getPreguntas().size()),
            () -> assertTrue(examen.getPreguntas().contains("aritmetica"))
        );

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(5L);
    }
}
```

Observa que seguimos utilizando when(), thenReturn(), assertAll() y verify() exactamente igual que antes.

Lo único que cambió fue la preparación de los mocks y del servicio.
## 8. Dependencia necesaria en Maven
   Para utilizar MockitoExtension, necesitas la integración de Mockito con JUnit Jupiter:
   
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

La versión debe estar definida explícitamente o administrada por tu proyecto, por ejemplo mediante un BOM o dependencyManagement.
## 9. Acordeón rápido

Anotación o método |	Función
--- | ---
@ExtendWith(MockitoExtension.class) |	Activa la integración entre JUnit 5 y Mockito.
@Mock |	Crea una dependencia simulada.
@InjectMocks |	Prepara el objeto real que queremos probar e intenta inyectarle los mocks.
@BeforeEach |	Ejecuta código de preparación antes de cada test.
MockitoAnnotations.openMocks(this) |	Inicializa manualmente las anotaciones de Mockito.
when().thenReturn() |	Configura las respuestas simuladas.
verify() |	Comprueba las llamadas realizadas a los mocks.

La idea que debes memorizar:

@Mock crea los colaboradores falsos, @InjectMocks los coloca en la clase real que estamos probando y @ExtendWith(MockitoExtension.class) permite que JUnit 5 administre esa preparación automáticamente.

Esta combinación es muy utilizada en pruebas unitarias de servicios Java, incluyendo proyectos con Spring Boot, porque permite probar la lógica del servicio sin levantar un contexto completo de Spring.

No pude generar el archivo .md descargable en esta ocasión. La explicación queda aquí organizada para que puedas copiarla a tus apuntes.

# 5. Probando el método guardar() con Mockito

## Objetivo

Probar la lógica real de `ExamenServiceImpl.guardar(Examen)` aislando sus dos dependencias con Mockito. El servicio guarda preguntas **solo si la lista no está vacía** y después guarda el examen.

```java
@Override
public Examen guardar(Examen examen) {
    if (!examen.getPreguntas().isEmpty()) {
        preguntaRepository.guardarVarias(examen.getPreguntas());
    }
    return examenRepository.guardar(examen);
}
```

## Interfaces implicadas

```java
public interface ExamenRepository {
    Examen guardar(Examen examen);
    List<Examen> findAll();
}

public interface PreguntaRepository {
    void guardarVarias(List<String> preguntas);
    List<String> findPreguntasPorExamenId(Long id);
}
```

`ExamenRepository.guardar()` devuelve un `Examen`; `PreguntaRepository.guardarVarias()` es `void`, por lo que no devuelve valor.

## Entender `when(repository.guardar(any(Examen.class))).thenReturn(Datos.EXAMEN)`

1. `repository` es el mock de `ExamenRepository`.
2. `guardar(...)` es la llamada que queremos simular.
3. `any(Examen.class)` es un *argument matcher*: coincide con cualquier instancia no nula compatible con `Examen`.
4. `when(...)` inicia la configuración (*stubbing*) de la llamada.
5. `thenReturn(Datos.EXAMEN)` define el objeto devuelto por el mock cuando recibe una llamada coincidente.

```java
when(repository.guardar(any(Examen.class)))
        .thenReturn(new Examen(8L, "Fisica"));
```

**No se guarda realmente nada en una base de datos.** El mock simplemente devuelve el objeto configurado. Incluso si recibe un examen distinto, devolverá el objeto fijado en `thenReturn`.

## Preparación con anotaciones

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {
    @Mock ExamenRepository repository;
    @Mock PreguntaRepository preguntaRepository;
    @InjectMocks ExamenServiceImpl service;
}
```

- `@ExtendWith(MockitoExtension.class)` activa la integración de Mockito con JUnit Jupiter.
- `@Mock` crea dependencias simuladas.
- `@InjectMocks` crea/prepara el servicio real e intenta inyectarle los mocks.

Se recomienda usar inyección por constructor y atributos `final` en `ExamenServiceImpl`.

## Prueba 1: guardar un examen sin preguntas

```java
@Test
@DisplayName("Guardar examen sin preguntas")
void testGuardarExamenSinPreguntas() {
    Examen entrada = new Examen(null, "Fisica");
    Examen guardado = new Examen(8L, "Fisica");

    when(repository.guardar(same(entrada))).thenReturn(guardado);

    Examen resultado = service.guardar(entrada);

    assertNotNull(resultado);
    assertEquals(8L, resultado.getId());
    assertEquals("Fisica", resultado.getNombre());

    verify(repository).guardar(same(entrada));
    verifyNoInteractions(preguntaRepository);
}
```

`same(entrada)` comprueba identidad de referencia: el servicio entrega al repositorio exactamente ese objeto. `verifyNoInteractions` comprueba que no se llamó a ningún método del repositorio de preguntas.

## Prueba 2: guardar un examen con preguntas

```java
@Test
@DisplayName("Guardar examen con preguntas")
void testGuardarExamenConPreguntas() {
    Examen entrada = new Examen(null, "Fisica");
    entrada.setPreguntas(List.of("Movimiento", "Energia"));
    Examen guardado = new Examen(8L, "Fisica");

    when(repository.guardar(same(entrada))).thenReturn(guardado);

    Examen resultado = service.guardar(entrada);

    assertNotNull(resultado);
    assertEquals(8L, resultado.getId());
    assertEquals("Fisica", resultado.getNombre());

    verify(preguntaRepository).guardarVarias(entrada.getPreguntas());
    verify(repository).guardar(same(entrada));
}
```

El método `guardarVarias` es `void`: por defecto, un mock no hace nada cuando se invoca. No hace falta `when(...).thenReturn(...)`; basta con verificar la llamada.

## `anyList()` y verificación de argumentos

```java
verify(preguntaRepository).guardarVarias(anyList());
```

`anyList()` acepta cualquier lista no nula. Comprueba que se pasó una lista, **pero no comprueba su contenido**. Para validar el contenido, usa una lista esperada, `eq(...)` o `ArgumentCaptor` (tema posterior):

```java
verify(preguntaRepository).guardarVarias(
    List.of("Movimiento", "Energia")
);
```

## Errores importantes en los ejemplos originales

1. En la prueba **sin preguntas**, `verify(preguntaRepository).guardarVarias(anyList())` es incorrecto: el servicio no invoca ese método. Debe utilizarse `never()` o `verifyNoInteractions(preguntaRepository)`.
2. `Datos.EXAMEN` es un objeto `static final`, **no inmutable**. Hacer `newExamen = Datos.EXAMEN` y después `setPreguntas(...)` modifica el mismo objeto compartido. Las pruebas pueden influirse entre sí. Crea un `new Examen(...)` por test.
3. Si configuras `thenReturn(Datos.EXAMEN)` y también envías `Datos.EXAMEN`, las aserciones de ID y nombre pueden pasar sin demostrar que se persistió nada. Un caso más realista usa un examen de entrada sin ID y otro devuelto con ID.
4. `verify(repository).guardar(any(Examen.class))` no comprueba cuál examen se envió. Usa `same(entrada)`, `eq(entrada)` o un captor cuando sea relevante.
5. La implementación actual guarda las preguntas **antes** de guardar el examen. En sistemas reales, puede ser necesario guardar primero el examen para obtener su ID y vincular las preguntas, además de gestionar transacciones. Aquí solo se prueba el comportamiento implementado.

## Resumen rápido

| Herramienta | Función |
|---|---|
| `when(...).thenReturn(...)` | Configura el valor que devolverá un método simulado. |
| `any(Examen.class)` | Coincide con cualquier instancia no nula compatible con `Examen`. |
| `anyList()` | Coincide con cualquier lista no nula. |
| `same(objeto)` | Coincide con la misma referencia de objeto. |
| `verify(mock).metodo()` | Comprueba que el método se llamó exactamente una vez. |
| `verifyNoInteractions(mock)` | Comprueba que el mock no recibió llamadas. |
| `never()` | Comprueba que una llamada específica no ocurrió. |

**Regla:** Mockito simula la persistencia; JUnit valida el resultado; `verify` comprueba qué métodos fueron invocados y con qué argumentos.
