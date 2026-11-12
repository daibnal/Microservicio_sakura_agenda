package com.sakura.agenda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sakura.agenda.client.SesionDTO;
import com.sakura.agenda.client.SesionGuard;
import com.sakura.agenda.exception.AccesoDenegadoException;
import com.sakura.agenda.exception.NoAutenticadoException;
import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.EstadosReserva;
import com.sakura.agenda.model.Hueco;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.model.ResultadoReserva;
import com.sakura.agenda.service.DisponibilidadService;
import com.sakura.agenda.service.ReservaService;

@WebMvcTest(ReservaController.class)
@ActiveProfiles("test")
class ReservaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservaService reservaService;

	@MockitoBean
	private DisponibilidadService disponibilidadService;

	@MockitoBean
	private SesionGuard sesionGuard;

	private static final String AUTH = "Bearer token-cliente";

	private SesionDTO sesion;

	@BeforeEach
	void setUp() {
		sesion = new SesionDTO();
		sesion.setId(7L);
		sesion.setRol("CLIENTE");
		when(sesionGuard.exigirSesion(any())).thenReturn(sesion);
	}

	@Test
	void disponibilidad_Exito() throws Exception {
		when(disponibilidadService.listarHuecos(20L, 10L, LocalDate.of(2026, 5, 10)))
				.thenReturn(List.of(new Hueco(LocalTime.of(9, 0), LocalTime.of(10, 0))));

		mockMvc.perform(get("/api/v1/reservas/disponibilidad")
				.param("profesionalId", "20")
				.param("servicioId", "10")
				.param("fecha", "2026-05-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].horaInicio").value("09:00:00"));
	}

	@Test
	void mias_Exito() throws Exception {
		when(reservaService.mias(7L)).thenReturn(List.of(reserva(1L)));

		mockMvc.perform(get("/api/v1/reservas/mias").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombreCliente").value("Juan Perez"));
	}

	@Test
	void mias_SinSesion_Responde401() throws Exception {
		when(sesionGuard.exigirSesion(any())).thenThrow(new NoAutenticadoException("Debe iniciar sesión"));

		mockMvc.perform(get("/api/v1/reservas/mias"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión"));
	}

	@Test
	void agenda_Exito() throws Exception {
		when(reservaService.agenda("dia", LocalDate.of(2026, 5, 10), 20L, "CONFIRMADA")).thenReturn(List.of(reserva(1L)));

		mockMvc.perform(get("/api/v1/reservas/agenda")
				.header("Authorization", AUTH)
				.param("vista", "dia")
				.param("fecha", "2026-05-10")
				.param("profesionalId", "20")
				.param("estado", "CONFIRMADA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void agenda_SinPermiso_Responde403() throws Exception {
		doThrow(new AccesoDenegadoException("No tiene permiso")).when(sesionGuard).exigirAdmin(any());

		mockMvc.perform(get("/api/v1/reservas/agenda").header("Authorization", AUTH)
				.param("vista", "dia").param("fecha", "2026-05-10"))
				.andExpect(status().isForbidden());
	}

	@Test
	void rango_Exito() throws Exception {
		when(reservaService.rango(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))).thenReturn(List.of(reserva(1L)));

		mockMvc.perform(get("/api/v1/reservas/rango").header("Authorization", AUTH)
				.param("desde", "2026-05-01").param("hasta", "2026-05-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void historial_Exito() throws Exception {
		when(reservaService.historial(20L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
				.thenReturn(List.of(reserva(1L)));

		mockMvc.perform(get("/api/v1/reservas/historial").header("Authorization", AUTH)
				.param("profesionalId", "20").param("desde", "2026-05-01").param("hasta", "2026-05-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombreServicio").value("Corte"));
	}

	@Test
	void existe_Exito() throws Exception {
		when(reservaService.tieneReservas(20L)).thenReturn(true);

		mockMvc.perform(get("/api/v1/reservas/profesional/20/existe").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.existe").value(true));
	}

	@Test
	void crear_Exito() throws Exception {
		when(reservaService.crear(eq(sesion), any())).thenReturn(new ResultadoReserva(reserva(8L), List.of(reserva(8L)), List.of()));

		mockMvc.perform(post("/api/v1/reservas")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"servicioId\":10,\"profesionalId\":20,\"fecha\":\"2026-05-10\",\"horaInicio\":\"10:00:00\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reserva.id").value(8));
	}

	@Test
	void crear_ConVariasPrestaciones_Exito() throws Exception {
		Reserva reserva = reserva(9L);
		reserva.setDuracionMinutos(105);
		reserva.setNombreServicio("Lifting, Uñas");
		when(reservaService.crear(eq(sesion), any())).thenReturn(new ResultadoReserva(reserva, List.of(reserva), List.of()));

		mockMvc.perform(post("/api/v1/reservas")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fecha\":\"2026-05-10\",\"horaInicio\":\"10:00:00\",\"prestaciones\":[{\"servicioId\":10,\"profesionalId\":20},{\"servicioId\":11,\"profesionalId\":30}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reserva.nombreServicio").value("Lifting, Uñas"))
				.andExpect(jsonPath("$.reserva.duracionMinutos").value(105));
	}

	@Test
	void crear_CuerpoVacio_Responde400() throws Exception {
		mockMvc.perform(post("/api/v1/reservas")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void obtener_Exito() throws Exception {
		when(reservaService.buscar(1L)).thenReturn(reserva(1L));

		mockMvc.perform(get("/api/v1/reservas/1").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombreCliente").value("Juan Perez"));
	}

	@Test
	void obtener_NoExiste_Responde404() throws Exception {
		when(reservaService.buscar(99L)).thenThrow(new RecursoNoEncontradoException("No existe la reserva"));

		mockMvc.perform(get("/api/v1/reservas/99").header("Authorization", AUTH))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("No existe la reserva"));
	}

	@Test
	void mover_Exito() throws Exception {
		when(reservaService.mover(eq(1L), any())).thenReturn(reserva(1L));

		mockMvc.perform(put("/api/v1/reservas/1")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fecha\":\"2026-05-12\",\"horaInicio\":\"11:00:00\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void cancelar_Conflicto_Responde409() throws Exception {
		when(reservaService.cancelar(1L)).thenThrow(new ReglaNegocioException("La reserva ya no se puede cancelar"));

		mockMvc.perform(post("/api/v1/reservas/1/cancelar").header("Authorization", AUTH))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("La reserva ya no se puede cancelar"));
	}

	@Test
	void cancelar_Exito() throws Exception {
		Reserva cancelada = reserva(1L);
		cancelada.setEstado(EstadosReserva.CANCELADA);
		when(reservaService.cancelar(1L)).thenReturn(cancelada);

		mockMvc.perform(post("/api/v1/reservas/1/cancelar").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"));
	}

	@Test
	void cancelarCliente_Exito() throws Exception {
		when(reservaService.cancelarCliente(eq(1L), eq(sesion), eq("Cambio de planes"))).thenReturn(reserva(1L));

		mockMvc.perform(post("/api/v1/reservas/1/cancelar-cliente")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"Cambio de planes\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void moverCliente_Exito() throws Exception {
		when(reservaService.moverCliente(eq(1L), eq(sesion), any())).thenReturn(reserva(1L));

		mockMvc.perform(put("/api/v1/reservas/1/cliente")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fecha\":\"2026-05-12\",\"horaInicio\":\"15:00:00\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void asistencia_Exito() throws Exception {
		Reserva enProceso = reserva(1L);
		enProceso.setEstado(EstadosReserva.EN_PROCESO);
		when(reservaService.marcarAsistencia(1L)).thenReturn(enProceso);

		mockMvc.perform(post("/api/v1/reservas/1/asistencia").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_PROCESO"));
	}

	@Test
	void inasistencia_ConMotivo_Exito() throws Exception {
		when(reservaService.marcarInasistencia(1L, "No llegó")).thenReturn(reserva(1L));

		mockMvc.perform(post("/api/v1/reservas/1/inasistencia")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"No llegó\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void inasistencia_SinCuerpo_Exito() throws Exception {
		when(reservaService.marcarInasistencia(1L, null)).thenReturn(reserva(1L));

		mockMvc.perform(post("/api/v1/reservas/1/inasistencia").header("Authorization", AUTH))
				.andExpect(status().isOk());
	}

	@Test
	void pagarAbono_Exito() throws Exception {
		when(reservaService.buscar(1L)).thenReturn(reserva(1L));
		when(reservaService.pagarAbono(1L, true, AUTH)).thenReturn(reserva(1L));

		mockMvc.perform(post("/api/v1/reservas/1/pagos")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aprobado\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void saldo_Exito() throws Exception {
		when(reservaService.registrarSaldo(1L, AUTH)).thenReturn(reserva(1L));

		mockMvc.perform(post("/api/v1/reservas/1/saldo").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void pagoManual_Exito() throws Exception {
		when(reservaService.registrarManual(1L, AUTH)).thenReturn(reserva(1L));

		mockMvc.perform(post("/api/v1/reservas/1/pagos/manual").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	private Reserva reserva(Long id) {
		Reserva reserva = new Reserva();
		reserva.setId(id);
		reserva.setIdCliente(7L);
		reserva.setNombreCliente("Juan Perez");
		reserva.setNombreServicio("Corte");
		reserva.setEstado(EstadosReserva.CONFIRMADA);
		return reserva;
	}
}
