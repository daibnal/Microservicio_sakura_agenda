package com.sakura.agenda.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfiguracionDTO {

	private int porcentajeAbono;

	private int diasAnticipacionMaxima;

	private int horasAnticipacionMinima;

}
