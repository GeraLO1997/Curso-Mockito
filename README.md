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

# 5. Mockito: probando `guardar()` con la clase `Datos`

## Clase `Datos` utilizada

```java
public class Datos {
    public static final List<Examen> EXAMENES = Arrays.asList(
        new Examen(5L, "Matematicas"),
        new Examen(6L, "Lenguaje"),
        new Examen(7L, "Historia")
    );
    public static final List<String> PREGUNTAS = Arrays.asList(
        "aritmetica", "integrales", "derivadas", "trigonometria", "geometria"
    );
    public static final Examen EXAMEN = new Examen(8L, "Fisica");
}
```

## ¿Qué hace `when(repository.guardar(any(Examen.class))).thenReturn(Datos.EXAMEN)`?

- `repository` es el mock de `ExamenRepository`.
- `guardar(...)` es el método cuya respuesta configuramos.
- `any(Examen.class)` coincide con cualquier argumento no nulo compatible con `Examen`.
- `when(...)` selecciona la invocación que se simulará.
- `thenReturn(Datos.EXAMEN)` hace que el mock devuelva la referencia al examen de Física (ID `8L`) definida en `Datos`, sin guardar realmente en base de datos.

## `testGuardarExamen`: examen sin preguntas

```java
@Test
@DisplayName("Guardar Examen")
void testGuardarExamen() {
    when(repository.guardar(any(Examen.class))).thenReturn(Datos.EXAMEN);

    Examen examen = service.guardar(Datos.EXAMEN);
    assertNotNull(examen.getId());
    assertEquals(8L, examen.getId());
    assertEquals("Fisica", examen.getNombre());

    verify(repository).guardar(any(Examen.class));
    // Corrección respecto al ejemplo original:
    verify(preguntaRepository, never()).guardarVarias(anyList());
}
```

**Flujo:** el examen recién creado tiene preguntas vacías; `service.guardar()` omite `preguntaRepository.guardarVarias()`, llama a `examenRepository.guardar(examen)` y recibe `Datos.EXAMEN` por la configuración de `when`. `verify(repository)` confirma una llamada. La verificación original `verify(preguntaRepository).guardarVarias(anyList())` sería incorrecta para este caso: debe ser `never()` o `verifyNoInteractions(preguntaRepository)`.

## `testGuardarExamenConPreguntas`

```java
@Test
@DisplayName("Guardar Examen con preguntas")
void testGuardarExamenConPeguntas() {
    Examen newExamen = Datos.EXAMEN;
    newExamen.setPreguntas(Datos.PREGUNTAS);

    when(repository.guardar(any(Examen.class))).thenReturn(Datos.EXAMEN);

    Examen examen = service.guardar(newExamen);
    assertNotNull(examen.getId());
    assertEquals(8L, examen.getId());
    assertEquals("Fisica", examen.getNombre());

    verify(repository).guardar(any(Examen.class));
    verify(preguntaRepository).guardarVarias(anyList());
}
```

**Flujo:** se asignan cinco preguntas a `Datos.EXAMEN`. Al ejecutar el servicio, el `if (!examen.getPreguntas().isEmpty())` es verdadero y se llama al mock `preguntaRepository.guardarVarias(...)`. Como el método devuelve `void`, el mock no hace nada por defecto; sí registra la llamada para `verify`. Luego se llama al mock `repository.guardar(...)`, que devuelve `Datos.EXAMEN` por `thenReturn`. Las aserciones comprueban ID y nombre; los `verify` comprueban ambas llamadas.

## Advertencia importante: estado compartido

`Examen newExamen = Datos.EXAMEN` **no copia** el objeto. Ambas referencias apuntan al mismo examen estático. `setPreguntas(Datos.PREGUNTAS)` modifica ese objeto compartido, de modo que la primera prueba podría encontrar preguntas si se ejecuta después de la segunda. `@BeforeEach` que reinicia mocks no reinicia `Datos.EXAMEN`.

Para conservar la misma clase `Datos` y evitar contaminación entre pruebas, se puede crear una copia a partir de sus atributos:

