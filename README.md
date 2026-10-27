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


# Usando doAnswer() en Mockito

## ¿Qué es?

`doAnswer()` permite configurar una **respuesta dinámica** para un método de un mock. A diferencia de `thenReturn(valor)`, que devuelve un valor prefijado, con `doAnswer()` podemos leer los argumentos, ejecutar una condición, modificar un objeto o generar valores.

**Analogía:** `thenReturn()` es una máquina que siempre entrega el mismo producto; `doAnswer()` observa el botón pulsado y decide qué entregar.

## Ejemplo 1: responder según el ID

```java
@Test
void testDoAnswer() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);

    doAnswer(invocation -> {
        Long id = invocation.getArgument(0);
        return id == 5L ? Datos.PREGUNTAS : null;
    }).when(preguntaRepository).findPreguntasPorExamenId(anyLong());

    Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

    assertEquals(5L, examen.getId());
    assertEquals("Matematicas", examen.getNombre());
    assertEquals(5, examen.getPreguntas().size());
    assertTrue(examen.getPreguntas().contains("geometria"));

    verify(preguntaRepository).findPreguntasPorExamenId(5L);
}
```

### Explicación

- `when(repository.findAll()).thenReturn(Datos.EXAMENES)` simula la lista de exámenes.
- `doAnswer(invocation -> {...})` define una respuesta personalizada para el repositorio de preguntas.
- `InvocationOnMock` es la interfaz que representa la llamada interceptada; `getArgument(0)` lee su primer argumento.
- `id == 5L ? Datos.PREGUNTAS : null` devuelve las preguntas solo para el examen 5. Como el matcher es `anyLong()`, la configuración aplica a cualquier `long` o `Long` no nulo.
- El **servicio real** se ejecuta y recibe las respuestas de los mocks.
- `assertEquals()` y `assertTrue()` son aserciones de JUnit 5.
- `verify(...).findPreguntasPorExamenId(5L)` comprueba que se usó exactamente el ID esperado. `anyLong()` solo garantizaría que se pasó algún ID no nulo.

> Nota: devolver `null` puede ser peligroso si el servicio espera una lista; para muchas pruebas es mejor `Collections.emptyList()`.

## Ejemplo 2: simular un ID autoincremental

```java
@Test
@DisplayName("Guardar Examen con preguntas")
void testDoAnswerGuardarExamenConPreguntas() {
    Examen nuevo = new Examen(null, "Fisica");
    nuevo.setPreguntas(Datos.PREGUNTAS);

    doAnswer(new Answer<Examen>() {
        long secuencia = 8L;

        @Override
        public Examen answer(InvocationOnMock invocation) throws Throwable {
            Examen examen = invocation.getArgument(0);
            examen.setId(secuencia++);
            return examen;
        }
    }).when(repository).guardar(any(Examen.class));

    Examen guardado = service.guardar(nuevo);

    assertNotNull(guardado.getId());
    assertEquals(8L, guardado.getId());
    assertEquals("Fisica", guardado.getNombre());
    verify(repository).guardar(any(Examen.class));
    verify(preguntaRepository).guardarVarias(anyList());
}
```

> Ajusta `new Examen(null, "Fisica")` al constructor real de tu clase. Crear un objeto nuevo evita modificar un `Datos.EXAMEN` compartido entre pruebas.

### ¿Qué hace cada parte?

- `Answer<Examen>` es una **interfaz funcional** de Mockito: establece el comportamiento del mock y el tipo de respuesta (`Examen`).
- `new Answer<Examen>() { ... }` crea una **clase anónima**, que implementa `answer(InvocationOnMock invocation)`.
- `InvocationOnMock.getArgument(0)` obtiene el examen pasado a `guardar()`.
- `secuencia = 8L` define el ID inicial **para esa instancia de Answer**.
- `examen.setId(secuencia++)` asigna 8 en la primera llamada, 9 en la segunda, etc.
- `return examen` devuelve el **mismo objeto modificado**, sin almacenar nada en una BD real.
- `verify()` confirma que se invocaron los dos repositorios, no que se hayan persistido datos.

### Versión más corta con lambda

```java
AtomicLong secuencia = new AtomicLong(8L);

doAnswer(invocation -> {
    Examen examen = invocation.getArgument(0);
    examen.setId(secuencia.getAndIncrement());
    return examen;
}).when(repository).guardar(any(Examen.class));
```

