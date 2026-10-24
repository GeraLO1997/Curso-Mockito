package org.gerardo.appmockito.ejemplo.services;

import org.gerardo.appmockito.ejemplo.models.Examen;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepository;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepositoryImpl;
import org.gerardo.appmockito.ejemplo.repositories.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExamenServiceImplTest {

    ExamenRepository repository;
    ExamenService service;
    PreguntaRepository preguntaRepository;

    @BeforeEach
    void setUp() {
        // 1. Crear el repositorio simulado.
        repository = mock(ExamenRepository.class);
        preguntaRepository = mock(PreguntaRepository.class);

                // 2. Crear el servicio REAL e inyectarle el mock.
        service = new ExamenServiceImpl(repository,preguntaRepository);

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

}