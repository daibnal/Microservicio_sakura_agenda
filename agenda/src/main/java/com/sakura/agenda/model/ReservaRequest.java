package com.sakura.agenda.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReservaRequest {

	private Long servicioId;

	private Long profesionalId;

	@Valid
	private List<PrestacionPedido> prestaciones;

	@AssertTrue(message = "Debe indicar al menos un servicio")
	@JsonIgnore
	public boolean isTieneServicio() {
		boolean lista = prestaciones != null && !prestaciones.isEmpty();
		boolean unico = servicioId != null && profesionalId != null;
		return lista || unico;
	}

	@NotNull(message = "La fecha es obligatoria")
	private LocalDate fecha;

	@NotNull(message = "La hora es obligatoria")
	private LocalTime horaInicio;

	@Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
	private String notas;

	private String recurrencia;

	private Integer repeticiones;

}
