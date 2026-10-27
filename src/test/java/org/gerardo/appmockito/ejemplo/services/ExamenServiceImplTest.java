package org.gerardo.appmockito.ejemplo.services;

import org.gerardo.appmockito.ejemplo.models.Examen;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepository;
import org.gerardo.appmockito.ejemplo.repositories.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExamenServiceImplTest {

    @Mock
    ExamenRepository repository;
    @Mock
    PreguntaRepository preguntaRepository;

    @InjectMocks
    ExamenServiceImpl service; // Aqui necesitamos la clase concreta por que no se puede instanciar una interface

    @Captor
    ArgumentCaptor<Long> captor;

    @BeforeEach
    void setUp() {
        // Necesitamos crear una implementacion concreta, tenemos que habilitar el uso de anotaciones
//        MockitoAnnotations.openMocks(this); // le pasamos this como esta referencia

        // 1. Crear el repositorio simulado.
//        repository = mock(ExamenRepository.class);
//        preguntaRepository = mock(PreguntaRepository.class);

                // 2. Crear el servicio REAL e inyectarle el mock.
//        service = new ExamenServiceImpl(repository,preguntaRepository);

    }

    @Test
    void findExamenPorNombre() {


        // 3. Preparar datos de prueba.
        // Usamos la clase DATOS

        // 4. Configurar la respuesta del mock.
        when(repository.findAll()).thenReturn(Datos.EXAMENES);

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

    @Test
    void findExamenPorNombreListaVacia(){

        List<Examen> datos = Collections.emptyList();

        when(repository.findAll()).thenReturn(datos);

        Optional<Examen> examen = service.findExamenPorNombre("Matematicas");


        assertAll("Buscar examen por atributos, lista vacia",
                () -> assertFalse(examen.isPresent())

        );
    }

    @Test
    void testPreguntasExamen(){
        when(repository.findAll()).thenReturn(Datos.EXAMENES);
        // le indicamos que solo cuando invoque con el agumento 5 tendremos las preguntas (match arguments)
        // agregamos anyLong() para que acepte cualquier tipo de dato
        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");
        assertEquals(5, examen.getPreguntas().size());
        assertTrue(examen.getPreguntas().contains("aritmetica"));
    }

    @Test
    void testPreguntasExamenVerify(){
        when(repository.findAll()).thenReturn(Datos.EXAMENES);
        // le indicamos que solo cuando invoque con el agumento 5 tendremos las preguntas (match arguments)
        // agregamos anyLong() para que acepte cualquier tipo de dato
        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");
        assertEquals(5, examen.getPreguntas().size());
        assertTrue(examen.getPreguntas().contains("aritmetica"));

        // aqui pondemos el . despues de los parentesis
        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(anyLong());
    }

    @Test
    void testNoExisteExamenVerify(){
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");
        assertNull(examen);

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(anyLong());
    }

    @Test
    @DisplayName("Guardar Examen")
    void testGuardarExamen(){
        // guardamos cualquier tipo de examen sin preguntas
        when(repository.guardar(any(Examen.class))).thenReturn(Datos.EXAMEN);

        Examen examen = service.guardar(Datos.EXAMEN);
        assertNotNull(examen.getId());
        assertEquals(8L,examen.getId());
        assertEquals("Fisica", examen.getNombre());

        verify(repository).guardar(any(Examen.class));
        verify(preguntaRepository).guardarVarias(anyList());
    }

    @Test
    @DisplayName("Guardar Examen con preguntas")
    // Given
    void testGuardarExamenConPeguntas(){
        Examen newExamen = Datos.EXAMEN;
        newExamen.setPreguntas(Datos.PREGUNTAS);

        when(repository.guardar(any(Examen.class))).then(new Answer<Examen>(){

            Long secuencia = 8L;

            @Override
            public Examen answer(InvocationOnMock invocation) throws Throwable {
                Examen examen = invocation.getArgument(0);
                examen.setId(secuencia++);
                return examen;
            }
        });

        // When
        Examen examen = service.guardar(newExamen); // agui lo guardamos

        // Then
        assertNotNull(examen.getId());
        assertEquals(8L,examen.getId());
        assertEquals("Fisica", examen.getNombre());

        verify(repository).guardar(any(Examen.class));
        verify(preguntaRepository).guardarVarias(anyList());
    }

    @Test
    void testManejoException(){

        when(repository.findAll()).thenReturn(Datos.EXAMENES);
        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenThrow(IllegalArgumentException.class);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.findExamenPorNombreConPreguntas("Matematicas");
        });

        assertEquals(IllegalArgumentException.class, exception.getClass());

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(anyLong());
    }

    void testManejoExceptionNull(){

        when(repository.findAll()).thenReturn(Datos.EXAMENES_ID_NULL);
        when(preguntaRepository.findPreguntasPorExamenId(isNull())).thenThrow(IllegalArgumentException.class);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.findExamenPorNombreConPreguntas("Matematicas");
        });

        assertEquals(IllegalArgumentException.class, exception.getClass());

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(isNull());
    }

    // Implementando con clases anonimas
    @Test
    void testArgumentMatchers(){
        when(repository.findAll()).thenReturn(Datos.EXAMENES);
        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        service.findExamenPorNombreConPreguntas("Matematicas");

        verify(repository).findAll();
//        verify(preguntaRepository).findPreguntasPorExamenId(argThat(arg -> arg != null && arg.equals(5L)));
        verify(preguntaRepository).findPreguntasPorExamenId(argThat(arg -> arg != null && arg >= 5L));
    }

    @Test
    void testArgumentMatchersClaseAnidada(){
        when(repository.findAll()).thenReturn(Datos.EXAMENES_ID_NEGATIVOS);
        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        service.findExamenPorNombreConPreguntas("Matematicas");

        verify(repository).findAll();
        verify(preguntaRepository).findPreguntasPorExamenId(argThat(new MiArgsMatchers()));
    }

    // clase anidada (dentro de la clase test)
    // La ventaja de esta forma es que podemos implementar el toString y escribir un mensaje
    public static class MiArgsMatchers implements ArgumentMatcher<Long>{

        private Long argument;

        @Override
        public boolean matches(Long argument) {
            this.argument = argument;
            return argument !=null && argument > 0;
        }

        @Override
        public String toString() {
            return "es para un mensaje personalizado de error que imprime mockito en caso de que falle el test " +
                    + argument + " debe ser un entero positivo";
        }
    }


    @Test
    void testArgumentCapture(){
        when(repository.findAll()).thenReturn(Datos.EXAMENES);
//        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        service.findExamenPorNombreConPreguntas("Matematicas");

        // o podemos usar la anotacion @Captor
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(preguntaRepository).findPreguntasPorExamenId(captor.capture());

        assertEquals(5L, captor.getValue());
    }


    @Test
    void testDoThrow() {
        Examen examen = Datos.EXAMEN;
        examen.setPreguntas(Datos.PREGUNTAS); // se lanza la excepcion si no le asignamos las preguntas
        doThrow(IllegalArgumentException.class).when(preguntaRepository).guardarVarias(anyList());

        assertThrows(IllegalArgumentException.class, () -> {
            service.guardar(examen);
        });
    }

    @Test
    void testDoAnswer(){
        when(repository.findAll()).thenReturn(Datos.EXAMENES);
//        when(preguntaRepository.findPreguntasPorExamenId(anyLong())).thenReturn(Datos.PREGUNTAS);

        doAnswer(invocationOnMock -> {
            Long id = invocationOnMock.getArgument(0);
            return id == 5L ? Datos.PREGUNTAS : null;
        }).when(preguntaRepository).findPreguntasPorExamenId(anyLong());

        Examen examen = service.findExamenPorNombreConPreguntas("Matematicas");

        assertEquals(5L, examen.getId());
        assertEquals("Matematicas",examen.getNombre());
        assertEquals(5,examen.getPreguntas().size());
        assertTrue(examen.getPreguntas().contains("geometria"));

        verify(preguntaRepository).findPreguntasPorExamenId(anyLong());
    }


    // con un id incremental
    @Test
    @DisplayName("Guardar Examen con preguntas")
        // Given
    void testDoAnswerGuardarExamenConPeguntas(){
        Examen newExamen = Datos.EXAMEN;
        newExamen.setPreguntas(Datos.PREGUNTAS);

//        when(repository.guardar(any(Examen.class))).then();
        doAnswer(new Answer<Examen>(){

            Long secuencia = 8L;

            @Override
            public Examen answer(InvocationOnMock invocation) throws Throwable {
                Examen examen = invocation.getArgument(0);
                examen.setId(secuencia++);
                return examen;
            }
        }).when(repository).guardar(any(Examen.class));

        // When
        Examen examen = service.guardar(newExamen); // agui lo guardamos

        // Then
        assertNotNull(examen.getId());
        assertEquals(8L,examen.getId());
        assertEquals("Fisica", examen.getNombre());

        verify(repository).guardar(any(Examen.class));
        verify(preguntaRepository).guardarVarias(anyList());
    }
}