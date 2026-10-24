package org.gerardo.appmockito.ejemplo.repositories;

import org.gerardo.appmockito.ejemplo.models.Examen;

import java.util.List;

public interface ExamenRepository {
    Examen guardar(Examen examen);
    List<Examen> findAll();
}
