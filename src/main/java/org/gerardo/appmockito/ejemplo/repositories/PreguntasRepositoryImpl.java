package org.gerardo.appmockito.ejemplo.repositories;

import org.gerardo.appmockito.ejemplo.Datos;

import java.util.List;

public class PreguntasRepositoryImpl implements PreguntaRepository{
    @Override
    public void guardarVarias(List<String> preguntas) {
        System.out.println("PreguntasRepositoryImpl.guardarVarias");

    }

    @Override
    public List<String> findPreguntasPorExamenId(Long id) {
        System.out.println("PreguntasRepositoryImpl.findPreguntasPorExamenId");
        return Datos.PREGUNTAS;
    }
}
