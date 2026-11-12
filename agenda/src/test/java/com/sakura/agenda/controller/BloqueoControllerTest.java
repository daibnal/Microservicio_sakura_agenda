package com.sakura.agenda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sakura.agenda.client.SesionGuard;
import com.sakura.agenda.exception.AccesoDenegadoException;
import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.model.Bloqueo;
import com.sakura.agenda.service.BloqueoService;

@WebMvcTest(BloqueoController.class)
@ActiveProfiles("test")
class BloqueoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BloqueoService bloqueoService;

	@MockitoBean
	private SesionGuard sesionGuard;

	private static final String AUTH = "Bearer token-admin";

	@Test
	void listar_Exito() throws Exception {
		Bloqueo bloqueo = bloqueo(1L);
		when(bloqueoService.listar(LocalDate.of(2026, 5, 10))).thenReturn(List.of(bloqueo));

		mockMvc.perform(get("/api/v1/reservas/bloqueos").header("Authorization", AUTH).param("fecha", "2026-05-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].motivo").value("Mantención"));
	}

	@Test
	void listar_SinPermiso_Responde403() throws Exception {
		doThrow(new AccesoDenegadoException("No tiene permiso")).when(sesionGuard).exigirAdmin(any());

		mockMvc.perform(get("/api/v1/reservas/bloqueos").header("Authorization", AUTH))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No tiene permiso"));
	}

	@Test
	void crear_Exito() throws Exception {
		when(bloqueoService.crear(any(Bloqueo.class))).thenReturn(bloqueo(4L));

		mockMvc.perform(post("/api/v1/reservas/bloqueos")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fecha\":\"2026-05-10\",\"horaInicio\":\"09:00:00\",\"horaFin\":\"10:00:00\",\"motivo\":\"Mantención\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(4));
	}

	@Test
	void crear_CuerpoVacio_Responde400() throws Exception {
		mockMvc.perform(post("/api/v1/reservas/bloqueos")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void eliminar_Exito() throws Exception {
		doNothing().when(bloqueoService).eliminar(4L);

		mockMvc.perform(delete("/api/v1/reservas/bloqueos/4").header("Authorization", AUTH))
				.andExpect(status().isNoContent());
	}

	@Test
	void eliminar_NoExiste_Responde404() throws Exception {
		doThrow(new RecursoNoEncontradoException("No existe el bloqueo")).when(bloqueoService).eliminar(99L);

		mockMvc.perform(delete("/api/v1/reservas/bloqueos/99").header("Authorization", AUTH))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("No existe el bloqueo"));
	}

	private Bloqueo bloqueo(Long id) {
		Bloqueo bloqueo = new Bloqueo();
		bloqueo.setId(id);
		bloqueo.setFecha(LocalDate.of(2026, 5, 10));
		bloqueo.setHoraInicio(LocalTime.of(9, 0));
		bloqueo.setHoraFin(LocalTime.of(10, 0));
		bloqueo.setMotivo("Mantención");
		return bloqueo;
	}
}