`AtomicLong` pertenece a `java.util.concurrent.atomic`. Aquí se usa para poder actualizar la secuencia desde la lambda; en un test sencillo no implica que estemos haciendo operaciones concurrentes.

## Comparación rápida

| Método | ¿Para qué sirve? |
|---|---|
| `thenReturn(valor)` | Devolver un valor fijo |
| `thenThrow(excepcion)` | Lanzar una excepción |
| `doThrow(excepcion)` | Lanzar una excepción; útil con métodos `void` |
| `doAnswer(respuesta)` | Ejecutar lógica personalizada y decidir qué devolver |
| `getArgument(0)` | Leer el primer argumento de la llamada al mock |
| `verify(mock)` | Comprobar interacciones con el mock |

`doAnswer()` **no ejecuta el método real del repositorio**: sustituye su comportamiento. La lógica que sí queremos probar es la del servicio.

## ¿Cuándo conviene usarlo?

Úsalo cuando la respuesta depende de los parámetros, cuando necesitas mutar un argumento o simular una secuencia de identificadores. Si el resultado siempre es fijo, `thenReturn()` suele ser más claro.


# Usando doCallRealMethod() en Mockito

## ¿Qué es?

`doCallRealMethod()` permite indicar que **una llamada concreta a un método de un mock ejecute su implementación real**, en lugar de devolver la respuesta predeterminada del mock.

Esto no convierte todo el mock en un objeto real: los demás métodos seguirán simulados, salvo que se configuren de otra forma.

## ¿Por qué necesitas una implementación concreta?

Una interfaz como `PreguntaRepository` solo declara el contrato:

```java
public interface PreguntaRepository {
    List<String> findPreguntasPorExamenId(Long id);
    void guardarVarias(List<String> preguntas);
}
```

La clase `PreguntasRepositoryImpl` sí tiene código ejecutable:

```java
public class PreguntasRepositoryImpl implements PreguntaRepository {
    @Override
    public List<String> findPreguntasPorExamenId(Long id) {
        System.out.println("PreguntasRepositoryImpl.findPreguntasPorExamenId");
        return Datos.PREGUNTAS;
    }

    @Override
    public void guardarVarias(List<String> preguntas) {
        System.out.println("PreguntasRepositoryImpl.guardarVarias");
    }
}
```

**Importante:** un mock de una interfaz no tiene una implementación concreta de ese método que Mockito pueda invocar. Por eso, para este ejemplo, se crea un mock de `PreguntasRepositoryImpl`.