```java
Examen nuevo = new Examen(Datos.EXAMEN.getId(), Datos.EXAMEN.getNombre());
// Solo para el test con preguntas:
nuevo.setPreguntas(Datos.PREGUNTAS);
```

Así ambos tests usan los valores de `Datos` pero trabajan con instancias independientes.

## Diferencia entre las verificaciones

- `verify(repository).guardar(any(Examen.class))`: confirma que se llamó una vez a `guardar` con un examen no nulo.
- `verify(preguntaRepository).guardarVarias(anyList())`: confirma una llamada con una lista no nula, pero no comprueba su contenido.
- `verify(preguntaRepository, never()).guardarVarias(anyList())`: confirma que no se llamó a ese método con una lista coincidente.
- `verifyNoInteractions(preguntaRepository)`: confirma que no hubo ninguna interacción con ese mock.

**Regla:** `when()` configura la respuesta; `service.guardar()` ejecuta la lógica real; `assert` valida el resultado y `verify()` comprueba las interacciones.


# 6. Test del ID incremental con Mockito: Answer e InvocationOnMock

## Objetivo
Simular que `ExamenRepository.guardar(Examen)` asigna IDs incrementales a los exámenes, sin utilizar una base de datos real. Se mantiene la clase `Datos` del curso:

```java
public class Datos {
    public static final List<Examen> EXAMENES = Arrays.asList(
        new Examen(5L, "Matematicas"),
        new Examen(6L, "Lenguaje"),
        new Examen(7L, "Historia")
    );
    public static final List<String> PREGUNTAS = Arrays.asList(
        "aritmetica", "integrales", "derivadas", "trigonometria", "geometria"
    );
    public static final Examen EXAMEN = new Examen(null, "Fisica");
}
```

## Código original explicado

```java
when(repository.guardar(any(Examen.class))).then(new Answer<Examen>() {
    Long secuencia = 8L;

    @Override
    public Examen answer(InvocationOnMock invocation) throws Throwable {
        Examen examen = invocation.getArgument(0);
        examen.setId(secuencia++);
        return examen;
    }
});
```

- `repository` es el mock de `ExamenRepository`.
- `guardar(Examen)` es el método del mock que configuramos.
- `any(Examen.class)` coincide con cualquier instancia no nula de `Examen`.
- `when(...)` comienza la configuración de la llamada simulada.
- `then(Answer<Examen>)` define una **respuesta dinámica**. A diferencia de `thenReturn(objeto)`, permite ejecutar código cada vez que se invoca el método.
- `Answer<Examen>` es una **interfaz genérica de Mockito**; el parámetro `Examen` determina el tipo de retorno de `answer()`.
- `new Answer<Examen>() { ... }` es una **clase anónima** que implementa esa interfaz.
- `Long secuencia = 8L` es un campo de esa instancia anónima; empieza en 8 y conserva su valor para las llamadas sucesivas que utilicen ese mismo `Answer`.
- `@Override` señala la implementación del método de la interfaz.
- `answer(InvocationOnMock invocation)` se ejecuta cuando el mock recibe una llamada coincidente. `InvocationOnMock` representa la invocación y permite consultar sus argumentos.
- `throws Throwable` es parte de la firma de `Answer.answer`; este ejemplo no lanza ninguna excepción.
- `invocation.getArgument(0)` extrae el **primer argumento** (los índices empiezan en cero) pasado a `repository.guardar(...)` y lo trata como `Examen`.
- `examen.setId(secuencia++)` asigna el ID actual y después incrementa `secuencia` (postincremento). Primera llamada: ID 8; segunda: 9; tercera: 10.
- `return examen` devuelve **el mismo objeto recibido**, ahora modificado, no una copia.

## Flujo de la prueba del curso

