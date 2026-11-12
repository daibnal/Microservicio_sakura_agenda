package com.sakura.agenda.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelacionRequest {

	@NotBlank(message = "El motivo es obligatorio")
	@Size(max = 300, message = "El motivo no puede superar 300 caracteres")
	private String motivo;

}
