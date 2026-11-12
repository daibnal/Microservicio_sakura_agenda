package com.sakura.agenda.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sakura.agenda.client.CatalogoCliente;
import com.sakura.agenda.client.ConfiguracionCliente;
import com.sakura.agenda.client.PagoCliente;
import com.sakura.agenda.client.PersonalCliente;
import com.sakura.agenda.client.ProfesionalDTO;
import com.sakura.agenda.client.ServicioDTO;
import com.sakura.agenda.client.SesionDTO;
import com.sakura.agenda.exception.AccesoDenegadoException;
import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.EstadosReserva;
import com.sakura.agenda.model.MoverReservaRequest;
import com.sakura.agenda.model.Prestacion;
import com.sakura.agenda.model.PrestacionPedido;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.model.ReservaRequest;
import com.sakura.agenda.model.ResultadoReserva;
import com.sakura.agenda.repository.PrestacionRepository;
import com.sakura.agenda.repository.ReservaRepository;

@Service
public class ReservaService {

	@Autowired
	private ReservaRepository reservaRepository;

	@Autowired
	private PrestacionRepository prestacionRepository;

	@Autowired
	private CatalogoCliente catalogoCliente;

	@Autowired
	private PersonalCliente personalCliente;

	@Autowired
	private DisponibilidadService disponibilidadService;

	@Autowired
	private NotificacionService notificacionService;

	@Autowired
	private PagoCliente pagoCliente;

	@Autowired
	private ConfiguracionCliente configuracionCliente;

	@Value("${sakura.reserva.plazo-minutos}")
	private int plazoMinutos;

	public int calcularAbono(int total) {
		return total * configuracionCliente.obtener().getPorcentajeAbono() / 100;
	}

	public ResultadoReserva crear(SesionDTO sesion, ReservaRequest datos) {
		Reserva reserva = armar(sesion, datos, datos.getFecha(), null);
		List<Reserva> generadas = new ArrayList<>();
		generadas.add(reserva);
		List<LocalDate> omitidas = new ArrayList<>();
		if ("QUINCENAL".equalsIgnoreCase(datos.getRecurrencia())) {
			int repeticiones = datos.getRepeticiones() == null ? 1 : datos.getRepeticiones();
			if (repeticiones < 1) {
				throw new ReglaNegocioException("Las repeticiones deben ser al menos 1");
			}
			String serie = UUID.randomUUID().toString();
			reserva.setSerieId(serie);
			reservaRepository.save(reserva);
			for (int i = 1; i < repeticiones; i++) {
				LocalDate fecha = datos.getFecha().plusDays(14L * i);
				try {
					generadas.add(armar(sesion, datos, fecha, serie));
				} catch (ReglaNegocioException ex) {
					omitidas.add(fecha);
				}
			}
		}
		return new ResultadoReserva(reserva, generadas, omitidas);
	}

	private Reserva armar(SesionDTO sesion, ReservaRequest datos, LocalDate fecha, String serieId) {
		List<PrestacionPedido> pedidos = pedidos(datos);
		LocalTime cursor = datos.getHoraInicio();
		List<Prestacion> lineas = new ArrayList<>();
		int total = 0;
		int minutos = 0;
		List<String> nombres = new ArrayList<>();
		for (int i = 0; i < pedidos.size(); i++) {
			PrestacionPedido pedido = pedidos.get(i);
			ServicioDTO servicio = catalogoCliente.obtener(pedido.getServicioId());
			ProfesionalDTO profesional = personalCliente.obtener(pedido.getProfesionalId());
			disponibilidadService.verificarLibre(profesional.getId(), servicio.getId(), fecha, cursor, null);
			LocalTime fin = cursor.plusMinutes(servicio.getDuracionMinutos());
			String categoria = servicio.getCategoria() == null ? "" : servicio.getCategoria().getNombre();
			if (categoria == null) {
				categoria = "";
			}
			Prestacion prestacion = new Prestacion();
			prestacion.setOrden(i + 1);
			prestacion.setServicioId(servicio.getId());
			prestacion.setNombreServicio(servicio.getNombre());
			prestacion.setNombreCategoria(categoria);
			prestacion.setDuracionMinutos(servicio.getDuracionMinutos());
			prestacion.setPrecio(servicio.getPrecio());
			prestacion.setProfesionalId(profesional.getId());
			prestacion.setNombreProfesional(profesional.getNombre());
			prestacion.setHoraInicio(cursor);
			prestacion.setHoraFin(fin);
			lineas.add(prestacion);
			total += servicio.getPrecio();
			minutos += servicio.getDuracionMinutos();
			nombres.add(servicio.getNombre());
			cursor = fin;
		}
		Prestacion primera = lineas.get(0);

		String nombre = sesion.getNombre();
		if (sesion.getApellido() != null && !sesion.getApellido().isBlank()) {
			nombre = nombre + " " + sesion.getApellido();
		}

		Reserva reserva = new Reserva();
		reserva.setIdCliente(sesion.getId());
		reserva.setCorreoCliente(sesion.getCorreo());
		reserva.setNombreCliente(nombre);
		reserva.setServicioId(primera.getServicioId());
		reserva.setNombreServicio(recortar(String.join(", ", nombres), 120));
		reserva.setDuracionMinutos(minutos);
		reserva.setNombreCategoria(recortar(primera.getNombreCategoria(), 80));
		reserva.setProfesionalId(primera.getProfesionalId());
		reserva.setNombreProfesional(primera.getNombreProfesional());
		reserva.setFecha(fecha);
		reserva.setHoraInicio(datos.getHoraInicio());
		reserva.setHoraFin(cursor);
		reserva.setEstado(EstadosReserva.PENDIENTE_PAGO);
		reserva.setMontoTotal(total);
		reserva.setMontoAbono(calcularAbono(total));
		reserva.setMontoPagado(0);
		reserva.setNotas(datos.getNotas());
		reserva.setSerieId(serieId);
		reserva.setExpiraEn(LocalDateTime.now().plusMinutes(plazoMinutos));
		reserva.setCreadaEn(LocalDateTime.now());
		for (Prestacion prestacion : lineas) {
			reserva.agregarPrestacion(prestacion);
		}
		return reservaRepository.save(reserva);
	}

