package com.sakura.agenda.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sakura.agenda.client.CatalogoCliente;
import com.sakura.agenda.client.ConfiguracionCliente;
import com.sakura.agenda.client.PersonalCliente;
import com.sakura.agenda.client.ProfesionalDTO;
import com.sakura.agenda.client.ServicioDTO;
import com.sakura.agenda.client.TurnoDTO;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.Hueco;
import com.sakura.agenda.repository.BloqueoRepository;
import com.sakura.agenda.repository.ReservaRepository;

@Service
public class DisponibilidadService {

	@Autowired
	private PersonalCliente personalCliente;

	@Autowired
	private CatalogoCliente catalogoCliente;

	@Autowired
	private BloqueoRepository bloqueoRepository;

	@Autowired
	private ReservaRepository reservaRepository;

	@Autowired
	private ConfiguracionCliente configuracionCliente;

	public void verificarLibre(Long profesionalId, Long servicioId, LocalDate fecha, LocalTime horaInicio,
			Long ignorarReservaId) {
		ServicioDTO servicio = catalogoCliente.obtener(servicioId);
		ProfesionalDTO profesional = personalCliente.obtener(profesionalId);
		if (!profesional.isActivo()) {
			throw new ReglaNegocioException("El profesional no está disponible");
		}
		if (!servicio.isActivo()) {
			throw new ReglaNegocioException("El servicio no está disponible");
		}
		if (!realiza(profesionalId, servicioId)) {
			throw new ReglaNegocioException("El profesional no realiza este servicio");
		}
		if (ausente(profesionalId, fecha)) {
			throw new ReglaNegocioException("El profesional no atiende en esa fecha");
		}
		if (fecha.isBefore(LocalDate.now())) {
			throw new ReglaNegocioException("No se puede agendar en una fecha pasada");
		}
		validarAnticipacion(fecha, horaInicio);
		LocalTime horaFin = finDeTramo(fecha, horaInicio, servicio.getDuracionMinutos());
		if (fecha.equals(LocalDate.now()) && !horaInicio.isAfter(LocalTime.now())) {
			throw new ReglaNegocioException("Ese horario ya pasó");
		}
		List<TurnoDTO> turnos = turnosDelDia(profesionalId, fecha);
		if (!cabeEnTurno(turnos, horaInicio, horaFin)) {
			throw new ReglaNegocioException("El horario está fuera del turno del profesional");
		}
		if (caeEnAlmuerzo(turnos, horaInicio, horaFin)) {
			throw new ReglaNegocioException("El horario corresponde al descanso del profesional");
		}
		if (!bloqueoRepository.buscarCruces(fecha, horaInicio, horaFin).isEmpty()) {
			throw new ReglaNegocioException("El horario está bloqueado por la administración");
		}
		boolean ocupado = reservaRepository
				.buscarCruces(profesionalId, fecha, horaInicio, horaFin, LocalDateTime.now())
				.stream()
				.anyMatch(reserva -> ignorarReservaId == null || !ignorarReservaId.equals(reserva.getId()));
		if (ocupado) {
			throw new ReglaNegocioException("El horario ya no está disponible");
		}
	}