## Preparación de la prueba

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {

    @Mock
    ExamenRepositoryImpl repository;

    @Mock
    PreguntasRepositoryImpl preguntaRepository;

    @InjectMocks
    ExamenServiceImpl service;

    @Test
    void testDoCallRealMethod() {
        when(repository.findAll()).thenReturn(Datos.EXAMENES);

        doCallRealMethod()
            .when(preguntaRepository)
            .findPreguntasPorExamenId(anyLong());

        Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

        assertEquals(5L, examen.getId());
        assertEquals("Matematicas", examen.getNombre());
        assertEquals(Datos.PREGUNTAS, examen.getPreguntas());

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(5L);
    }
}
```

Este ejemplo supone que el servicio inyecta ambos repositorios y coloca en el examen la lista devuelta por `findPreguntasPorExamenId`.

### Paso a paso

1. `@Mock ExamenRepositoryImpl repository`: crea un mock de la clase concreta. **No** se ejecuta automáticamente su `findAll()` real.
2. `when(repository.findAll()).thenReturn(Datos.EXAMENES)`: configura la respuesta simulada del primer repositorio.
3. `@Mock PreguntasRepositoryImpl preguntaRepository`: crea otro mock, también de una clase concreta.
4. `doCallRealMethod().when(preguntaRepository).findPreguntasPorExamenId(anyLong())`: permite ejecutar el cuerpo real de **este método específico** cuando reciba un `Long` no nulo.
5. `service.findExamenPorNombreConPreguntas("Matematicas")`: ejecuta el método real del servicio; el servicio consulta los repositorios inyectados.
6. La llamada `repository.findAll()` devuelve `Datos.EXAMENES` por el `thenReturn()`; **no imprime** el mensaje del `ExamenRepositoryImpl.findAll()` real.
7. La llamada `preguntaRepository.findPreguntasPorExamenId(5L)` ejecuta el método real de `PreguntasRepositoryImpl`, imprime el mensaje y devuelve `Datos.PREGUNTAS`.
8. `assertEquals` comprueba el resultado y `verify` comprueba las interacciones con los mocks.

## ¿Qué función cumple cada elemento?

| Elemento | Función |
|---|---|
| `@ExtendWith(MockitoExtension.class)` | Integra Mockito con el ciclo de vida de JUnit 5. |
| `@Mock` | Crea un objeto simulado, incluso cuando el tipo declarado es una clase concreta. |
| `@InjectMocks` | Crea el servicio real e intenta inyectarle sus mocks. |
| `doCallRealMethod()` | Ejecuta el método real seleccionado en el mock. |
| `when(...).thenReturn(...)` | Configura una respuesta simulada. |
| `anyLong()` | Coincide con cualquier valor `long` / `Long` no nulo. |
| `assertEquals()` | Comprueba valores con JUnit 5. |
| `verify()` | Verifica una llamada al mock. |

## Diferencia entre @Mock, doCallRealMethod() y @Spy

- `@Mock`: por defecto no ejecuta los métodos reales.
- `@Mock` + `doCallRealMethod()`: ejecuta **solo los métodos configurados** para la llamada real.
- `@Spy`: por defecto llama a los métodos reales del objeto espiado, salvo aquellos que se configuren para simular otra respuesta.

### Precaución importante

Un **mock de una clase** no es lo mismo que una **instancia real inicializada**. Si la implementación real usa campos, repositorios o dependencias internas, `doCallRealMethod()` puede provocar errores porque esos campos podrían no estar inicializados en el mock.

En tu ejemplo funciona porque el método real mostrado únicamente imprime un mensaje y devuelve `Datos.PREGUNTAS`: no necesita estado interno.

## ¿Por qué mover Datos a main?

Tu `PreguntasRepositoryImpl`, ubicada en `src/main/java`, importa `Datos`. Por tanto, `Datos` debe estar disponible para el código de producción si esa implementación depende de ella: una clase bajo `src/test/java` normalmente no está disponible para compilar `src/main/java`.

**Pero no es una buena práctica mover datos ficticios de prueba a `main` solamente para que un test funcione.** En proyectos reales es preferible que los datos de prueba permanezcan en `src/test/java`, y que las implementaciones de producción utilicen sus fuentes de datos reales. Esta implementación sirve como ejemplo didáctico.

## Ejemplo práctico: cuándo usarlo

Puede ser útil para verificar un método real y simular los demás de una clase, pero usa esta técnica con cuidado: las llamadas parciales a métodos reales pueden acoplar la prueba a detalles internos. Normalmente es más claro probar servicios reales con colaboradores mockeados o emplear un `@Spy` sobre un objeto completamente construido cuando el objetivo sea observar comportamiento parcial.

## Resumen

**`doCallRealMethod()` le dice a un mock: «Para esta llamada, ejecuta el código real de este método».** Requiere un método con implementación real; no hace real al repositorio completo ni crea una base de datos.


# Implementando espías con `@Spy` y `doReturn()` en Mockito

## ¿Qué es un Spy?

Un **Spy** de Mockito ejecuta los métodos reales por defecto, salvo que configuremos su comportamiento. Un **Mock**, en cambio, responde por defecto con valores de Mockito y no ejecuta la implementación real.

**Analogía:** un mock es una calculadora de utilería; un spy es una calculadora funcional a la que podemos cambiarle el resultado de una operación específica.

## Creación manual con `spy()`

```java
@Test
void testSpy() {
    ExamenRepository examenRepository = spy(ExamenRepositoryImpl.class);
    PreguntaRepository preguntaRepository = spy(PreguntasRepositoryImpl.class);
    ExamenService examenService = new ExamenServiceImpl(examenRepository, preguntaRepository);

    List<String> preguntas = Arrays.asList("aritmetica");
    doReturn(preguntas).when(preguntaRepository).findPreguntasPorExamenId(anyLong());

    Examen examen = examenService.findExamenPorNombreConPreguntas("Matematicas");

    assertEquals(5L, examen.getId());
    assertEquals("Matematicas", examen.getNombre());
    assertEquals(1, examen.getPreguntas().size());
    assertTrue(examen.getPreguntas().contains("aritmetica"));

    verify(examenRepository).findAll();
    verify(preguntaRepository).findPreguntasPorExamenId(5L);
}
```

1. `spy(ExamenRepositoryImpl.class)` crea un espía de la implementación concreta (Mockito debe poder construir la instancia). También existe `spy(new ExamenRepositoryImpl())`.
2. `new ExamenServiceImpl(...)` crea el **servicio real** con los espías inyectados.
3. `examenRepository.findAll()` ejecuta su implementación real y devuelve `Datos.EXAMENES`.
4. `doReturn(preguntas).when(preguntaRepository).findPreguntasPorExamenId(anyLong())` reemplaza **solo ese método**, que devolverá `["aritmetica"]` para los argumentos que coincidan.
5. Las aserciones de JUnit comprueban el examen y sus preguntas; `verify()` comprueba las interacciones. Usar `5L` en vez de `anyLong()` permite comprobar el ID exacto.

## ¿Por qué `doReturn()` en lugar de `when().thenReturn()`?

En un espía, configurar:

```java
when(preguntaRepository.findPreguntasPorExamenId(anyLong()))
    .thenReturn(preguntas);