```java
@Test
@DisplayName("Guardar Examen con preguntas")
void testGuardarExamenConPeguntas() {
    // Given
    Examen newExamen = Datos.EXAMEN;
    newExamen.setPreguntas(Datos.PREGUNTAS);

    when(repository.guardar(any(Examen.class))).then(new Answer<Examen>() {
        Long secuencia = 8L;

        @Override
        public Examen answer(InvocationOnMock invocation) throws Throwable {
            Examen examen = invocation.getArgument(0);
            examen.setId(secuencia++);
            return examen;
        }
    });

    // When
    Examen examen = service.guardar(newExamen);

    // Then
    assertNotNull(examen.getId());
    assertEquals(8L, examen.getId());
    assertEquals("Fisica", examen.getNombre());
    verify(repository).guardar(any(Examen.class));
    verify(preguntaRepository).guardarVarias(anyList());
}
```

1. `Datos.EXAMEN` comienza con ID `null` y nombre `Fisica`.
2. `setPreguntas(Datos.PREGUNTAS)` agrega cinco preguntas al mismo objeto.
3. `service.guardar(newExamen)` invoca `preguntaRepository.guardarVarias(...)` porque hay preguntas; al ser un método `void` en un mock, no hace nada por defecto.
4. El servicio llama a `examenRepository.guardar(examen)`; Mockito ejecuta `answer()`.
5. `getArgument(0)` obtiene el examen recibido; `setId(8L)` asigna el ID; `return examen` lo devuelve.
6. JUnit verifica ID y nombre; Mockito verifica ambas llamadas.

**Limitación:** el test del curso solo guarda **una vez**. Comprueba que se asigna el ID 8, pero no comprueba que el contador incremente en llamadas sucesivas.

## Prueba adicional: comprobar el incremento real

```java
@Test
void testIdsIncrementales() {
    when(repository.guardar(any(Examen.class))).then(new Answer<Examen>() {
        long secuencia = 8L;

        @Override
        public Examen answer(InvocationOnMock invocation) {
            Examen examen = invocation.getArgument(0);
            examen.setId(secuencia++);
            return examen;
        }
    });

    Examen fisica = new Examen(null, Datos.EXAMEN.getNombre());
    Examen quimica = new Examen(null, "Quimica");
    Examen biologia = new Examen(null, "Biologia");

    assertEquals(8L, service.guardar(fisica).getId());
    assertEquals(9L, service.guardar(quimica).getId());
    assertEquals(10L, service.guardar(biologia).getId());

    verify(repository, times(3)).guardar(any(Examen.class));
    verifyNoInteractions(preguntaRepository);
}
```

Este test utiliza objetos nuevos con listas de preguntas vacías y demuestra el incremento dentro de **la misma configuración del mock**. No persiste datos entre pruebas.

## Forma corta con lambda

Como `Answer<T>` es una interfaz funcional, se puede escribir:

```java
AtomicLong secuencia = new AtomicLong(8L);
when(repository.guardar(any(Examen.class))).thenAnswer(invocation -> {
    Examen examen = invocation.getArgument(0);
    examen.setId(secuencia.getAndIncrement());
    return examen;
});
```

`thenAnswer(...)` es una forma expresiva de registrar la respuesta. `AtomicLong.getAndIncrement()` devuelve el valor actual y después lo incrementa. Aquí se usa para disponer de un contador mutable capturado por la lambda; no implica que el mock simule una base de datos real.

## Buenas prácticas

- **No reutilizar `Datos.EXAMEN` mutable entre pruebas:** es `static final`, pero el objeto puede modificarse mediante `setId()` y `setPreguntas()`. Para pruebas aisladas, crea `new Examen(null, Datos.EXAMEN.getNombre())`.
- `any(Examen.class)` no coincide con `null`; usa el matcher apropiado si necesitas probar argumentos nulos.
- `getArgument(0)` devuelve la referencia del argumento, por lo que `setId()` cambia el objeto que recibió el mock.
- `verify(repository).guardar(...)` comprueba una interacción, no que los datos se hayan persistido.
- Prueba por separado el primer ID y los incrementos posteriores.
- `Answer` sirve para respuestas dinámicas dependientes de argumentos o estado; para una respuesta fija suele bastar `thenReturn()`.

## Resumen rápido

