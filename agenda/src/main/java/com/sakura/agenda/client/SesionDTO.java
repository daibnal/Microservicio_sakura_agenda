package com.sakura.agenda.client;

import lombok.Data;

@Data
public class SesionDTO {

	private Long id;

	private String nombre;

	private String apellido;

	private String correo;

	private String rol;

	private String token;

}
