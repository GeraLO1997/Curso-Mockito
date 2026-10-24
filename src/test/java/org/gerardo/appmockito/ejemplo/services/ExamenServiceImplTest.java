package org.gerardo.appmockito.ejemplo.services;

import org.gerardo.appmockito.ejemplo.models.Examen;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepository;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepositoryImpl;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void findExamenPorNombreListaVacia(){

        ExamenRepository repository = mock(ExamenRepository.class);

        ExamenService service = new ExamenServiceImpl(repository);

        List<Examen> datos = Collections.emptyList();

        when(repository.findAll()).thenReturn(datos);

        Optional<Examen> examen = service.findExamenPorNombre("Matematicas");


        assertAll("Buscar examen por atributos, lista vacia",
                () -> assertTrue(examen.isPresent()),
                ()-> assertEquals(5L, examen.orElseThrow().getId()),
                () -> assertEquals("Matematicas", examen.orElseThrow().getNombre())
        );

//        assertNotNull(examen);

    }
}