| Elemento | Función |
|---|---|
| `when(...)` | Selecciona la llamada que queremos simular |
| `any(Examen.class)` | Coincide con un `Examen` no nulo |
| `then(new Answer<Examen>() {...})` | Registra comportamiento dinámico |
| `answer(InvocationOnMock)` | Se ejecuta por cada invocación coincidente |
| `getArgument(0)` | Recupera el primer argumento |
| `secuencia++` | Asigna el valor actual y después incrementa |
| `setId(...)` | Modifica el examen recibido |
| `return examen` | Devuelve el examen modificado |
| `verify(...)` | Verifica llamadas realizadas |


# 7. Comprobaciones de excepciones con `when()` y `thenThrow()`

## Objetivo
Aprender a simular excepciones en repositorios con Mockito y verificarlas con JUnit 5, utilizando las listas reales de la clase `Datos`.

## Clase `Datos`

```java
public class Datos {
    public static final List<Examen> EXAMENES = Arrays.asList(
        new Examen(5L, "Matematicas"),
        new Examen(6L, "Lenguaje"),
        new Examen(7L, "Historia")
    );

    public static final List<Examen> EXAMENES_ID_NULL = Arrays.asList(
        new Examen(null, "Matematicas"),
        new Examen(null, "Lenguaje"),
        new Examen(null, "Historia")
    );

    public static final List<String> PREGUNTAS = Arrays.asList(
        "aritmetica", "integrales", "derivadas", "trigonometria", "geometria"
    );

    public static final Examen EXAMEN = new Examen(null, "Fisica");
}
```

## `thenReturn()` frente a `thenThrow()`

```java
when(repository.findAll()).thenReturn(Datos.EXAMENES);
when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
    .thenThrow(IllegalArgumentException.class);
```

- `when(...)` configura el comportamiento de una llamada al **mock**.
- `thenReturn(valor)` hace que el mock devuelva un valor.
- `thenThrow(TipoExcepcion.class)` hace que el mock lance una excepción cuando la llamada coincida.
- `IllegalArgumentException` es una excepción de Java que suele indicar argumentos inválidos.
- `anyLong()` coincide con valores `long`/`Long` **no nulos**; no coincide con `null`.
- `isNull()` coincide con un argumento `null`.

## Prueba 1: examen con ID válido

```java
@Test
void testManejoException() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);
    when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
        .thenThrow(IllegalArgumentException.class);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.findExamenPorNombreConPreguntas("Matematicas")
    );

    assertEquals(IllegalArgumentException.class, exception.getClass());
    verify(repository).findAll();
    verify(preguntaRepository).findPreguntasPorExamenId(anyLong());
}
```

### Paso a paso

1. `repository.findAll()` devuelve `Datos.EXAMENES`.
2. El servicio encuentra Matemáticas, con ID `5L`.
3. El servicio llama a `preguntaRepository.findPreguntasPorExamenId(5L)`.
4. `anyLong()` coincide con `5L` y Mockito lanza `IllegalArgumentException`.
5. `assertThrows()` de JUnit 5 ejecuta la lambda, comprueba la excepción y devuelve la excepción capturada.
6. `verify()` confirma que los dos repositorios fueron consultados, incluso si el segundo lanzó una excepción.

La comprobación `assertEquals(IllegalArgumentException.class, exception.getClass())` es redundante si basta con comprobar que la excepción es de un tipo compatible, porque `assertThrows()` ya lo hizo. `assertThrowsExactly()` exige la clase exacta.

## Prueba 2: examen con ID nulo

**Importante:** el código original carecía de `@Test`; sin esa anotación JUnit no lo ejecuta como prueba.

```java
@Test
void testManejoExceptionNull() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES_ID_NULL);
    when(preguntaRepository.findPreguntasPorExamenId(isNull()))
        .thenThrow(IllegalArgumentException.class);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.findExamenPorNombreConPreguntas("Matematicas")
    );

    assertEquals(IllegalArgumentException.class, exception.getClass());
    verify(repository).findAll();
    verify(preguntaRepository).findPreguntasPorExamenId(isNull());
}
```

### Paso a paso

