package com.sakura.agenda.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicioDTO {

	private Long id;

	private String nombre;

	private Integer precio;

	private Integer duracionMinutos;

	private boolean activo;

	private CategoriaDTO categoria;

}
