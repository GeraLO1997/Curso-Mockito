package org.gerardo.appmockito.ejemplo.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.gerardo.appmockito.ejemplo.models.Examen;
import org.gerardo.appmockito.ejemplo.repositories.ExamenRepository;

import java.util.Optional;

@RequiredArgsConstructor
@AllArgsConstructor
public class ExamenServiceImpl implements ExamenService{

    private ExamenRepository examenRepository;

    @Override
    public Examen findExamenPorNombre(String nombre) {
        Optional<Examen> examenOptional = examenRepository.findAll()
                .stream()
                .filter(e -> e.getNombre().contains(nombre))
                .findFirst();

        Examen examen = null;
        if (examenOptional.isPresent()){
            examen = examenOptional.orElseThrow();
        }
        return null;

    }
}