1. `repository.findAll()` devuelve exámenes cuyos IDs son `null`.
2. El servicio encuentra Matemáticas, cuyo `getId()` devuelve `null`.
3. El servicio llama a `findPreguntasPorExamenId(null)`.
4. `isNull()` coincide y Mockito lanza `IllegalArgumentException`.
5. JUnit comprueba la excepción; `verify(...isNull())` confirma el argumento recibido.

La excepción no procede de una validación explícita del servicio: se produce porque el mock fue configurado para lanzarla.

## Diferencia entre los comparadores

| Matcher | `5L` | `null` |
|---|---|---|
| `anyLong()` | Coincide | No coincide |
| `isNull()` | No coincide | Coincide |

## Consejos

- Para comprobar exactamente el ID consultado, utiliza `verify(preguntaRepository).findPreguntasPorExamenId(5L)` en la primera prueba.
- `thenThrow()` configura errores de métodos con valor de retorno. Para un método `void`, utiliza `doThrow(...).when(mock).metodo(...)`.
- `assertThrows()` pertenece a JUnit 5; `when()`, `thenThrow()`, `anyLong()`, `isNull()` y `verify()` pertenecen a Mockito.
- En estos tests el servicio **propaga** la excepción porque no la captura con `try/catch`; no está resolviendo ni transformando el error.

## Resumen

`when()` prepara el escenario, `thenThrow()` simula el fallo, `assertThrows()` verifica la excepción y `verify()` confirma las llamadas que ocurrieron antes del fallo.


# Argument Matchers en Mockito

## ¿Qué son?

Los **Argument Matchers** son herramientas de Mockito para indicar qué argumentos aceptamos al configurar un mock con `when(...)` o qué argumentos esperamos al comprobar una llamada con `verify(...)`.

**Analogía:** el mock es un empleado ficticio. `when(...).thenReturn(...)` le dice qué responder si recibe cierta solicitud; `verify(...)` comprueba qué solicitud recibió. Los matchers son las reglas para reconocerla: «cualquier número», «exactamente 5», «cualquier número mayor o igual a 5».

## `anyLong()` al configurar una respuesta

```java
when(repository.findAll()).thenReturn(Datos.EXAMENES);
when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
    .thenReturn(Datos.PREGUNTAS);
```

- `repository` y `preguntaRepository` son **mocks**: no consultan una base de datos.
- `findAll()` devolverá la lista de exámenes creada en memoria.
- `anyLong()` acepta cualquier valor de tipo `long` o `Long` **no nulo**; no significa que genere un ID.
- `thenReturn(Datos.PREGUNTAS)` especifica la lista que devolverá el mock cuando se llame al método con un argumento compatible.

## Ejecutar el servicio real

```java
service.findExamenPorNombreConPreguntas("Matematicas");
```

`service`, creado mediante `@InjectMocks`, es el objeto real que ejecuta su lógica. Supongamos que localiza el examen «Matematicas» con ID `5L` y llama a `preguntaRepository.findPreguntasPorExamenId(5L)`. Mockito no ejecuta la consulta del repositorio; devuelve `Datos.PREGUNTAS`.

## `verify(...)` y `argThat(...)`

```java
verify(repository).findAll();
verify(preguntaRepository)
    .findPreguntasPorExamenId(argThat(arg -> arg != null && arg >= 5L));
```

- `verify(repository).findAll()` exige que `findAll()` se haya invocado exactamente una vez (es el valor predeterminado).
- `argThat(...)` permite crear una **condición personalizada** sobre el argumento usado en la llamada real.
- `arg -> arg != null && arg >= 5L` es una **expresión lambda** que devuelve `true` si el argumento no es nulo y es al menos 5.
- Este `verify` acepta `5L`, `6L`, etc. Si el objetivo es comprobar el ID exacto, conviene usar `verify(preguntaRepository).findPreguntasPorExamenId(5L);` o `eq(5L)`.

## Matcher con una clase anidada