	public List<Hueco> listarHuecos(Long profesionalId, Long servicioId, LocalDate fecha) {
		ServicioDTO servicio = catalogoCliente.obtener(servicioId);
		ProfesionalDTO profesional = personalCliente.obtener(profesionalId);
		if (!profesional.isActivo() || !servicio.isActivo()) {
			return List.of();
		}
		if (!realiza(profesionalId, servicioId)) {
			return List.of();
		}
		if (ausente(profesionalId, fecha)) {
			return List.of();
		}
		if (fecha.isBefore(LocalDate.now()) || fecha.isAfter(LocalDate.now().plusDays(diasMaximos()))) {
			return List.of();
		}
		List<TurnoDTO> turnos = turnosDelDia(profesionalId, fecha);
		List<Hueco> huecos = new ArrayList<>();
		for (TurnoDTO turno : turnos) {
			LocalTime cursor = turno.getHoraInicio();
			while (true) {
				LocalDateTime finFecha = LocalDateTime.of(fecha, cursor).plusMinutes(servicio.getDuracionMinutos());
				LocalTime horaFin = finFecha.toLocalTime();
				if (!horaFin.isAfter(cursor) || !finFecha.toLocalDate().equals(fecha)
						|| horaFin.isAfter(turno.getHoraFin())) {
					break;
				}
				boolean yaPaso = fecha.equals(LocalDate.now()) && !cursor.isAfter(LocalTime.now());
				boolean muyPronto = LocalDateTime.of(fecha, cursor).isBefore(LocalDateTime.now().plusHours(horasMinimas()));
				boolean almuerzo = caeEnAlmuerzo(List.of(turno), cursor, horaFin);
				boolean bloqueado = !bloqueoRepository.buscarCruces(fecha, cursor, horaFin).isEmpty();
				boolean ocupado = !reservaRepository.buscarCruces(profesionalId, fecha, cursor, horaFin, LocalDateTime.now())
						.isEmpty();
				if (!yaPaso && !muyPronto && !almuerzo && !bloqueado && !ocupado) {
					huecos.add(new Hueco(cursor, horaFin));
				}
				cursor = horaFin;
			}
		}
		return huecos;
	}

	private boolean realiza(Long profesionalId, Long servicioId) {
		return personalCliente.servicios(profesionalId).stream()
				.anyMatch(servicio -> servicioId.equals(servicio.getId()));
	}

	private boolean ausente(Long profesionalId, LocalDate fecha) {
		return personalCliente.ausencias(profesionalId).stream()
				.anyMatch(ausencia -> !fecha.isBefore(ausencia.getFechaInicio()) && !fecha.isAfter(ausencia.getFechaFin()));
	}

	private List<TurnoDTO> turnosDelDia(Long profesionalId, LocalDate fecha) {
		int dia = fecha.getDayOfWeek().getValue();
		return personalCliente.turnos(profesionalId).stream()
				.filter(turno -> turno.getDiaSemana() != null && turno.getDiaSemana() == dia)
				.toList();
	}

	private LocalTime finDeTramo(LocalDate fecha, LocalTime horaInicio, int duracionMinutos) {
		LocalDateTime fin = LocalDateTime.of(fecha, horaInicio).plusMinutes(duracionMinutos);
		if (!fin.toLocalDate().equals(fecha)) {
			throw new ReglaNegocioException("El horario está fuera del turno del profesional");
		}
		return fin.toLocalTime();
	}

	private boolean cabeEnTurno(List<TurnoDTO> turnos, LocalTime horaInicio, LocalTime horaFin) {
		for (TurnoDTO turno : turnos) {
			if (!horaInicio.isBefore(turno.getHoraInicio()) && !horaFin.isAfter(turno.getHoraFin())) {
				return true;
			}
		}
		return false;
	}

	private void validarAnticipacion(LocalDate fecha, LocalTime horaInicio) {
		if (fecha.isAfter(LocalDate.now().plusDays(diasMaximos()))) {
			throw new ReglaNegocioException("No se puede agendar con tanta anticipación");
		}
		if (LocalDateTime.of(fecha, horaInicio).isBefore(LocalDateTime.now().plusHours(horasMinimas()))) {
			throw new ReglaNegocioException("Se requiere mayor margen de anticipación");
		}
	}

	private boolean caeEnAlmuerzo(List<TurnoDTO> turnos, LocalTime horaInicio, LocalTime horaFin) {
		for (TurnoDTO turno : turnos) {
			if (turno.getHoraAlmuerzoInicio() == null || turno.getHoraAlmuerzoFin() == null) {
				continue;
			}
			if (horaInicio.isBefore(turno.getHoraAlmuerzoFin()) && turno.getHoraAlmuerzoInicio().isBefore(horaFin)) {
				return true;
			}
		}
		return false;
	}

	private int diasMaximos() {
		return configuracionCliente.obtener().getDiasAnticipacionMaxima();
	}

	private int horasMinimas() {
		return configuracionCliente.obtener().getHorasAnticipacionMinima();
	}

}
