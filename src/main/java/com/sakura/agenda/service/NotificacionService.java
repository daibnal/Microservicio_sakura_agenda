package com.sakura.agenda.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sakura.agenda.model.EstadosReserva;
import com.sakura.agenda.model.Notificacion;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.repository.NotificacionRepository;
import com.sakura.agenda.repository.ReservaRepository;

@Service
public class NotificacionService {

	@Autowired
	private NotificacionRepository notificacionRepository;

	@Autowired
	private ReservaRepository reservaRepository;

	public void confirmar(Reserva reserva) {
		avisar(reserva, "CONFIRMACION", "Confirmación de su cita en Sakura Salón. " + detalle(reserva));
	}

	public void avisar(Reserva reserva, String tipo, String mensaje) {
		Notificacion notificacion = new Notificacion();
		notificacion.setReservaId(reserva.getId());
		notificacion.setTipo(tipo);
		notificacion.setMensaje(mensaje);
		notificacion.setCreadaEn(LocalDateTime.now());
		notificacionRepository.save(notificacion);
		System.out.println("----- Correo Sakura -----");
		System.out.println("Para: " + reserva.getCorreoCliente());
		System.out.println(mensaje);
		System.out.println("-------------------------");
	}

	@Scheduled(cron = "0 0 * * * *")
	public void tareasHorarias() {
		liberarVencidas();
		enviarRecordatorios();
	}

	public void liberarVencidas() {
		List<Reserva> pendientes = reservaRepository.findByEstado(EstadosReserva.PENDIENTE_PAGO);
		for (Reserva reserva : pendientes) {
			if (reserva.getExpiraEn() != null && reserva.getExpiraEn().isBefore(LocalDateTime.now())) {
				reserva.setEstado(EstadosReserva.LIBERADA);
				reservaRepository.save(reserva);
			}
		}
	}

	public void enviarRecordatorios() {
		LocalDateTime desde = LocalDateTime.now().plusHours(23);
		LocalDateTime hasta = LocalDateTime.now().plusHours(25);
		List<Reserva> confirmadas = reservaRepository.findByEstado(EstadosReserva.CONFIRMADA);
		List<Reserva> pagadas = reservaRepository.findByEstado(EstadosReserva.PAGADA_COMPLETA);
		avisarSiCorresponde(confirmadas, desde, hasta);
		avisarSiCorresponde(pagadas, desde, hasta);
	}

	private void avisarSiCorresponde(List<Reserva> reservas, LocalDateTime desde, LocalDateTime hasta) {
		for (Reserva reserva : reservas) {
			LocalDateTime cuando = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
			if (cuando.isBefore(desde) || cuando.isAfter(hasta)) {
				continue;
			}
			if (notificacionRepository.existsByReservaIdAndTipo(reserva.getId(), "RECORDATORIO")) {
				continue;
			}
			avisar(reserva, "RECORDATORIO",
					"Recordatorio: su cita en Sakura Salón es en cerca de 24 horas. " + detalle(reserva));
		}
	}

	private String detalle(Reserva reserva) {
		return reserva.getNombreCliente() + ", " + reserva.getNombreServicio() + " con "
				+ reserva.getNombreProfesional() + " el " + reserva.getFecha() + " a las " + reserva.getHoraInicio()
				+ ".";
	}

}