```

**Puede ejecutar el método real en el momento de configurar el stubbing.** Esto importa si ese método hace consultas, modifica estado o falla con determinados argumentos. Por ejemplo, al usar `anyLong()` durante la configuración, la llamada puede pasar `0L` al método real.

Con:

```java
doReturn(preguntas)
    .when(preguntaRepository)
    .findPreguntasPorExamenId(anyLong());
```

no ejecutamos ese método real durante la configuración.

### Ejemplo de error evitado

```java
class Calculadora {
    int dividir(int a, int b) { return a / b; }
}
Calculadora calculadora = spy(new Calculadora());

// Ejecutaría el método real y lanzaría ArithmeticException:
// when(calculadora.dividir(10, 0)).thenReturn(5);

// Configura el retorno sin ejecutar la división:
doReturn(5).when(calculadora).dividir(10, 0);
assertEquals(5, calculadora.dividir(10, 0));
```

## Con anotaciones `@Spy` y `@InjectMocks`

```java
@ExtendWith(MockitoExtension.class)
class ExamenServiceImplSpyTest {

    @Spy
    ExamenRepositoryImpl repository;

    @Spy
    PreguntasRepositoryImpl preguntaRepository;

    @InjectMocks
    ExamenServiceImpl service;

    @Test
    void testSpyConAnotaciones() {
        List<String> preguntas = Arrays.asList("aritmetica");

        doReturn(preguntas).when(preguntaRepository)
            .findPreguntasPorExamenId(anyLong());

        Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

        assertEquals(5L, examen.getId());
        assertEquals("Matematicas", examen.getNombre());
        assertEquals(1, examen.getPreguntas().size());
        assertTrue(examen.getPreguntas().contains("aritmetica"));

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(5L);
    }
}
```

- **`@ExtendWith(MockitoExtension.class)`** integra Mockito con JUnit 5 e inicializa las anotaciones.
- **`@Spy`** crea un espía que llama a métodos reales por defecto.
- **`@InjectMocks`** crea el servicio real e intenta inyectarle mocks o spies. No lo convierte en mock.
- **`doReturn()`** redefine el valor que devuelve un método del spy sin ejecutarlo al configurarlo.
- **`verify()`** comprueba llamadas efectuadas al objeto espiado.

## Comparación rápida

| Operación | Mock | Spy |
|---|---|---|
| Comportamiento por defecto | Respuestas predeterminadas de Mockito | Métodos reales |
| Modificar un método | `when/thenReturn`, `doReturn`, etc. | `doReturn`, `doThrow`, etc. |
| Crear | `mock()`, `@Mock` | `spy()`, `@Spy` |
| Ejecutar método real | `doCallRealMethod()` si es posible | Por defecto |

## Consejos

- Para **pruebas unitarias de servicios**, suele ser preferible simular las dependencias con `@Mock`, porque así se aísla mejor la lógica del servicio.
- Usa spies cuando tengas una razón concreta para conservar parte del comportamiento real y modificar otra parte.
- Un spy **no significa una base de datos real**: en este ejemplo, las implementaciones devuelven datos Java de la clase `Datos`.
- En las pruebas, utiliza `5L` para comparar con un ID `Long` y verifica la llamada con el ID exacto cuando eso forme parte del comportamiento que deseas asegurar.

**Idea clave:** `@Spy` ejecuta los métodos reales por defecto; `doReturn()` sustituye el resultado de un método seleccionado sin ejecutarlo al configurar la prueba.


# Verificando el orden de las invocaciones con InOrder en Mockito

## ¿Qué es InOrder?

`InOrder` es una interfaz de Mockito que permite verificar que determinadas llamadas a uno o varios mocks ocurrieron en el orden esperado. A diferencia de `verify()`, no solo comprobamos que una interacción existió, sino también su secuencia.

**Analogía:** en una cafetería, preparar el café y entregarlo son dos acciones necesarias; `verify()` confirma que ambas ocurrieron, mientras `InOrder` permite comprobar que primero se preparó y luego se entregó.

## Primera prueba: orden dentro de un mock

```java
@Test
void testOrdenDeInvocaciones() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);

    service.findExamenPorNombreConPreguntas("Matematicas");
    service.findExamenPorNombreConPreguntas("Lenguaje");

    InOrder inOrder = inOrder(preguntaRepository);
    inOrder.verify(preguntaRepository).findPreguntasPorExamenId(5L);
    inOrder.verify(preguntaRepository).findPreguntasPorExamenId(6L);
}
```

1. `when(repository.findAll()).thenReturn(Datos.EXAMENES)` prepara la lista de exámenes del repositorio simulado.
2. La primera llamada al servicio busca Matemáticas (ID `5L`) y solicita sus preguntas.
3. La segunda llamada busca Lenguaje (ID `6L`) y solicita sus preguntas.
4. `inOrder(preguntaRepository)` crea un verificador del orden de las interacciones con ese mock.
5. Los `inOrder.verify(...)` comprueban que se solicitó primero el ID `5L` y después el `6L`.

La secuencia esperada es:

```text
preguntaRepository.findPreguntasPorExamenId(5L)
preguntaRepository.findPreguntasPorExamenId(6L)
```

Esto no comprueba explícitamente las llamadas a `repository.findAll()` porque ese mock no fue incluido en `inOrder(...)`.

## Segunda prueba: orden entre dos mocks

```java
@Test
void testOrdenDeInvocaciones2() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);

    service.findExamenPorNombreConPreguntas("Matematicas");
    service.findExamenPorNombreConPreguntas("Lenguaje");

    InOrder inOrder = inOrder(repository, preguntaRepository);
    inOrder.verify(repository).findAll();
    inOrder.verify(preguntaRepository).findPreguntasPorExamenId(5L);
    inOrder.verify(repository).findAll();
    inOrder.verify(preguntaRepository).findPreguntasPorExamenId(6L);
}
```

Aquí se verifica el orden de llamadas *entre ambos* repositorios:

```text
1. repository.findAll()
2. preguntaRepository.findPreguntasPorExamenId(5L)
3. repository.findAll()
4. preguntaRepository.findPreguntasPorExamenId(6L)
```

Así se puede comprobar que cada búsqueda del servicio consulta primero los exámenes y después las preguntas correspondientes.

## Diferencia entre verify() e InOrder

```java
verify(preguntaRepository).findPreguntasPorExamenId(5L);
verify(preguntaRepository).findPreguntasPorExamenId(6L);
```

Estas verificaciones comprueban que las llamadas ocurrieron, pero no establecen cuál fue primero.

```java
InOrder orden = inOrder(preguntaRepository);
orden.verify(preguntaRepository).findPreguntasPorExamenId(5L);
orden.verify(preguntaRepository).findPreguntasPorExamenId(6L);
```

Ahora verificamos la secuencia.

## ¿Qué ocurre si hay llamadas extra?

Por defecto, `InOrder` comprueba que puede encontrar las llamadas verificadas en la secuencia indicada; no exige automáticamente que sean las únicas interacciones con los mocks. Si necesitamos verificar que no quedaron interacciones adicionales sin verificar en el orden, podemos usar:

```java
inOrder.verifyNoMoreInteractions();
```

Esto es distinto de simplemente verificar dos llamadas en orden. Úsalo solo cuando sea un requisito real de la prueba.

## Métodos e interfaces empleados

| Elemento | Función |
|---|---|
| `InOrder` | Interfaz de Mockito para verificar secuencias de invocaciones. |
| `inOrder(mock1, mock2)` | Crea un verificador para uno o varios mocks. |
| `inOrder.verify(mock)` | Comprueba una invocación en el orden esperado. |
| `verify(mock)` | Comprueba una interacción, sin exigir orden relativo. |
| `inOrder.verifyNoMoreInteractions()` | Comprueba que no quedan interacciones sin verificar después de la última verificada en la secuencia. |
| `when(...).thenReturn(...)` | Configura la respuesta de un mock. |

## Recomendación

Usa `InOrder` cuando el **orden sea un requisito del comportamiento**, como validar antes de guardar, cobrar antes de confirmar o consultar un registro antes de procesarlo. Si el orden no es importante para el resultado esperado, `verify()` suele ser más sencillo y hace la prueba menos frágil.

**Idea clave:** `verify()` pregunta «¿se llamó este método?»; `InOrder` pregunta «¿se llamó en esta secuencia?».

# Verificando el número de invocaciones de los mocks en Mockito

## ¿Para qué sirve?

Mockito permite comprobar **cuántas veces se llamó a un método de un mock** mediante `verify(mock, modo)`. Esto sirve para detectar llamadas duplicadas, llamadas faltantes o interacciones que nunca deberían suceder.

## Ejemplo

```java
@Test
void testNumeroDeInvocaciones() {
    when(repository.findAll()).thenReturn(Datos.EXAMENES);

    service.findExamenPorNombreConPreguntas("Matematicas");

    verify(preguntaRepository, times(1)).findPreguntasPorExamenId(5L);
    verify(preguntaRepository, atLeast(1)).findPreguntasPorExamenId(5L);
    verify(preguntaRepository, atLeastOnce()).findPreguntasPorExamenId(5L);
    verify(preguntaRepository, atMost(1)).findPreguntasPorExamenId(5L);
}
```

1. `when(repository.findAll()).thenReturn(Datos.EXAMENES)` prepara una respuesta simulada.
2. `service.findExamenPorNombreConPreguntas("Matematicas")` ejecuta la lógica real del servicio. Si encuentra el examen con ID `5L`, llama a `preguntaRepository.findPreguntasPorExamenId(5L)`.
3. Cada `verify(...)` **comprueba el historial de llamadas** que Mockito registró; **no vuelve a ejecutar el método**.

## Modos de verificación

| Modo | Significado | ¿Pasa si hubo exactamente una llamada? |
|---|---|---|
| `times(1)` | Exactamente una llamada | Sí |
| `atLeast(1)` | Una o más llamadas | Sí |
| `atLeastOnce()` | Una o más llamadas | Sí |
| `atMost(1)` | Cero o una llamada | Sí |
| `never()` o `times(0)` | Ninguna llamada | No |
| `times(2)` | Exactamente dos llamadas | No |
| `atMostOnce()` | Cero o una llamada | Sí |

`verify(mock).metodo()` equivale a `verify(mock, times(1)).metodo()`.

### Tabla de ejemplos

| Llamadas reales | `times(1)` | `atLeastOnce()` | `atMost(1)` | `never()` |
|---:|---|---|---|---|
| 0 | Falla | Falla | Pasa | Pasa |
| 1 | Pasa | Pasa | Pasa | Falla |
| 2 | Falla | Pasa | Falla | Falla |

## Importante: verificar no significa invocar

En el código original, `atLeastOnce()` aparecía dos veces. Repetir una verificación es redundante, no significa que Mockito cuente dos llamadas. Todas las verificaciones inspeccionan el mismo historial registrado.

Además, las verificaciones se refieren a **ese método con ese argumento** (`5L`); otras llamadas con IDs diferentes no cuentan para esta condición concreta.

## Ejemplo: detectar una llamada duplicada

```java
service.findExamenPorNombreConPreguntas("Matematicas");
service.findExamenPorNombreConPreguntas("Matematicas");

verify(preguntaRepository, times(1)).findPreguntasPorExamenId(5L);
// Falla si el servicio llamó al repositorio dos veces.
```

Se asume que el servicio llama una vez al repositorio por cada búsqueda y no existe caché que evite la segunda llamada.

## Otros usos

```java
verify(preguntaRepository, never()).guardarVarias(anyList());
verify(repository, times(1)).findAll();
verify(preguntaRepository, atMost(2)).findPreguntasPorExamenId(5L);
```

Para verificar que no hubo **ninguna interacción** con un mock: `verifyNoInteractions(preguntaRepository)`.

## Conceptos de las APIs

- `verify()`: método estático de Mockito que verifica interacciones previas con mocks.
- `times(int n)`: exige exactamente `n` invocaciones.
- `atLeast(int n)`: exige `n` o más.
- `atLeastOnce()`: equivalente a `atLeast(1)`.
- `atMost(int n)`: exige `n` o menos.
- `atMostOnce()`: equivalente a `atMost(1)`.
- `never()`: equivalente a `times(0)`.
- `when().thenReturn()`: configura un mock y no comprueba el número de llamadas.

**Idea clave:** el modo de verificación expresa una regla sobre el historial de invocaciones, no produce llamadas nuevas.