```java
@Test
void testArgumentMatchersClaseAnidada() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);
    when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
        .thenReturn(Datos.PREGUNTAS);

    service.findExamenPorNombreConPreguntas("Matematicas");

    verify(repository).findAll();
    verify(preguntaRepository)
        .findPreguntasPorExamenId(argThat(new MiArgsMatchers()));
}

public static class MiArgsMatchers implements ArgumentMatcher<Long> {
    @Override
    public boolean matches(Long argument) {
        return argument != null && argument > 0;
    }

    @Override
    public String toString() {
        return "se esperaba un ID de examen positivo";
    }
}
```

- `ArgumentMatcher<Long>` es una **interfaz de Mockito** para crear reglas reutilizables que reciben un `Long`.
- `matches(Long argument)` es el **método de la interfaz** que decide si un argumento coincide (`true`) o no (`false`).
- `toString()` es un método heredado de `Object` que se sobrescribe para dar una descripción legible del matcher en determinados mensajes de error de Mockito.
- `MiArgsMatchers` es una **clase anidada estática con nombre**, no una clase anónima. La lambda del ejemplo anterior tampoco es una clase anónima en el código fuente.
- No hace falta guardar el argumento en un atributo de la clase para mostrar el requisito. Una descripción fija evita depender de cuándo Mockito llama a `matches()`.

## Comparación rápida

| Matcher | Significado |
|---|---|
| `anyLong()` | Cualquier `long` o `Long` no nulo |
| `eq(5L)` | Exactamente `5L` |
| `argThat(id -> id != null && id >= 5L)` | ID no nulo de al menos 5 |
| `argThat(new MiArgsMatchers())` | Cumple la regla definida en `matches()` |

**Atención:** `anyLong()` se usa arriba para **preparar** la respuesta del mock, mientras que `argThat(...)` se usa en `verify(...)` para **comprobar** qué argumento pasó realmente el servicio. Ambos pueden utilizarse en configuración o verificación.

Si en una invocación a Mockito usas matchers para algunos argumentos, usa matchers para **todos** los argumentos de esa invocación (por ejemplo, `metodo(eq(5L), anyString())`).

## Ejemplo de prueba más precisa

```java
@Test
void debeBuscarPreguntasDelExamenMatematicas() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);
    when(preguntaRepository.findPreguntasPorExamenId(5L))
        .thenReturn(Datos.PREGUNTAS);

    service.findExamenPorNombreConPreguntas("Matematicas");

    verify(repository).findAll();
    verify(preguntaRepository).findPreguntasPorExamenId(5L);
}
```

Esta prueba verifica el ID esperado de forma exacta, en lugar de aceptar cualquier ID positivo. Podrías añadir `assertEquals(...)` u otras assertions sobre las preguntas devueltas por el servicio.


# Capturando argumentos con ArgumentCaptor en Mockito

## ¿Qué es `ArgumentCaptor`?

`ArgumentCaptor<T>` es una clase de Mockito que permite **capturar el argumento que el código real envió a un método de un mock** y comprobarlo después mediante aserciones de JUnit 5.

**Analogía:** imagina que el servicio entrega un sobre con el ID de un examen al repositorio. `ArgumentCaptor` toma una copia del número escrito en el sobre para que puedas revisarlo después.

No simula una base de datos ni intercepta directamente un método real: captura el argumento de una **interacción registrada con un mock**, normalmente cuando se utiliza junto con `verify()`.

## Tu ejemplo paso a paso

```java
@Test
void testArgumentCapture() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);

    service.findExamenPorNombreConPreguntas("Matematicas");

    ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
    verify(preguntaRepository).findPreguntasPorExamenId(captor.capture());

    assertEquals(5L, captor.getValue());
}
```

1. `when(repository.findAll()).thenReturn(Datos.EXAMENES)` prepara la lista de exámenes que devolverá el primer mock.
2. `service.findExamenPorNombreConPreguntas("Matematicas")` ejecuta el **servicio real**. Si encuentra Matemáticas con ID `5L`, llama a `preguntaRepository.findPreguntasPorExamenId(5L)`.
3. `ArgumentCaptor.forClass(Long.class)` crea un capturador de argumentos `Long`.
4. `verify(preguntaRepository).findPreguntasPorExamenId(captor.capture())` comprueba que se invocó el método una vez y captura el ID enviado.
5. `captor.getValue()` recupera el argumento capturado; `assertEquals(5L, ...)` valida que el servicio haya utilizado exactamente el ID esperado.

