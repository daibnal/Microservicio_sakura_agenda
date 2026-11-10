package com.sakura.agenda.client;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurnoDTO {

	private Integer diaSemana;

	private LocalTime horaInicio;

	private LocalTime horaFin;

	private LocalTime horaAlmuerzoInicio;

	private LocalTime horaAlmuerzoFin;

}
