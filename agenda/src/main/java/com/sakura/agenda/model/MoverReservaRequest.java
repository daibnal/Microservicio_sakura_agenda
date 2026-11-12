package com.sakura.agenda.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoverReservaRequest {

	@NotNull(message = "La fecha es obligatoria")
	private LocalDate fecha;

	@NotNull(message = "La hora es obligatoria")
	private LocalTime horaInicio;

}