**Nota:** la prueba supone que el servicio efectivamente llama al repositorio de preguntas. Si no lo hace, la verificación falla.

## ¿Por qué funciona sin el `thenReturn()` del repositorio de preguntas?

En tu prueba comentaste:

```java
// when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
//     .thenReturn(Datos.PREGUNTAS);
```

Para un mock normal de Mockito, una llamada no configurada que devuelve una `List` suele devolver una **lista vacía**, no `Datos.PREGUNTAS`. El servicio puede seguir ejecutándose si soporta esa respuesta. Aquí estás comprobando **qué ID envía el servicio**, no que se hayan cargado correctamente las preguntas. Si deseas verificar las preguntas del examen, también deberías configurar y comprobar la respuesta.

## Alternativa con `@Captor`

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {

    @Mock
    ExamenRepository repository;

    @Mock
    PreguntaRepository preguntaRepository;

    @InjectMocks
    ExamenServiceImpl service;

    @Captor
    ArgumentCaptor<Long> captor;

    @Test
    void testArgumentCapture() {
        when(repository.findAll()).thenReturn(Datos.EXAMENES);

        service.findExamenPorNombreConPreguntas("Matematicas");

        verify(preguntaRepository)
            .findPreguntasPorExamenId(captor.capture());

        assertEquals(5L, captor.getValue());
    }
}
```

`@Captor` hace que Mockito inicialice el capturador automáticamente mediante `MockitoExtension`. Es una alternativa a `ArgumentCaptor.forClass(Long.class)`; no necesitas ambas formas para el mismo capturador.

## `ArgumentCaptor` vs. `argThat()`

```java
// Verifica directamente una condición sobre el argumento.
verify(preguntaRepository)
    .findPreguntasPorExamenId(argThat(id -> id != null && id > 0));

// Captura el valor para inspeccionarlo con aserciones.
verify(preguntaRepository)
    .findPreguntasPorExamenId(captor.capture());
assertEquals(5L, captor.getValue());
```

- **`argThat()`**: cuando basta con decidir si un argumento cumple una condición.
- **`ArgumentCaptor`**: cuando necesitas inspeccionar el valor recibido o validar varias propiedades del objeto.
- **`eq(5L)` o `5L`**: cuando simplemente quieres comprobar un valor exacto; suele ser lo más sencillo.

## Métodos y anotaciones utilizados

| Elemento | Función |
|---|---|
| `ArgumentCaptor<T>` | Capturador de argumentos del tipo `T`. |
| `ArgumentCaptor.forClass(Long.class)` | Crea un capturador de `Long`. |
| `@Captor` | Inicializa automáticamente un campo capturador con Mockito. |
| `captor.capture()` | Indica qué argumento se captura durante `verify()`. |
| `captor.getValue()` | Recupera el último argumento capturado. |
| `captor.getAllValues()` | Recupera todos los argumentos capturados, útil para varias invocaciones. |
| `verify(mock)` | Verifica una interacción, una vez por defecto. |
| `assertEquals(esperado, real)` | Aserción de JUnit 5 para comparar el valor esperado con el recibido. |

## Conclusión

`ArgumentCaptor` permite confirmar **qué dato salió del servicio y llegó al repositorio simulado**. En este ejemplo, la prueba demuestra que al buscar "Matematicas" el servicio pidió preguntas con el ID `5L`, siempre que la llamada haya ocurrido como se esperaba.


# doThrow para comprobar excepciones en métodos void

## ¿Qué es `doThrow()`?

`doThrow()` es una herramienta de Mockito que permite **configurar un mock para que lance una excepción** cuando se invoque determinado método. Resulta especialmente útil para métodos `void`, ya que estos no devuelven un valor que podamos usar dentro de `when(...).thenThrow(...)`.

**Analogía:** imagina que tu servicio intenta guardar preguntas en un almacén, pero el encargado (repositorio simulado) tiene instrucciones de lanzar un error cuando se le pida guardar la lista. Así compruebas cómo reacciona tu servicio ante el fallo.

## Ejemplo de la prueba

```java
@Test
void testDoThrow() {
    Examen examen = Datos.EXAMEN;
    examen.setPreguntas(Datos.PREGUNTAS);

    doThrow(IllegalArgumentException.class)
        .when(preguntaRepository)
        .guardarVarias(anyList());

    assertThrows(IllegalArgumentException.class, () -> {
        service.guardar(examen);
    });
}
```

### Paso a paso

1. `Examen examen = Datos.EXAMEN;` obtiene el examen de prueba.
2. `examen.setPreguntas(Datos.PREGUNTAS);` le asigna una lista de preguntas.
3. `doThrow(IllegalArgumentException.class)` configura la excepción que lanzará el mock.
4. `.when(preguntaRepository).guardarVarias(anyList())` indica que la excepción aparecerá cuando se invoque `guardarVarias(...)` con una lista no nula.
5. `service.guardar(examen)` ejecuta el método **real** del servicio, que debe llegar a llamar a ese repositorio.
6. `assertThrows(...)` comprueba que la ejecución de la lambda lance `IllegalArgumentException` (o una subclase).

**Importante:** Mockito no invoca `guardarVarias(...)` durante la configuración. La excepción se dispara después, si la ejecución real llega a llamar al mock con un argumento que coincide. Si el servicio captura esa excepción y no la vuelve a lanzar, `assertThrows` fallará.

## ¿Por qué no usar `when().thenThrow()` en `void`?

Esto no compila si `guardarVarias` devuelve `void`:

```java
// Incorrecto para un método void:
when(preguntaRepository.guardarVarias(anyList()))
    .thenThrow(IllegalArgumentException.class);
