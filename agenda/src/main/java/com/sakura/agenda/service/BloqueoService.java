package com.sakura.agenda.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.Bloqueo;
import com.sakura.agenda.repository.BloqueoRepository;

@Service
public class BloqueoService {

	@Autowired
	private BloqueoRepository bloqueoRepository;

	public List<Bloqueo> listar(LocalDate fecha) {
		if (fecha == null) {
			return bloqueoRepository.findAll();
		}
		return bloqueoRepository.findByFechaOrderByHoraInicioAsc(fecha);
	}

	public Bloqueo crear(Bloqueo bloqueo) {
		if (!bloqueo.getHoraInicio().isBefore(bloqueo.getHoraFin())) {
			throw new ReglaNegocioException("La hora de inicio debe ser anterior a la de término");
		}
		bloqueo.setId(null);
		return bloqueoRepository.save(bloqueo);
	}

	public void eliminar(Long id) {
		if (!bloqueoRepository.existsById(id)) {
			throw new RecursoNoEncontradoException("No existe el bloqueo");
		}
		bloqueoRepository.deleteById(id);
	}

}