	private List<PrestacionPedido> pedidos(ReservaRequest datos) {
		if (datos.getPrestaciones() != null && !datos.getPrestaciones().isEmpty()) {
			return datos.getPrestaciones();
		}
		if (datos.getServicioId() == null || datos.getProfesionalId() == null) {
			throw new ReglaNegocioException("Debe indicar al menos un servicio");
		}
		PrestacionPedido unico = new PrestacionPedido();
		unico.setServicioId(datos.getServicioId());
		unico.setProfesionalId(datos.getProfesionalId());
		return List.of(unico);
	}

	private List<Prestacion> lineas(Reserva reserva) {
		List<Prestacion> guardadas = reserva.prestacionesGuardadas();
		if (guardadas.isEmpty()) {
			return reserva.getPrestaciones();
		}
		return guardadas;
	}

	public List<Reserva> mias(Long idCliente) {
		return reservaRepository.findByIdClienteOrderByFechaDescHoraInicioDesc(idCliente);
	}

	public Reserva buscar(Long id) {
		return reservaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva"));
	}

	public void exigirDuenoOAdmin(Reserva reserva, SesionDTO sesion) {
		if ("ADMIN".equals(sesion.getRol())) {
			return;
		}
		if (!reserva.getIdCliente().equals(sesion.getId())) {
			throw new AccesoDenegadoException("No puede operar una reserva de otro cliente");
		}
	}

