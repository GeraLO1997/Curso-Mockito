package org.gerardo.appmockito.ejemplo.repositories;

import org.gerardo.appmockito.ejemplo.Datos;
import org.gerardo.appmockito.ejemplo.models.Examen;

import java.util.Arrays;
import java.util.List;

public class ExamenRepositoryImpl implements ExamenRepository{


    @Override
    public Examen guardar(Examen examen) {
        System.out.println("ExamenRepositoryImpl.guardar");
        return Datos.EXAMEN;
    }

    @Override
    public List<Examen> findAll() {
        System.out.println("ExamenRepositoryImpl.findAll");
        return Datos.EXAMENES;
    }
}
