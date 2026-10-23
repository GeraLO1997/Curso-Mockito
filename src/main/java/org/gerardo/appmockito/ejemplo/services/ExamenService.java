package org.gerardo.appmockito.ejemplo.services;

import org.gerardo.appmockito.ejemplo.models.Examen;

public interface ExamenService {
    Examen findExamenPorNombre(String nombre);

}