	public List<Reserva> agenda(String vista, LocalDate fecha, Long profesionalId, String estado) {
		LocalDate desde;
		LocalDate hasta;
		if ("dia".equals(vista)) {
			desde = fecha;
			hasta = fecha;
		} else if ("semana".equals(vista)) {
			desde = fecha.minusDays(fecha.getDayOfWeek().getValue() - 1L);
			hasta = desde.plusDays(6);
		} else if ("mes".equals(vista)) {
			desde = fecha.withDayOfMonth(1);
			hasta = fecha.withDayOfMonth(fecha.lengthOfMonth());
		} else {
			throw new ReglaNegocioException("La vista debe ser dia, semana o mes");
		}
		List<Reserva> visitas = reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta).stream()
				.filter(reserva -> !EstadosReserva.LIBERADA.equals(reserva.getEstado()))
				.filter(reserva -> estado == null || estado.isBlank() || estado.equals(reserva.getEstado()))
				.toList();
		if (profesionalId == null) {
			return visitas;
		}
		List<Reserva> filas = new ArrayList<>();
		for (Reserva visita : visitas) {
			for (Prestacion prestacion : lineas(visita)) {
				if (profesionalId.equals(prestacion.getProfesionalId())) {
					filas.add(filaDeAgenda(visita, prestacion));
				}
			}
		}
		return filas;
	}

	public List<Reserva> rango(LocalDate desde, LocalDate hasta) {
		return reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta);
	}

	public List<Reserva> historial(Long profesionalId, LocalDate desde, LocalDate hasta) {
		List<Reserva> visitas = reservaRepository.findHistorial(profesionalId, desde, hasta,
				List.of(EstadosReserva.CONFIRMADA, EstadosReserva.PAGADA_COMPLETA, EstadosReserva.EN_PROCESO));
		List<Reserva> filas = new ArrayList<>();
		for (Reserva visita : visitas) {
			for (Prestacion prestacion : lineas(visita)) {
				if (profesionalId.equals(prestacion.getProfesionalId())) {
					filas.add(filaDeAgenda(visita, prestacion));
				}
			}
		}
		return filas;
	}

	public boolean tieneReservas(Long profesionalId) {
		return reservaRepository.existsByProfesionalId(profesionalId)
				|| prestacionRepository.existsByProfesionalId(profesionalId);
	}

	public Reserva cancelar(Long id) {
		Reserva reserva = buscar(id);
		if (EstadosReserva.CANCELADA.equals(reserva.getEstado()) || EstadosReserva.LIBERADA.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La reserva ya no se puede cancelar");
		}
		reserva.setEstado(EstadosReserva.CANCELADA);
		reservaRepository.save(reserva);
		notificacionService.avisar(reserva, "CANCELACION",
				"Su cita fue cancelada por la administración. " + texto(reserva));
		return reserva;
	}

	public Reserva mover(Long id, MoverReservaRequest datos) {
		Reserva reserva = buscar(id);
		if (EstadosReserva.CANCELADA.equals(reserva.getEstado()) || EstadosReserva.LIBERADA.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La reserva ya no se puede modificar");
		}
		reprogramar(reserva, datos.getFecha(), datos.getHoraInicio());
		reservaRepository.save(reserva);
		notificacionService.avisar(reserva, "MODIFICACION",
				"Su cita fue modificada por la administración. " + texto(reserva));
		return reserva;
	}

	public Reserva cancelarCliente(Long id, SesionDTO sesion, String motivo) {
		Reserva reserva = buscar(id);
		if (!reserva.getIdCliente().equals(sesion.getId())) {
			throw new AccesoDenegadoException("No puede cancelar una reserva de otro cliente");
		}
		if (!esFutura(reserva)) {
			throw new ReglaNegocioException("Solo puede cancelar una cita futura");
		}
		if (EstadosReserva.CANCELADA.equals(reserva.getEstado()) || EstadosReserva.LIBERADA.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La reserva ya no se puede cancelar");
		}
		reserva.setEstado(EstadosReserva.CANCELADA);
		reserva.setMotivoCancelacion(motivo);
		reservaRepository.save(reserva);
		notificacionService.avisar(reserva, "CANCELACION", "Su cita fue cancelada. Motivo: " + motivo);
		return reserva;
	}

	public Reserva moverCliente(Long id, SesionDTO sesion, MoverReservaRequest datos) {
		Reserva reserva = buscar(id);
		if (!reserva.getIdCliente().equals(sesion.getId())) {
			throw new AccesoDenegadoException("No puede modificar una reserva de otro cliente");
		}
		if (!esFutura(reserva)) {
			throw new ReglaNegocioException("Solo puede modificar una cita futura");
		}
		return mover(id, datos);
	}

	public Reserva marcarAsistencia(Long id) {
		Reserva reserva = buscar(id);
		if (!EstadosReserva.CONFIRMADA.equals(reserva.getEstado())
				&& !EstadosReserva.PAGADA_COMPLETA.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("Solo se puede marcar asistencia de una cita confirmada");
		}
		reserva.setEstado(EstadosReserva.EN_PROCESO);
		return reservaRepository.save(reserva);
	}

	public Reserva marcarInasistencia(Long id, String motivo) {
		Reserva reserva = buscar(id);
		if (EstadosReserva.CANCELADA.equals(reserva.getEstado()) || EstadosReserva.LIBERADA.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La reserva ya no admite inasistencia");
		}
		reserva.setEstado(EstadosReserva.INASISTENCIA);
		reserva.setMotivoCancelacion(motivo);
		return reservaRepository.save(reserva);
	}

	public Reserva pagarAbono(Long reservaId, boolean aprobado, String authorization) {
		Reserva reserva = buscar(reservaId);
		if (!EstadosReserva.PENDIENTE_PAGO.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La reserva no está pendiente de pago");
		}
		if (reserva.getExpiraEn() == null || reserva.getExpiraEn().isBefore(LocalDateTime.now())) {
			reserva.setEstado(EstadosReserva.LIBERADA);
			reservaRepository.save(reserva);
			throw new ReglaNegocioException("El tiempo para pagar el abono venció");
		}
		pagoCliente.registrar(reserva, "ABONO", reserva.getMontoAbono(), aprobado, authorization);
		if (!aprobado) {
			throw new ReglaNegocioException(
					"El pago fue rechazado. Puede reintentar mientras la reserva siga pendiente");
		}
		reserva.setEstado(EstadosReserva.CONFIRMADA);
		reserva.setMontoPagado(reserva.getMontoAbono());
		reservaRepository.save(reserva);
		notificacionService.confirmar(reserva);
		return reserva;
	}

	public Reserva registrarSaldo(Long reservaId, String authorization) {
		Reserva reserva = buscar(reservaId);
		if (!EstadosReserva.CONFIRMADA.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La cita debe estar confirmada para registrar el saldo");
		}
		int saldo = reserva.getMontoTotal() - reserva.getMontoPagado();
		pagoCliente.registrar(reserva, "SALDO", saldo, true, authorization);
		reserva.setMontoPagado(reserva.getMontoTotal());
		reserva.setEstado(EstadosReserva.PAGADA_COMPLETA);
		return reservaRepository.save(reserva);
	}

	public Reserva registrarManual(Long reservaId, String authorization) {
		Reserva reserva = buscar(reservaId);
		if (!EstadosReserva.PENDIENTE_PAGO.equals(reserva.getEstado())) {
			throw new ReglaNegocioException("La reserva no está pendiente de pago");
		}
		pagoCliente.registrar(reserva, "ABONO", reserva.getMontoAbono(), true, authorization);
		reserva.setEstado(EstadosReserva.CONFIRMADA);
		reserva.setMontoPagado(reserva.getMontoAbono());
		reservaRepository.save(reserva);
		notificacionService.confirmar(reserva);
		return reserva;
	}

	private void reprogramar(Reserva reserva, LocalDate fecha, LocalTime inicio) {
		List<Prestacion> actuales = lineas(reserva);
		LocalTime cursor = inicio;
		for (Prestacion prestacion : actuales) {
			disponibilidadService.verificarLibre(prestacion.getProfesionalId(), prestacion.getServicioId(), fecha,
					cursor, reserva.getId());
			LocalTime fin = cursor.plusMinutes(prestacion.getDuracionMinutos());
			prestacion.setHoraInicio(cursor);
			prestacion.setHoraFin(fin);
			cursor = fin;
		}
		if (reserva.prestacionesGuardadas().isEmpty()) {
			for (Prestacion prestacion : actuales) {
				reserva.agregarPrestacion(prestacion);
			}
		}
		reserva.setFecha(fecha);
		reserva.setHoraInicio(inicio);
		reserva.setHoraFin(cursor);
	}

	private Reserva filaDeAgenda(Reserva visita, Prestacion prestacion) {
		Reserva fila = new Reserva();
		fila.setId(visita.getId());
		fila.setIdCliente(visita.getIdCliente());
		fila.setCorreoCliente(visita.getCorreoCliente());
		fila.setNombreCliente(visita.getNombreCliente());
		fila.setServicioId(prestacion.getServicioId());
		fila.setNombreServicio(prestacion.getNombreServicio());
		fila.setDuracionMinutos(prestacion.getDuracionMinutos());
		fila.setNombreCategoria(prestacion.getNombreCategoria());
		fila.setProfesionalId(prestacion.getProfesionalId());
		fila.setNombreProfesional(prestacion.getNombreProfesional());
		fila.setFecha(visita.getFecha());
		fila.setHoraInicio(prestacion.getHoraInicio());
		fila.setHoraFin(prestacion.getHoraFin());
		fila.setEstado(visita.getEstado());
		fila.setMontoTotal(prestacion.getPrecio());
		fila.setMontoAbono(visita.getMontoAbono());
		fila.setMontoPagado(visita.getMontoPagado());
		fila.setNotas(visita.getNotas());
		fila.setSerieId(visita.getSerieId());
		fila.agregarPrestacion(copiar(prestacion));
		return fila;
	}

	private Prestacion copiar(Prestacion origen) {
		Prestacion copia = new Prestacion();
		copia.setOrden(origen.getOrden());
		copia.setServicioId(origen.getServicioId());
		copia.setNombreServicio(origen.getNombreServicio());
		copia.setNombreCategoria(origen.getNombreCategoria());
		copia.setDuracionMinutos(origen.getDuracionMinutos());
		copia.setPrecio(origen.getPrecio());
		copia.setProfesionalId(origen.getProfesionalId());
		copia.setNombreProfesional(origen.getNombreProfesional());
		copia.setHoraInicio(origen.getHoraInicio());
		copia.setHoraFin(origen.getHoraFin());
		return copia;
	}

	private String recortar(String texto, int maximo) {
		if (texto == null) {
			return "";
		}
		return texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}

	private boolean esFutura(Reserva reserva) {
		return LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio()).isAfter(LocalDateTime.now());
	}

	private String texto(Reserva reserva) {
		return reserva.getNombreServicio() + " el " + reserva.getFecha() + " a las " + reserva.getHoraInicio() + ".";
	}

}
