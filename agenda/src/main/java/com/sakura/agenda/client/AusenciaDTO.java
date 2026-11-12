package com.sakura.agenda.client;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AusenciaDTO {

	private LocalDate fechaInicio;

	private LocalDate fechaFin;

}
