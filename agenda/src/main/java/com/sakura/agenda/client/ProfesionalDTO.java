package com.sakura.agenda.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfesionalDTO {

	private Long id;

	private String nombre;

	private boolean activo;

}
