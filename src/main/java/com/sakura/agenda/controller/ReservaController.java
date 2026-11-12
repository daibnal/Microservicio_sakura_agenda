package com.sakura.agenda.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sakura.agenda.client.SesionDTO;
import com.sakura.agenda.client.SesionGuard;
import com.sakura.agenda.model.CancelacionRequest;
import com.sakura.agenda.model.Hueco;
import com.sakura.agenda.model.MoverReservaRequest;
import com.sakura.agenda.model.PagoRequest;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.model.ReservaRequest;
import com.sakura.agenda.model.ResultadoReserva;
import com.sakura.agenda.service.DisponibilidadService;
import com.sakura.agenda.service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

	@Autowired
	private ReservaService reservaService;

	@Autowired
	private DisponibilidadService disponibilidadService;

	@Autowired
	private SesionGuard sesionGuard;

	@GetMapping("/disponibilidad")
	public List<Hueco> disponibilidad(@RequestParam Long profesionalId, @RequestParam Long servicioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return disponibilidadService.listarHuecos(profesionalId, servicioId, fecha);
	}

	@GetMapping("/mias")
	public List<Reserva> mias(@RequestHeader(value = "Authorization", required = false) String authorization) {
		SesionDTO sesion = sesionGuard.exigirSesion(authorization);
		return reservaService.mias(sesion.getId());
	}

	@GetMapping("/agenda")
	public List<Reserva> agenda(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam String vista,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Long profesionalId,
			@RequestParam(required = false) String estado) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.agenda(vista, fecha, profesionalId, estado);
	}

	@GetMapping("/rango")
	public List<Reserva> rango(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.rango(desde, hasta);
	}

	@GetMapping("/historial")
	public List<Reserva> historial(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam Long profesionalId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.historial(profesionalId, desde, hasta);
	}

	@GetMapping("/profesional/{profesionalId}/existe")
	public Map<String, Boolean> existe(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long profesionalId) {
		sesionGuard.exigirAdmin(authorization);
		return Map.of("existe", reservaService.tieneReservas(profesionalId));
	}

	@PostMapping
	public ResponseEntity<ResultadoReserva> crear(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody ReservaRequest datos) {
		SesionDTO sesion = sesionGuard.exigirSesion(authorization);
		return new ResponseEntity<>(reservaService.crear(sesion, datos), HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public Reserva obtener(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		SesionDTO sesion = sesionGuard.exigirSesion(authorization);
		Reserva reserva = reservaService.buscar(id);
		reservaService.exigirDuenoOAdmin(reserva, sesion);
		return reserva;
	}

	@PutMapping("/{id}")
	public Reserva mover(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id, @Valid @RequestBody MoverReservaRequest datos) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.mover(id, datos);
	}

	@PostMapping("/{id}/cancelar")
	public Reserva cancelar(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.cancelar(id);
	}

	@PostMapping("/{id}/cancelar-cliente")
	public Reserva cancelarCliente(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id, @Valid @RequestBody CancelacionRequest datos) {
		SesionDTO sesion = sesionGuard.exigirSesion(authorization);
		return reservaService.cancelarCliente(id, sesion, datos.getMotivo());
	}

	@PutMapping("/{id}/cliente")
	public Reserva moverCliente(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id, @Valid @RequestBody MoverReservaRequest datos) {
		SesionDTO sesion = sesionGuard.exigirSesion(authorization);
		return reservaService.moverCliente(id, sesion, datos);
	}

	@PostMapping("/{id}/asistencia")
	public Reserva asistencia(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.marcarAsistencia(id);
	}

	@PostMapping("/{id}/inasistencia")
	public Reserva inasistencia(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id, @RequestBody(required = false) CancelacionRequest datos) {
		sesionGuard.exigirAdmin(authorization);
		String motivo = datos == null ? null : datos.getMotivo();
		return reservaService.marcarInasistencia(id, motivo);
	}

	@PostMapping("/{id}/pagos")
	public Reserva pagarAbono(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id, @RequestBody PagoRequest request) {
		SesionDTO sesion = sesionGuard.exigirSesion(authorization);
		Reserva reserva = reservaService.buscar(id);
		reservaService.exigirDuenoOAdmin(reserva, sesion);
		return reservaService.pagarAbono(id, request.isAprobado(), authorization);
	}

	@PostMapping("/{id}/saldo")
	public Reserva saldo(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.registrarSaldo(id, authorization);
	}

	@PostMapping("/{id}/pagos/manual")
	public Reserva pagoManual(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		sesionGuard.exigirAdmin(authorization);
		return reservaService.registrarManual(id, authorization);
	}

}
