package org.gerardo.appmockito.ejemplo.services;

import org.gerardo.appmockito.ejemplo.models.Examen;

import java.util.Optional;

public interface ExamenService {
    Optional<Examen> findExamenPorNombre(String nombre);
    Examen findExamenPorNombreConPreguntas(String nombre);

    Examen guardar(Examen examen);

}