```

`when(...)` requiere una expresión con valor, pero un método `void` no produce ninguno. Para ello se utiliza:

```java
doThrow(IllegalArgumentException.class)
    .when(preguntaRepository)
    .guardarVarias(anyList());
```

## Diferencia entre Mockito y JUnit 5

| Herramienta | Función |
| --- | --- |
| `doThrow(...)` (Mockito) | Prepara el mock para **lanzar** la excepción. |
| `when(preguntaRepository)` (Mockito) | Selecciona el mock cuyo comportamiento vamos a configurar. |
| `anyList()` (Mockito) | Hace coincidir cualquier lista no nula; no restringe sus elementos. |
| `assertThrows(...)` (JUnit 5) | Comprueba que el código probado **lance** la excepción. |

## Versión mejorada

```java
@Test
void testDoThrow() {
    Examen examen = Datos.EXAMEN;
    examen.setPreguntas(Datos.PREGUNTAS);

    doThrow(new IllegalArgumentException("Error al guardar preguntas"))
        .when(preguntaRepository)
        .guardarVarias(anyList());

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> service.guardar(examen)
    );

    assertEquals("Error al guardar preguntas", error.getMessage());
    verify(preguntaRepository).guardarVarias(anyList());
}
```

La verificación adicional confirma que el mock recibió la llamada (si la ejecución llega hasta ella). La aserción del mensaje comprueba que la excepción observada tenga el mensaje configurado; este ejemplo presupone que el servicio no cambia esa excepción.

## Nota sobre `Datos.EXAMEN`

Si `Datos.EXAMEN` es un objeto estático compartido entre pruebas, al llamar `setPreguntas(...)` se modifica ese mismo objeto. Conviene construir un examen nuevo para cada prueba para evitar interferencias.

## Resumen

- `doThrow(...).when(mock).metodoVoid(...)` configura una excepción en un método `void` del mock.
- `assertThrows(...)` verifica que esa excepción llegue al código que ejecuta la prueba.
- `anyList()` coincide con una lista no nula, sin importar su contenido.
- El servicio es real; lo que simula Mockito es la dependencia (`preguntaRepository`).
- Si el método del servicio nunca llama al método configurado, la excepción no se producirá por esa configuración.
