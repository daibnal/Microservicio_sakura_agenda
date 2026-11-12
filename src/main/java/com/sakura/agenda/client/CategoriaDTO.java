package com.sakura.agenda.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoriaDTO {

	private Long id;

	private String nombre;

}
